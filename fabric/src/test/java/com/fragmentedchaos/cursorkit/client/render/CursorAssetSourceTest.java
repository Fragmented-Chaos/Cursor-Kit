package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.client.render.CursorAssetSource;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the texture side of cursor packs: a pack is read from a folder or from a {@code .zip}
 * without going through Minecraft's texture loading.
 */
class CursorAssetSourceTest {

    private static final String TEXTURE = "arrow.png";
    private static final byte[] PIXELS = "pretend png bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    void readsTexturesFromAFolderPack(@TempDir Path configDirectory) throws IOException {
        Path file = configDirectory.resolve("packs/mine/assets/testpack/textures/cursor/" + TEXTURE);
        Files.createDirectories(file.getParent());
        Files.write(file, PIXELS);

        assertArrayEquals(PIXELS, read(set("packs/mine"), configDirectory));
    }

    @Test
    void readsTexturesFromAnArchivedPack(@TempDir Path configDirectory) throws IOException {
        Path archive = configDirectory.resolve("packs/mine.zip");
        Files.createDirectories(archive.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("assets/testpack/textures/cursor/" + TEXTURE));
            zip.write(PIXELS);
            zip.closeEntry();
        }

        assertArrayEquals(PIXELS, read(set("packs/mine.zip"), configDirectory));
    }

    @Test
    void missingTexturesAreReportedAsNoTexture(@TempDir Path configDirectory) throws IOException {
        Path archive = configDirectory.resolve("packs/mine.zip");
        Files.createDirectories(archive.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("assets/testpack/cursor/one.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertNull(CursorAssetSource.forImage(set("packs/mine.zip"), image(), configDirectory));
        assertNull(CursorAssetSource.forImage(set("packs/missing.zip"), image(), configDirectory));
        assertNull(CursorAssetSource.forImage(set("packs/mine"), image(), configDirectory));
    }

    @Test
    void aBrokenArchiveIsReportedAsNoTexture(@TempDir Path configDirectory) throws IOException {
        Path archive = configDirectory.resolve("packs/broken.zip");
        Files.createDirectories(archive.getParent());
        Files.writeString(archive, "definitely not a zip");

        assertNull(CursorAssetSource.forImage(set("packs/broken.zip"), image(), configDirectory));
    }

    @Test
    void resourcePackSetsAreLeftToMinecraft(@TempDir Path configDirectory) {
        CursorSet set = new CursorSet("one", "One", "testpack", CursorSetOrigin.RESOURCE_PACK,
                "file/Test", 1, Map.of(CursorState.DEFAULT, image()));

        assertNull(CursorAssetSource.forImage(set, image(), configDirectory));
    }

    private static CursorSet set(String source) {
        return new CursorSet("one", "One", "testpack", CursorSetOrigin.CONFIG_PACK, source, 1,
                Map.of(CursorState.DEFAULT, image()));
    }

    private static CursorImage image() {
        return new CursorImage(TEXTURE, 0, 0, 1, 100);
    }

    private static byte[] read(CursorSet set, Path configDirectory) throws IOException {
        IoSupplier<InputStream> supplier = CursorAssetSource.forImage(set, image(), configDirectory);
        assertNotNull(supplier);
        try (InputStream stream = supplier.get()) {
            return stream.readAllBytes();
        }
    }
}
