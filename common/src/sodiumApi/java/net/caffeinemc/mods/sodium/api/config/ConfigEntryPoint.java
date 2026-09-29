package net.caffeinemc.mods.sodium.api.config;

import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;

/**
 * Compile-only stub of Sodium's configuration API entry point.
 * <p>
 * This file is never compiled into the jar and never loaded at runtime: it exists so this mod's
 * {@code CursorSodiumConfig} can declare {@code implements ConfigEntryPoint} without Sodium being a
 * build dependency. At runtime Sodium's own interface - which lives under the same name - is used,
 * so the method signatures here have to stay identical to the ones in Sodium's API. Sodium declares
 * {@code registerConfigEarly} as a default method, so only {@code registerConfigLate} is needed.
 *
 * @see <a href="https://github.com/CaffeineMC/sodium">Sodium's configuration API</a>
 */
public interface ConfigEntryPoint {

    void registerConfigLate(ConfigBuilder builder);
}
