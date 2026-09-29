package com.fragmentedchaos.cursorkit.cursor;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The fingerprint is what makes live reloading work: {@code CursorWatcher} only re-reads the config
 * directory when it moves.
 */
class CursorSetLoaderSignatureTest {

    @Test
    void signatureIsStableWhileNothingChanges(@TempDir Path configDirectory) throws IOException {
        Files.writeString(configDirectory.resolve("one.json"), "{}");

        assertEquals(CursorSetLoader.signature(configDirectory),
                CursorSetLoader.signature(configDirectory));
    }

    @Test
    void signatureMovesWhenAFileAppears(@TempDir Path configDirectory) throws IOException {
        long before = CursorSetLoader.signature(configDirectory);

        Files.writeString(configDirectory.resolve("one.json"), "{}");

        assertNotEquals(before, CursorSetLoader.signature(configDirectory));
    }

    @Test
    void signatureMovesWhenAFileChanges(@TempDir Path configDirectory) throws IOException {
        Path file = configDirectory.resolve("one.json");
        Files.writeString(file, "{}");
        long before = CursorSetLoader.signature(configDirectory);

        Files.writeString(file, "{ \"name\": \"changed\" }");
        Files.setLastModifiedTime(file, FileTime.fromMillis(
                Files.getLastModifiedTime(file).toMillis() + 5_000L));

        assertNotEquals(before, CursorSetLoader.signature(configDirectory));
    }

    @Test
    void signatureCoversCursorPacks(@TempDir Path configDirectory) throws IOException {
        long before = CursorSetLoader.signature(configDirectory);

        Path archive = configDirectory.resolve("packs/mine.zip");
        Files.createDirectories(archive.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("assets/testpack/cursor/one.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertNotEquals(before, CursorSetLoader.signature(configDirectory));
    }

    @Test
    void missingDirectoryHasAnEmptySignature(@TempDir Path configDirectory) {
        assertEquals(0L, CursorSetLoader.signature(null));
        assertEquals(0L, CursorSetLoader.signature(configDirectory.resolve("nope")));
    }
}
