package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads cursor packs from {@code config/cursorkit/packs/}.
 * <p>
 * A cursor pack is either a folder or a {@code .zip} file and uses exactly the resource pack layout
 * this mod looks for anywhere else:
 *
 * <pre>
 * assets/&lt;namespace&gt;/cursor/&lt;id&gt;.json
 * assets/&lt;namespace&gt;/textures/cursor/&lt;image&gt;.png
 * </pre>
 * <p>
 * That way one pack can be dropped into {@code config/cursorkit/packs/} (always active, no resource
 * pack screen involved) or into {@code resourcepacks/} without changing a single file. A
 * {@code pack.mcmeta} is only needed for the resource pack route and is ignored here.
 * <p>
 * Deliberately Minecraft-free, like the rest of this package, so the scanning rules are unit
 * testable.
 */
public final class CursorPackLoader {

    /** Directory inside {@code config/cursorkit/} that holds cursor packs. */
    public static final String DIRECTORY = "packs";

    /** Suffix of a cursor pack that is stored as an archive. */
    public static final String ZIP_SUFFIX = ".zip";

    private static final String ASSETS_PREFIX = "assets/";
    private static final String CURSOR_SEGMENT = "/cursor/";
    private static final String JSON_SUFFIX = ".json";

    private CursorPackLoader() {
        throw new UnsupportedOperationException("CursorPackLoader cannot be instantiated");
    }

    /** @return {@code config/cursorkit/packs}, or {@code null} when there is no config directory */
    public static Path packsDirectory(Path configDirectory) {
        return configDirectory == null ? null : configDirectory.resolve(DIRECTORY);
    }

    /**
     * Reads every cursor pack below {@code config/cursorkit/packs/}. Broken packs and broken sets
     * are skipped with a log line, one bad file never hides the rest.
     *
     * @param configDirectory the {@code config/cursorkit} directory, may be {@code null}
     * @return every cursor set the packs contain, in no particular order
     */
    public static List<CursorSet> loadAll(Path configDirectory) {
        List<CursorSet> result = new ArrayList<>();
        Path root = packsDirectory(configDirectory);
        if (root == null || !Files.isDirectory(root)) {
            return result;
        }

        List<Path> packs;
        try (Stream<Path> stream = Files.list(root)) {
            packs = stream.sorted().toList();
        } catch (IOException e) {
            Constants.LOG.warn("Could not list {}: {}", root, e.toString());
            return result;
        }

        for (Path pack : packs) {
            String name = pack.getFileName().toString();
            // Source is relative to the config directory so the details panel stays readable and
            // the asset lookup can find the pack again.
            String source = DIRECTORY + "/" + name;
            List<CursorSet> loaded;
            if (Files.isDirectory(pack)) {
                loaded = loadFromDirectory(pack, source);
            } else if (name.endsWith(ZIP_SUFFIX)) {
                loaded = loadFromArchive(pack, source);
            } else {
                continue;
            }
            if (!loaded.isEmpty()) {
                Constants.LOG.info("Cursor pack {} provides {} cursor set(s)", source, loaded.size());
            }
            result.addAll(loaded);
        }
        return result;
    }

    private static List<CursorSet> loadFromDirectory(Path pack, String source) {
        List<CursorSet> result = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(pack)) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            Constants.LOG.warn("Could not read cursor pack {}: {}", source, e.toString());
            return result;
        }

        for (Path file : files) {
            String path = pack.relativize(file).toString().replace('\\', '/');
            PackEntry entry = parseEntry(path);
            if (entry == null) {
                continue;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                add(result, entry, source, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (Exception e) {
                Constants.LOG.warn("Skipping cursor set '{}' from {}: {}", entry.id(), source,
                        e.toString());
            }
        }
        return result;
    }

    private static List<CursorSet> loadFromArchive(Path pack, String source) {
        List<CursorSet> result = new ArrayList<>();
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            List<? extends ZipEntry> entries = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .toList();
            // A zip built by zipping the pack's folder has one directory on top of everything;
            // players do that all the time, so look one level down before giving up.
            String wrapper = wrapperDirectory(entries.stream().map(ZipEntry::getName).toList());
            for (ZipEntry entry : entries) {
                PackEntry parsed = parseEntry(wrapper == null
                        ? entry.getName() : entry.getName().substring(wrapper.length()));
                if (parsed == null) {
                    continue;
                }
                try (InputStream stream = zip.getInputStream(entry);
                     Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    add(result, parsed, source, JsonParser.parseReader(reader).getAsJsonObject());
                } catch (Exception e) {
                    Constants.LOG.warn("Skipping cursor set '{}' from {}: {}", parsed.id(), source,
                            e.toString());
                }
            }
        } catch (IOException e) {
            Constants.LOG.warn("Skipping cursor pack {}: {}", source, e.toString());
        }
        return result;
    }

    private static void add(List<CursorSet> result, PackEntry entry, String source, JsonObject root)
            throws CursorSetFormatException {
        result.add(CursorSetParser.parse(entry.id(), entry.namespace(), CursorSetOrigin.CONFIG_PACK,
                source, root));
    }

    /**
     * Finds the single directory every path sits in, which is what a pack looks like when its folder
     * was zipped instead of its contents.
     *
     * @param paths every file path of the pack, relative to its root
     * @return that directory including its trailing slash, or {@code null} when there is not exactly
     *         one and it does not look like a wrapper
     */
    static String wrapperDirectory(List<String> paths) {
        if (paths.isEmpty()) {
            return null;
        }
        String candidate = null;
        for (String path : paths) {
            int slash = path.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            String first = path.substring(0, slash + 1);
            if (candidate == null) {
                candidate = first;
            } else if (!candidate.equals(first)) {
                return null;
            }
        }
        // Only a folder that does not already look like a pack root can be the wrapper.
        for (String path : paths) {
            String stripped = path.substring(candidate.length());
            if (parseEntry(stripped) != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * @return the namespace and id of {@code assets/<namespace>/cursor/<path>.json}, or {@code null}
     *         when the path is not a cursor set of a pack
     */
    static PackEntry parseEntry(String path) {
        if (!path.startsWith(ASSETS_PREFIX) || !path.endsWith(JSON_SUFFIX)) {
            return null;
        }
        int cursor = path.indexOf(CURSOR_SEGMENT, ASSETS_PREFIX.length());
        if (cursor < 0) {
            return null;
        }
        String namespace = path.substring(ASSETS_PREFIX.length(), cursor);
        if (!isValidNamespace(namespace)) {
            return null;
        }
        String rest = path.substring(cursor + CURSOR_SEGMENT.length());
        if (rest.isEmpty()) {
            return null;
        }
        String file = rest.substring(rest.lastIndexOf('/') + 1);
        String id = file.substring(0, file.length() - JSON_SUFFIX.length());
        return id.isEmpty() ? null : new PackEntry(namespace, id);
    }

    /** A namespace becomes an identifier, so it has to follow the identifier rules. */
    private static boolean isValidNamespace(String namespace) {
        if (namespace.isEmpty() || namespace.indexOf('/') >= 0) {
            return false;
        }
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Namespace and set id of one entry inside a cursor pack. */
    record PackEntry(String namespace, String id) {
    }
}
