package com.fragmentedchaos.cursorkit.cursor.model;

import com.fragmentedchaos.cursorkit.cursor.state.CursorStateResolver;

/**
 * Everything the state resolution needs, gathered once per frame.
 * <p>
 * Minecraft-free on purpose: {@link CursorStateResolver} and its tests only ever see this record.
 *
 * @param dragging true while the left button drags something (slot drag, slider drag, ...)
 * @param busy     true while the game shows a loading overlay
 * @param vanilla  the cursor kind vanilla requested for this frame
 */
public record CursorContext(boolean dragging, boolean busy, VanillaCursor vanilla) {

    public static final CursorContext EMPTY = new CursorContext(false, false, VanillaCursor.DEFAULT);

    public CursorContext {
        if (vanilla == null) {
            vanilla = VanillaCursor.DEFAULT;
        }
    }
}
