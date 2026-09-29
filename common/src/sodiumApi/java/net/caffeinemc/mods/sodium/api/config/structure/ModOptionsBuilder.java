package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

/**
 * Compile-only stub of Sodium's configuration API, see
 * {@link net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint}.
 */
public interface ModOptionsBuilder {

    ModOptionsBuilder setName(String name);

    ModOptionsBuilder setIcon(Identifier icon);

    ModOptionsBuilder setColorTheme(ColorThemeBuilder theme);

    ModOptionsBuilder addPage(PageBuilder page);
}
