package com.fragmentedchaos.cursorkit.cursor.state;

import com.fragmentedchaos.cursorkit.cursor.model.CursorContext;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Public extension point: lets another mod force a cursor state for the current frame.
 * <p>
 * Providers are asked in registration order <b>before</b> the built-in resolution, so the first one
 * that returns a non-null state wins. Return {@code null} to fall through to vanilla's own
 * detection.
 */
@FunctionalInterface
public interface CursorStateProvider {

    /**
     * @param screen  the screen being rendered, never {@code null} while this is called
     * @param mouseX  cursor x in GUI-scaled coordinates
     * @param mouseY  cursor y in GUI-scaled coordinates
     * @param context the detected context, for providers that only want to refine it
     * @return the state to use, or {@code null} to keep the built-in resolution
     */
    @Nullable
    CursorState stateFor(Screen screen, double mouseX, double mouseY, CursorContext context);
}
