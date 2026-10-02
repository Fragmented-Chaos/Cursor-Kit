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

    /** The screen the last frame resolved for, used to spot the frame a screen was opened on. */
    private static Screen lastScreen;

    /** True while the left button was already down when the current screen opened. */
    private static boolean staleLeftButton;

    /** @return the state and the screen it applies to; the screen is null when none is open */
    public static Resolved resolve() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = (minecraft == null || minecraft.gui == null) ? null : minecraft.gui.screen();
        if (screen == null || minecraft == null) {
            return new Resolved(null, CursorState.DEFAULT, CursorContext.EMPTY);
        }

        // A screen is usually opened by the very click that is still held down (attack, use, open
        // the inventory): counting that press would leave the cursor stuck in the grabbing state
        // until the player lets go, so it is ignored until the button is released once.
        boolean leftHeld = minecraft.mouseHandler != null && minecraft.mouseHandler.isLeftPressed();
        if (screen != lastScreen) {
            lastScreen = screen;
            staleLeftButton = leftHeld;
        } else if (!leftHeld) {
            staleLeftButton = false;
        }
        boolean dragging = screen.isDragging() || (leftHeld && !staleLeftButton);
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
