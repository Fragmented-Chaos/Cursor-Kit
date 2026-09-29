package com.fragmentedchaos.cursorkit.cursor.load;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Installs what the player drags onto the picker window.
 * <p>
 * Everything lands in the same two places the mod already reads: loose cursor files (a PNG, a JSON,
 * a {@code .cur}) go into {@code config/cursorkit/}, packs (a {@code .zip}, a folder holding
 * {@code assets/}, or any folder of sets) go into {@code config/cursorkit/packs/}. Nothing is ever
 * overwritten - a name that is already taken gets a {@code -2}, {@code -3}, ... suffix - because the
 * alternative is silently destroying a cursor set the player installed earlier.
 * <p>
 * Deliberately Minecraft-free, so the rules are unit testable.
 */
public final class CursorPackInstaller {

    /** File extensions that are a cursor image or its description. */
    private static final List<String> LOOSE_SUFFIXES = List.of(".png", ".json", ".cur", ".ani");

    private CursorPackInstaller() {
        throw new UnsupportedOperationException("CursorPackInstaller cannot be instantiated");
    }

    /**
     * Copies one dropped item into the mod's own directories.
     *
     * @param source          the file or folder that was dropped
     * @param configDirectory {@code config/cursorkit}, created when missing
     * @return what was installed, for the log and the status line; empty when nothing was
     * @throws IOException when copying fails - the caller reports it instead of losing the drop
     */
    public static String install(Path source, Path configDirectory) throws IOException {
        if (source == null || configDirectory == null || !Files.exists(source)) {
            return "";
        }
        Files.createDirectories(configDirectory);
        Path packs = CursorPackLoader.packsDirectory(configDirectory);

        if (Files.isDirectory(source)) {
            if (Files.isDirectory(source.resolve("assets"))) {
                Path target = unique(packs.resolve(source.getFileName().toString()));
                copyTree(source, target);
                return "pack " + target.getFileName();
            }
            // A plain folder of cursor sets: its files are loose sets, its subfolders are packs.
            int files = 0;
            int folders = 0;
            try (Stream<Path> children = Files.list(source)) {
                for (Path child : children.sorted().toList()) {
                    if (Files.isDirectory(child)) {
                        Path target = unique(packs.resolve(child.getFileName().toString()));
                        copyTree(child, target);
                        folders++;
                    } else if (isLoose(child)) {
                        Files.copy(child, unique(configDirectory.resolve(child.getFileName().toString())),
                                StandardCopyOption.COPY_ATTRIBUTES);
                        files++;
                    }
                }
            }
            return describe(files, folders);
        }

        if (isZip(source)) {
            Path target = unique(packs.resolve(source.getFileName().toString()));
            Files.createDirectories(packs);
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            return "pack " + target.getFileName();
        }
        if (isLoose(source)) {
            Path target = unique(configDirectory.resolve(source.getFileName().toString()));
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            return "set " + target.getFileName();
        }
        return "";
    }

    /** @return true when the drop is something this mod knows what to do with */
    public static boolean isSupported(Path source) {
        return source != null && Files.exists(source)
                && (Files.isDirectory(source) || isZip(source) || isLoose(source));
    }

    private static boolean isZip(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private static boolean isLoose(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return LOOSE_SUFFIXES.stream().anyMatch(name::endsWith);
    }

    private static String describe(int files, int folders) {
        List<String> parts = new ArrayList<>();
        if (files > 0) {
            parts.add(files + (files == 1 ? " set" : " sets"));
        }
        if (folders > 0) {
            parts.add(folders + (folders == 1 ? " pack" : " packs"));
        }
        return String.join(", ", parts);
    }

    /** @return {@code wanted}, or {@code wanted} with a {@code -2}, {@code -3}, ... before its suffix */
    private static Path unique(Path wanted) {
        if (!Files.exists(wanted)) {
            return wanted;
        }
        String name = wanted.getFileName().toString();
        String base = name;
        String suffix = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            suffix = name.substring(dot);
        }
        Path parent = wanted.getParent();
        for (int index = 2; index < 1000; index++) {
            Path candidate = parent.resolve(base + "-" + index + suffix);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        return parent.resolve(base + "-" + System.nanoTime() + suffix);
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.sorted().toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    try (InputStream in = Files.newInputStream(path)) {
                        Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
}
