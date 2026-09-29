package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Compile-only stub of Sodium's configuration API, see
 * {@link net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint}.
 * <p>
 * A page that is not built from options but simply opens a screen - which is how the cursor picker is
 * added to Sodium's list.
 */
public interface ExternalPageBuilder extends PageBuilder {

    ExternalPageBuilder setName(Component name);

    ExternalPageBuilder setScreenConsumer(Consumer<Screen> consumer);
}
