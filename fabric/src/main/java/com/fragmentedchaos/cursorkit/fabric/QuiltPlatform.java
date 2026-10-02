package com.fragmentedchaos.cursorkit.fabric;

import com.fragmentedchaos.cursorkit.client.CursorPlatform;
import com.fragmentedchaos.cursorkit.client.CursorPlatforms;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.Optional;

/**
 * The loader facts on Quilt, taken from Quilt Loader's own API.
 * <p>
 * Quilt starts this mod through its Fabric compatibility layer, so this class is chosen by
 * {@code CursorKitFabric} rather than by a module of its own. It is only loaded once
 * {@code CursorKitFabric} has found Quilt on the class path. Quilt Loader publishes its API in the
 * same jar that is running - it is a compile-time dependency, never a bundled one - so asking the
 * live loader is the same call here and at runtime.
 * <p>
 * Quilt's compatibility layer also answers Fabric's {@code getModContainer("fabricloader")}, but
 * with the Fabric API version it emulates ({@code 0.18.6}), not the Quilt version that is running.
 * That is why this asks Quilt directly.
 */
final class QuiltPlatform implements CursorPlatform {

    /** Quilt Loader publishes itself as a mod under this id. */
    private static final String LOADER_ID = "quilt_loader";

    @Override
    public String loaderName() {
        return container().map(container -> container.metadata().name()).orElse("Quilt Loader");
    }

    @Override
    public String loaderVersion() {
        return container().map(container -> container.metadata().version().raw()).orElse("");
    }

    private static Optional<ModContainer> container() {
        try {
            return QuiltLoader.getModContainer(LOADER_ID);
        } catch (RuntimeException e) {
            // Asked before the loader finished starting, which should not happen from an entry
            // point; the fallbacks keep the about popup readable if it ever does.
            return Optional.empty();
        }
    }

    /** Publishes this platform, unless another loader already claimed the game. */
    static void register() {
        CursorPlatforms.register(new QuiltPlatform());
    }
}
