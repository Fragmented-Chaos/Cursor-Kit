package com.fragmentedchaos.cursorkit.fabric;

import com.fragmentedchaos.cursorkit.client.CursorPlatform;
import com.fragmentedchaos.cursorkit.client.CursorPlatforms;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Optional;

/**
 * The loader facts on Fabric, taken from Fabric Loader's own metadata.
 * <p>
 * Read rather than written down, so the about popup names the loader that is actually installed
 * instead of the version this mod was built against.
 */
final class FabricPlatform implements CursorPlatform {

    /** Fabric Loader publishes itself as a mod under this id. */
    private static final String LOADER_ID = "fabricloader";

    @Override
    public String loaderName() {
        return container().map(container -> container.getMetadata().getName())
                .orElse("Fabric Loader");
    }

    @Override
    public String loaderVersion() {
        return container().map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    private static Optional<ModContainer> container() {
        try {
            return FabricLoader.getInstance().getModContainer(LOADER_ID);
        } catch (RuntimeException e) {
            // Asked before the loader finished starting, which should not happen from an entry
            // point; the fallbacks keep the about popup readable if it ever does.
            return Optional.empty();
        }
    }

    /** Publishes this platform, unless another loader already claimed the game. */
    static void register() {
        CursorPlatforms.register(new FabricPlatform());
    }
}
