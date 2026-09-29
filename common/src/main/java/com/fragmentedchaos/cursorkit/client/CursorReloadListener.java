package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.client.render.CursorAssetSource;
import com.fragmentedchaos.cursorkit.client.render.CursorFile;
import com.fragmentedchaos.cursorkit.client.render.CursorGeometry;
import com.fragmentedchaos.cursorkit.client.render.CursorTexture;
import com.fragmentedchaos.cursorkit.client.render.CursorTextures;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Re-reads every cursor set whenever the client reloads its resources (startup, F3+T, pack
 * changes) and registers the textures this mod serves itself.
 * <p>
 * Registered through Minecraft's own {@code ReloadableResourceManager}, which keeps this free of
 * any loader specific API - the same code runs on Fabric, NeoForge and Quilt.
 */
public final class CursorReloadListener implements ResourceManagerReloadListener {

    public static final CursorReloadListener INSTANCE = new CursorReloadListener();

    private CursorReloadListener() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDirectory = configDirectory(minecraft);
        Path configFile = configFile(minecraft);

        CursorManager manager = CursorManager.get();
        manager.setGameDirectory(minecraft.gameDirectory == null ? null
                : minecraft.gameDirectory.toPath());
        manager.reload(resourceManager, configDirectory, configFile);
        measureFrameSizes(resourceManager, manager.sets(), configDirectory);
        registerTextures(minecraft, manager.sets(), configDirectory);
        CursorWatcher.remember(configDirectory);
    }

    /** @return {@code <game>/config/cursorkit}, or {@code null} when the client is not ready yet */
    public static Path configDirectory(Minecraft minecraft) {
        Path configRoot = configRoot(minecraft);
        return configRoot == null ? null : configRoot.resolve(Constants.MOD_ID);
    }

    /** @return {@code <game>/config/cursorkit.json}, or {@code null} when the client is not ready */
    public static Path configFile(Minecraft minecraft) {
        Path configRoot = configRoot(minecraft);
        return configRoot == null ? null : configRoot.resolve(CursorConfig.FILE_NAME);
    }

    private static Path configRoot(Minecraft minecraft) {
        if (minecraft == null || minecraft.gameDirectory == null) {
            return null;
        }
        return minecraft.gameDirectory.toPath().resolve("config");
    }

    /**
     * Registers the textures of whatever the manager holds right now.
     * <p>
     * Used after a live rescan - by {@link CursorWatcher} when a file appears on disk and by the
     * picker when the player edits one of the per-state paths.
     *
     * @param configDirectory the {@code config/cursorkit} directory
     */
    public static void rescanTextures(Minecraft minecraft, Path configDirectory) {
        registerTextures(minecraft, CursorManager.get().sets(), configDirectory);
    }

    /**
     * Reads the size of every cursor image.
     * <p>
     * Resource pack sets go through the resource manager here, because their textures are loaded by
     * Minecraft and never pass through {@link CursorTexture}: without this a 32x32 or 64x64 image in
     * a pack would still be treated as 16x16 and drawn as a crop.
     */
    static void measureFrameSizes(ResourceManager resourceManager, List<CursorSet> sets,
                                  Path configDirectory) {
        for (CursorSet set : sets) {
            for (CursorImage image : imagesOf(set)) {
                Identifier id = CursorTextures.resolve(set, image);
                if (id == null) {
                    continue;
                }
                try {
                    if (set.origin() == CursorSetOrigin.RESOURCE_PACK) {
                        List<Resource> stack = resourceManager.getResourceStack(id);
                        if (stack.isEmpty()) {
                            continue;
                        }
                        try (InputStream stream = stack.get(0).open()) {
                            measure(id, stream.readAllBytes());
                        }
                    } else {
                        IoSupplier<InputStream> supplier =
                                CursorAssetSource.forImage(set, image, configDirectory);
                        if (supplier == null) {
                            continue;
                        }
                        try (InputStream stream = supplier.get()) {
                            measure(id, stream.readAllBytes());
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.debug("Could not measure {}: {}", id, e.toString());
                }
            }
        }
    }

    /**
     * Every image a set brings: its states, plus the animation of its click effect when it has one.
     * <p>
     * The effect is measured and registered through exactly the same path, which is what lets a pack
     * ship its own click effect instead of picking one of the built-in shapes.
     */
    static List<CursorImage> imagesOf(CursorSet set) {
        List<CursorImage> images = new java.util.ArrayList<>(set.images().size() + 1);
        for (CursorState state : CursorState.values()) {
            CursorImage image = set.images().get(state);
            if (image != null) {
                images.add(image);
            }
        }
        CursorImage effect = set.clickEffect().asImage();
        if (effect != null) {
            images.add(effect);
        }
        return images;
    }

    /** Reads the size - and the click point, for a {@code .cur} - of one cursor image. */
    private static void measure(Identifier id, byte[] bytes) {
        if (CursorFile.isCursor(bytes)) {
            CursorFile.Entry entry = CursorFile.largest(bytes);
            if (entry != null) {
                CursorGeometry.record(id, entry.height(), 1);
                CursorGeometry.recordHotspot(id, entry.hotspotX(), entry.hotspotY());
            }
            return;
        }
        CursorGeometry.recordPngHeader(id, new java.io.ByteArrayInputStream(bytes));
    }

    /**
     * Registers every texture this mod serves from its own sources. Resource pack sets are skipped:
     * Minecraft's texture manager already finds those in the pack.
     * <p>
     * Also used by {@link CursorWatcher} when a cursor set appears on disk while the game runs.
     */
    static void registerTextures(Minecraft minecraft, List<CursorSet> sets, Path configDirectory) {
        if (minecraft == null) {
            return;
        }
        TextureManager textureManager = minecraft.getTextureManager();
        if (textureManager == null) {
            return;
        }
        for (CursorSet set : sets) {
            for (CursorImage image : imagesOf(set)) {
                IoSupplier<InputStream> source =
                        CursorAssetSource.forImage(set, image, configDirectory);
                if (source == null) {
                    continue;
                }
                Identifier id = CursorTextures.resolve(set, image);
                textureManager.registerAndLoad(id, new CursorTexture(id, source, image));
                Constants.LOG.debug("Registered cursor texture {} for {}", id, set.name());
            }
        }
    }
}
