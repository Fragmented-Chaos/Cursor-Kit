package com.fragmentedchaos.cursorkit.cursor.state;

import com.fragmentedchaos.cursorkit.cursor.model.CursorContext;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for {@link CursorStateProvider}s. Copy-on-write, so mods may register from any thread.
 */
public final class CursorStateProviders {

    private static final List<CursorStateProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private CursorStateProviders() {
        throw new UnsupportedOperationException("CursorStateProviders cannot be instantiated");
    }

    /** Registers a provider; the earliest registered provider gets the first say. */
    public static void register(CursorStateProvider provider) {
        if (provider != null) {
            PROVIDERS.add(provider);
        }
    }

    /** @return the providers in registration order */
    public static List<CursorStateProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    /**
     * @return the first state a provider claims, or {@code null} when every provider declined
     */
    public static @Nullable CursorState firstMatch(Screen screen, double mouseX, double mouseY,
                                                   CursorContext context) {
        for (CursorStateProvider provider : PROVIDERS) {
            CursorState state = provider.stateFor(screen, mouseX, mouseY, context);
            if (state != null) {
                return state;
            }
        }
        return null;
    }
}
