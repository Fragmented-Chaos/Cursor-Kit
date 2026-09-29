package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.cursor.state.CursorStateProviders;
import com.fragmentedchaos.cursorkit.cursor.state.CursorStateResolver;
import com.fragmentedchaos.cursorkit.cursor.model.CursorContext;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.cursor.model.VanillaCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Per-frame cursor state: captures what vanilla requested, adds the two things vanilla cannot
 * express (dragging, busy) and runs our own state resolution.
 */
public final class CursorStateTracker {

    private static volatile VanillaCursor requested = VanillaCursor.DEFAULT;

    private CursorStateTracker() {
        throw new UnsupportedOperationException("CursorStateTracker cannot be instantiated");
    }

    /** Called from the {@code GuiGraphicsExtractor} mixin once per frame, with the final request. */
    public static void setRequested(VanillaCursor cursor) {
        requested = (cursor == null) ? VanillaCursor.DEFAULT : cursor;
    }

    /** @return the cursor kind vanilla asked for during the last rendered frame */
    public static VanillaCursor requested() {
        return requested;
    }

    /** @return what the mod should draw right now */
    public static CursorState currentState() {
        return resolve().state();
    }

    /** @return the client's mouse handler, or {@code null} before the client finished starting */
    public static MouseHandler mouse() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.mouseHandler;
    }

    /** @return the state and the screen it applies to; the screen is null when none is open */
    public static Resolved resolve() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = (minecraft == null || minecraft.gui == null) ? null : minecraft.gui.screen();
        if (screen == null || minecraft == null) {
            return new Resolved(null, CursorState.DEFAULT, CursorContext.EMPTY);
        }

        boolean dragging = screen.isDragging()
                || (minecraft.mouseHandler != null && minecraft.mouseHandler.isLeftPressed());
        boolean busy = minecraft.gui.overlay() != null;

        CursorContext context = new CursorContext(dragging, busy, requested);
        @Nullable CursorState provided = CursorStateProviders.firstMatch(screen, 0.0D, 0.0D, context);
        return new Resolved(screen, provided != null ? provided : CursorStateResolver.resolve(context),
                context);
    }

    /**
     * @param screen  the screen the state belongs to, {@code null} when no screen is open
     * @param state   the state to draw
     * @param context the raw detected context, useful for diagnostics
     */
    public record Resolved(@Nullable Screen screen, CursorState state, CursorContext context) {
    }
}
