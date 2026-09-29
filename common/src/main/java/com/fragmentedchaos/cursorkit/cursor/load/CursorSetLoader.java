package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads cursor sets from the client's resource packs and from {@code config/cursorkit/}.
 * <p>
 * Resource packs contribute {@code assets/<namespace>/cursor/<name>.json}; the config directory
 * contributes {@code <name>.json} (full sets) and {@code <name>.png} (a single default image, used
 * unless a JSON of the same name exists).
 */
public final class CursorSetLoader {

    /** Directory inside a namespace that holds cursor sets: {@code assets/<ns>/cursor/}. */
    public static final String RESOURCE_DIRECTORY = "cursor";

    private static final String JSON_SUFFIX = ".json";
    private static final String PNG_SUFFIX = ".png";

    private CursorSetLoader() {
        throw new UnsupportedOperationException("CursorSetLoader cannot be instantiated");
    }

    /**
     * @return every available cursor set, already merged by name (see {@link CursorSetRegistry})
     */
    public static List<CursorSet> loadAll(ResourceManager resourceManager, Path configDirectory) {
        List<CursorSet> found = new ArrayList<>();
        found.addAll(loadFromResourcePacks(resourceManager));
        found.addAll(CursorPackLoader.loadAll(configDirectory));
        found.addAll(loadFromConfigDirectory(configDirectory));
        return CursorSetRegistry.merge(found);
    }

    /** Reads every {@code assets/<namespace>/cursor/<name>.json}. */
    public static List<CursorSet> loadFromResourcePacks(ResourceManager resourceManager) {
        List<CursorSet> result = new ArrayList<>();
        Map<Identifier, Resource> resources = resourceManager.listResources(
                RESOURCE_DIRECTORY, id -> id.getPath().endsWith(JSON_SUFFIX));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier id = entry.getKey();
            Resource resource = entry.getValue();
            CursorSetOrigin origin = CursorSetOrigin.RESOURCE_PACK;
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                result.add(CursorSetParser.parse(stripSuffix(fileName(id.getPath()), JSON_SUFFIX),
                        id.getNamespace(), origin, resource.sourcePackId(), root));
            } catch (Exception e) {
                Constants.LOG.warn("Skipping cursor set '{}' from {}: {}",
                        id, resource.sourcePackId(), e.toString());
            }
        }
        return result;
    }

    /** Reads {@code config/cursorkit/*.json} and {@code config/cursorkit/*.png}. */
    public static List<CursorSet> loadFromConfigDirectory(Path configDirectory) {
        List<CursorSet> result = new ArrayList<>();
        if (configDirectory == null || !Files.isDirectory(configDirectory)) {
            return result;
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(configDirectory)) {
            files = stream.sorted().toList();
        } catch (IOException e) {
            Constants.LOG.warn("Could not list {}: {}", configDirectory, e.toString());
            return result;
        }

        for (Path file : files) {
            String name = file.getFileName().toString();
            try {
                if (name.endsWith(JSON_SUFFIX)) {
                    String setName = stripSuffix(name, JSON_SUFFIX);
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        result.add(CursorSetParser.parse(setName, Constants.MOD_ID,
                                CursorSetOrigin.CONFIG, file.getFileName().toString(), root));
                    }
                } else if (name.endsWith(PNG_SUFFIX) && !hasJsonSibling(files, name)) {
                    String setName = stripSuffix(name, PNG_SUFFIX);
                    result.add(new CursorSet(setName, setName, Constants.MOD_ID,
                            CursorSetOrigin.CONFIG, file.getFileName().toString(), 1,
                            Map.of(CursorState.DEFAULT, new CursorImage(name, 0, 0, 1, 100))));
                }
            } catch (Exception e) {
                Constants.LOG.warn("Skipping cursor set '{}' from {}: {}", name, file, e.toString());
            }
        }
        return result;
    }

    private static boolean hasJsonSibling(List<Path> files, String pngName) {
        String jsonName = stripSuffix(pngName, PNG_SUFFIX) + JSON_SUFFIX;
        return files.stream().anyMatch(p -> p.getFileName().toString().equals(jsonName));
    }

    /**
     * Fingerprint of everything inside {@code config/cursorkit/}: names, sizes and modification
     * times, including cursor packs. Comparing two fingerprints is how the mod notices that a user
     * dropped a new cursor set in while the game is running, without parsing the files again on
     * every check.
     *
     * @param configDirectory the {@code config/cursorkit} directory, may be {@code null}
     * @return the fingerprint, or {@code 0} when the directory does not exist or cannot be read
     */
    public static long signature(@Nullable Path configDirectory) {
        if (configDirectory == null || !Files.isDirectory(configDirectory)) {
            return 0L;
        }
        long hash = 1_125_899_906_842_597L;
        try (Stream<Path> stream = Files.walk(configDirectory)) {
            for (Path path : stream.sorted().toList()) {
                hash = hash * 31 + configDirectory.relativize(path).toString().hashCode();
                boolean regular = Files.isRegularFile(path);
                hash = hash * 31 + (regular ? Files.size(path) : -1L);
                hash = hash * 31 + (regular ? Files.getLastModifiedTime(path).toMillis() : -1L);
            }
        } catch (IOException e) {
            Constants.LOG.debug("Could not fingerprint {}: {}", configDirectory, e.toString());
            return 0L;
        }
        return hash;
    }

    private static String fileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
