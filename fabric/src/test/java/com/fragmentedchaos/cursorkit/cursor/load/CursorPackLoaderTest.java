package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.load.CursorPackLoader;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorPackLoaderTest {

    private static final String SET_JSON = """
            { "name": "From Pack",
              "states": { "default": { "texture": "arrow.png" } } }
            """;

    @Test
    void readsAFolderPack(@TempDir Path configDirectory) throws IOException {
        write(configDirectory.resolve("packs/folderpack/assets/testpack/cursor/one.json"), SET_JSON);

        List<CursorSet> sets = CursorPackLoader.loadAll(configDirectory);

        assertEquals(1, sets.size());
        CursorSet set = sets.get(0);
        assertEquals("one", set.id());
        assertEquals("From Pack", set.name());
        assertEquals("testpack", set.namespace());
        assertEquals(CursorSetOrigin.CONFIG_PACK, set.origin());
        assertEquals("packs/folderpack", set.source());
    }

    @Test
    void readsAnArchivedPack(@TempDir Path configDirectory) throws IOException {
        Path archive = configDirectory.resolve("packs/zippack.zip");
        Files.createDirectories(archive.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            put(zip, "assets/testpack/cursor/one.json", SET_JSON);
            put(zip, "assets/testpack/textures/cursor/arrow.png", "not really a png");
            put(zip, "pack.mcmeta", "{ \"pack\": { \"description\": \"test\" } }");
        }

        List<CursorSet> sets = CursorPackLoader.loadAll(configDirectory);

        assertEquals(1, sets.size());
        CursorSet set = sets.get(0);
        assertEquals("one", set.id());
        assertEquals("testpack", set.namespace());
        assertEquals(CursorSetOrigin.CONFIG_PACK, set.origin());
        assertEquals("packs/zippack.zip", set.source());
    }

    @Test
    void folderAndArchiveProvideTheSameSets(@TempDir Path configDirectory) throws IOException {
        write(configDirectory.resolve("packs/mine/assets/testpack/cursor/one.json"), SET_JSON);
        Path archive = configDirectory.resolve("packs/mine.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            put(zip, "assets/testpack/cursor/one.json", SET_JSON);
        }

        List<CursorSet> sets = CursorPackLoader.loadAll(configDirectory);

        // Same id from two packs: the registry keeps one of them, both were readable.
        assertEquals(2, sets.size());
        assertEquals(2, sets.stream().filter(set -> set.id().equals("one")).count());
    }

    @Test
    void skipsEverythingThatIsNotASet(@TempDir Path configDirectory) throws IOException {
        write(configDirectory.resolve("packs/mine/pack.mcmeta"), "{ \"pack\": {} }");
        write(configDirectory.resolve("packs/mine/assets/testpack/textures/cursor/arrow.json"),
                SET_JSON);
        write(configDirectory.resolve("packs/mine/assets/Bad Namespace/cursor/one.json"), SET_JSON);
        write(configDirectory.resolve("packs/mine/readme.txt"), "hello");

        assertTrue(CursorPackLoader.loadAll(configDirectory).isEmpty());
    }

    @Test
    void oneBrokenSetDoesNotHideTheRest(@TempDir Path configDirectory) throws IOException {
        write(configDirectory.resolve("packs/mine/assets/testpack/cursor/broken.json"), "{ not json");
        write(configDirectory.resolve("packs/mine/assets/testpack/cursor/good.json"), SET_JSON);

        List<CursorSet> sets = CursorPackLoader.loadAll(configDirectory);

        assertEquals(1, sets.size());
        assertEquals("good", sets.get(0).id());
    }

    @Test
    void aBrokenArchiveIsSkipped(@TempDir Path configDirectory) throws IOException {
        write(configDirectory.resolve("packs/broken.zip"), "this is not a zip");

        assertTrue(CursorPackLoader.loadAll(configDirectory).isEmpty());
    }

    @Test
    void missingDirectoryIsNotAnError(@TempDir Path configDirectory) {
        assertTrue(CursorPackLoader.loadAll(configDirectory).isEmpty());
        assertTrue(CursorPackLoader.loadAll(null).isEmpty());
    }

    @Test
    void entryPathsAreParsed() {
        CursorPackLoader.PackEntry entry = CursorPackLoader.parseEntry("assets/mypack/cursor/one.json");
        assertNotNull(entry);
        assertEquals("mypack", entry.namespace());
        assertEquals("one", entry.id());

        // Sub directories are allowed, the file name is the id.
        assertEquals("two", CursorPackLoader.parseEntry("assets/mypack/cursor/sub/two.json").id());

        assertNull(CursorPackLoader.parseEntry("assets/mypack/textures/cursor/arrow.json"));
        assertNull(CursorPackLoader.parseEntry("assets/cursorkit/cursor/one.txt"));
        assertNull(CursorPackLoader.parseEntry("assets//cursor/one.json"));
        assertNull(CursorPackLoader.parseEntry("assets/mypack/cursor/.json"));
        assertNull(CursorPackLoader.parseEntry("cursor/one.json"));
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Test
    void aZipThatStillHasItsFolderAroundItWorks(@TempDir Path root) throws IOException {
        // What you get from "zip -r pack.zip pack": one directory on top of everything.
        Path zip = root.resolve("wrapped.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("My Pack/assets/mine/cursor/hero.json"));
            out.write("""
                    { "states": { "default": { "texture": "arrow.png" } } }
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("My Pack/assets/mine/textures/cursor/arrow.png"));
            out.write(new byte[] {1});
            out.closeEntry();
        }
        Path packs = Files.createDirectories(root.resolve("cursorkit/packs"));
        Files.copy(zip, packs.resolve("wrapped.zip"));

        List<CursorSet> sets = CursorPackLoader.loadAll(root.resolve("cursorkit"));

        assertEquals(1, sets.size(), "the set inside the wrapper folder is still found");
        assertEquals("hero", sets.get(0).id());
    }

    @Test
    void anUnwrappedPackIsNotRewritten(@TempDir Path root) throws IOException {
        Path zip = root.resolve("plain.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("assets/mine/cursor/hero.json"));
            out.write("""
                    { "states": { "default": { "texture": "arrow.png" } } }
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("assets/mine/textures/cursor/arrow.png"));
            out.write(new byte[] {1});
            out.closeEntry();
        }
        Path packs = Files.createDirectories(root.resolve("cursorkit/packs"));
        Files.copy(zip, packs.resolve("plain.zip"));

        List<CursorSet> sets = CursorPackLoader.loadAll(root.resolve("cursorkit"));

        assertEquals(1, sets.size());
        assertEquals("hero", sets.get(0).id());
    }
}
