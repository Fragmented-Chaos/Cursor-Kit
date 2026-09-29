package com.fragmentedchaos.cursorkit.cursor.state;

import com.fragmentedchaos.cursorkit.cursor.model.CursorContext;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.cursor.model.VanillaCursor;

/**
 * Decides which {@link CursorState} to show, using vanilla's own cursor request plus the two
 * situations vanilla cannot express (dragging, loading).
 * <p>
 * The priority is exactly the declaration order of {@link CursorState}:
 * {@code drag > text > busy > disabled > clickable > default}.
 * <p>
 * Minecraft-free so the whole priority table is unit tested.
 */
public final class CursorStateResolver {

    private CursorStateResolver() {
        throw new UnsupportedOperationException("CursorStateResolver cannot be instantiated");
    }

    public static CursorState resolve(CursorContext context) {
        if (context == null) {
            return CursorState.DEFAULT;
        }
        if (context.dragging()) {
            return CursorState.DRAG;
        }
        if (context.vanilla() == VanillaCursor.IBEAM) {
            return CursorState.TEXT;
        }
        if (context.busy()) {
            return CursorState.BUSY;
        }
        if (context.vanilla() == VanillaCursor.NOT_ALLOWED) {
            return CursorState.DISABLED;
        }
        if (isClickable(context.vanilla())) {
            return CursorState.CLICKABLE;
        }
        return CursorState.DEFAULT;
    }

    /** Vanilla uses the resize cursors for draggable edges; those read as "clickable" to us. */
    private static boolean isClickable(VanillaCursor cursor) {
        return cursor == VanillaCursor.POINTING_HAND
                || cursor == VanillaCursor.RESIZE_NS
                || cursor == VanillaCursor.RESIZE_EW
                || cursor == VanillaCursor.RESIZE_ALL;
    }
}
