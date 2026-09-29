package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.network.chat.Component;

/**
 * Compile-only stub of Sodium's configuration API, see
 * {@link net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint}.
 * <p>
 * A page built from options. Sodium does not put the arrow it prefixes external pages with on these,
 * which is why the cursor entry is an option page holding a single "open the picker" button.
 */
public interface OptionPageBuilder extends PageBuilder {

    OptionPageBuilder setName(Component name);

    OptionPageBuilder addOption(OptionBuilder option);
}
