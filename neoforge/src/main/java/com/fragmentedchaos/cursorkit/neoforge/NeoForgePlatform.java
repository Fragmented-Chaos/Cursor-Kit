package com.fragmentedchaos.cursorkit.neoforge;

import com.fragmentedchaos.cursorkit.client.CursorPlatform;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * The loader facts on NeoForge, taken from NeoForge's own metadata.
 * <p>
 * NeoForge registers itself in its mod list under this id, the same list it shows in its mods
 * screen, so the name and version here are the ones a player would see there.
 */
final class NeoForgePlatform implements CursorPlatform {

    /** NeoForge publishes itself as a mod under this id. */
    private static final String LOADER_ID = "neoforge";

    @Override
    public String loaderName() {
        return container().map(container -> container.getModInfo().getDisplayName())
                .orElse("NeoForge");
    }

    @Override
    public String loaderVersion() {
        return container().map(container -> container.getModInfo().getVersion().toString()).orElse("");
    }

    private static Optional<? extends ModContainer> container() {
        try {
            return ModList.get().getModContainerById(LOADER_ID);
        } catch (RuntimeException | LinkageError e) {
            // Asked before the mod list exists, which should not happen from a mod constructor; the
            // fallbacks keep the about popup readable if it ever does.
            return Optional.empty();
        }
    }
}
