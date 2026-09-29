package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.load.CursorPackInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Installing what the player drags onto the picker: packs go to {@code packs/}, loose cursor files
 * next to the configuration, and nothing that is already there gets overwritten.
 */
class CursorPackInstallerTest {

    @Test
    void aDroppedZipBecomesAPack(@TempDir Path root) throws IOException {
        Path config = root.resolve("cursorkit");
        Path zip = root.resolve("My Pack.zip");
        writeZip(zip);

        String installed = CursorPackInstaller.install(zip, config);

        assertEquals("pack My Pack.zip", installed);
        assertTrue(Files.isRegularFile(config.resolve("packs/My Pack.zip")));
    }

    @Test
    void aFolderWithAssetsBecomesAPack(@TempDir Path root) throws IOException {
        Path config = root.resolve("cursorkit");
        Path folder = Files.createDirectories(root.resolve("Cool Pack/assets/mine/cursor"));
        Files.writeString(folder.resolve("hero.json"), "{}", StandardCharsets.UTF_8);

        assertEquals("pack Cool Pack", CursorPackInstaller.install(root.resolve("Cool Pack"), config));
        assertTrue(Files.isRegularFile(config.resolve("packs/Cool Pack/assets/mine/cursor/hero.json")));
    }

    @Test
    void aFolderOfLooseFilesLandsInTheConfigDirectory(@TempDir Path root) throws IOException {
        Path config = root.resolve("cursorkit");
        Path folder = Files.createDirectories(root.resolve("my cursors"));
        Files.write(folder.resolve("arrow.png"), new byte[] {1});
        Files.writeString(folder.resolve("arrow.json"), "{}", StandardCharsets.UTF_8);
        Files.write(folder.resolve("notes.txt"), new byte[] {2});
        Files.createDirectories(folder.resolve("nested/assets/mine/cursor"));

        assertEquals("2 sets, 1 pack", CursorPackInstaller.install(folder, config));

        assertTrue(Files.isRegularFile(config.resolve("arrow.png")));
        assertTrue(Files.isRegularFile(config.resolve("arrow.json")));
        assertFalse(Files.exists(config.resolve("notes.txt")), "only cursor files are taken");
        assertTrue(Files.isDirectory(config.resolve("packs/nested/assets/mine/cursor")));
    }

    @Test
    void aSingleImageBecomesASet(@TempDir Path root) throws IOException {
        Path config = root.resolve("cursorkit");
        Path png = root.resolve("pointer.cur");
        Files.write(png, new byte[] {3});

        assertEquals("set pointer.cur", CursorPackInstaller.install(png, config));
        assertTrue(Files.isRegularFile(config.resolve("pointer.cur")));
    }

    @Test
    void anExistingNameIsKeptAndTheNewOneIsSuffixed(@TempDir Path root) throws IOException {
        Path config = root.resolve("cursorkit");
        Path zip = root.resolve("pack.zip");
        writeZip(zip);

        CursorPackInstaller.install(zip, config);
        String second = CursorPackInstaller.install(zip, config);

        assertEquals("pack pack-2.zip", second);
        assertTrue(Files.isRegularFile(config.resolve("packs/pack.zip")));
        assertTrue(Files.isRegularFile(config.resolve("packs/pack-2.zip")));
    }

    @Test
    void unrelatedDropsAreIgnored(@TempDir Path root) throws IOException {
        Path config = root.resolve("cursorkit");
        Path text = root.resolve("readme.txt");
        Files.writeString(text, "hello", StandardCharsets.UTF_8);

        assertEquals("", CursorPackInstaller.install(text, config));
        assertFalse(CursorPackInstaller.isSupported(text));
        assertFalse(CursorPackInstaller.isSupported(root.resolve("does-not-exist")));
    }

    private static void writeZip(Path zip) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("assets/mine/cursor/hero.json"));
            out.write("{}".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    /** Keeps the read helper honest; also proves the copied pack is a readable zip. */
    @SuppressWarnings("unused")
    private static byte[] read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return in.readAllBytes();
        }
    }
}
