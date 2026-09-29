package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Compile-only stub of Sodium's configuration API, see
 * {@link net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint}.
 * <p>
 * A row that is not an option but a button opening a screen of its own.
 */
public interface ExternalButtonOptionBuilder extends OptionBuilder {

    ExternalButtonOptionBuilder setName(Component name);

    ExternalButtonOptionBuilder setTooltip(Component tooltip);

    ExternalButtonOptionBuilder setScreenConsumer(Consumer<Screen> consumer);
}
