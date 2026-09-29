package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

/**
 * Compile-only stub of Sodium's configuration API, see
 * {@link net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint}.
 * <p>
 * Only what this mod calls is declared. Signatures must match Sodium's exactly, because the compiled
 * call sites in {@code CursorSodiumConfig} are resolved against Sodium's real interfaces at runtime.
 */
public interface ConfigBuilder {

    ModOptionsBuilder registerOwnModOptions();

    ColorThemeBuilder createColorTheme();

    ExternalPageBuilder createExternalPage();

    OptionPageBuilder createOptionPage();

    ExternalButtonOptionBuilder createExternalButtonOption(Identifier icon);
}
