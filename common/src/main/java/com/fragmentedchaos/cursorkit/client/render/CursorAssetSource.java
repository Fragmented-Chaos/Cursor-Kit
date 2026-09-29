package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Opens the bytes of a cursor set asset.
 * <p>
 * Cursor packs and the loose files in {@code config/cursorkit/} are read straight from disk. That is
 * deliberate: Fabric only exposes mod resources to the game through Fabric API's resource loader,
 * and requiring Fabric API would make the mod refuse to load on Quilt (which needs QFAPI for it).
 * Reading our own files keeps the mod dependency-free and behaves identically on Fabric, NeoForge
 * and Quilt.
 * <p>
 * Cursor sets that come from a resource pack are left to Minecraft's own texture loader.
 */
public final class CursorAssetSource {

    /** Every cursor pack keeps the resource pack layout, so this prefix is shared. */
    private static final String ASSETS_PREFIX = "assets/";

    private CursorAssetSource() {
        throw new UnsupportedOperationException("CursorAssetSource cannot be instantiated");
    }

    /**
     * @param configDirectory the {@code config/cursorkit} directory, may be {@code null}
     * @return a supplier for the image's bytes, or {@code null} when the set is served by a resource
     *         pack instead
     */
    public static @Nullable IoSupplier<InputStream> forImage(CursorSet set, CursorImage image,
                                                             @Nullable Path configDirectory) {
        if (set.origin() == CursorSetOrigin.RESOURCE_PACK) {
            // Resource packs are Minecraft's own business: its texture manager finds
            // assets/<namespace>/textures/cursor/... inside the pack. Offering a classpath
            // supplier here would fail and take the whole resource reload down with it.
            return null;
        }
        if (set.origin() == CursorSetOrigin.CUSTOM_STATES) {
            // The image's texture field is the absolute path of the file the player picked.
            Path file = Path.of(image.texture());
            return Files.isRegularFile(file) ? () -> Files.newInputStream(file) : null;
        }
        if (set.origin() == CursorSetOrigin.CONFIG) {
            if (configDirectory == null) {
                return null;
            }
            Path file = configDirectory.resolve(image.texture());
            return Files.isRegularFile(file) ? () -> Files.newInputStream(file) : null;
        }
        if (set.origin() == CursorSetOrigin.CONFIG_PACK) {
            if (configDirectory == null) {
                return null;
            }
            // set.source() is "packs/<name>" or "packs/<name>.zip", relative to the config
            // directory, and a cursor pack keeps the resource pack layout.
            Path pack = configDirectory.resolve(set.source());
            String entry = ASSETS_PREFIX + set.namespace() + "/"
                    + CursorTextures.TEXTURE_DIRECTORY + image.texture();
            if (Files.isDirectory(pack)) {
                Path file = pack.resolve(entry);
                return Files.isRegularFile(file) ? () -> Files.newInputStream(file) : null;
            }
            return fromArchive(pack, entry);
        }
        // Anything else is served by a source the caller does not know about.
        return null;
    }

    /**
     * Opens one entry of a cursor pack that is stored as a {@code .zip}. A pack that is missing,
     * unreadable or does not contain the entry is reported as "no texture" instead of throwing,
     * because an exception here would abort Minecraft's whole resource reload.
     *
     * @return a supplier for the entry's bytes, or {@code null} when it cannot be read
     */
    private static @Nullable IoSupplier<InputStream> fromArchive(Path archive, String entryName) {
        if (!Files.isRegularFile(archive)) {
            return null;
        }
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            if (zip.getEntry(entryName) == null) {
                return null;
            }
        } catch (IOException e) {
            Constants.LOG.warn("Could not read cursor pack {}: {}", archive, e.toString());
            return null;
        }
        return () -> {
            // The archive has to stay open while its entry is being read, so the stream closes both.
            ZipFile zip = new ZipFile(archive.toFile());
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                zip.close();
                throw new IOException("missing " + entryName + " in " + archive);
            }
            return new FilterInputStream(zip.getInputStream(entry)) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        zip.close();
                    }
                }
            };
        };
    }
}
