package com.fragmentedchaos.cursorkit.cursor.model;

import com.fragmentedchaos.cursorkit.cursor.state.CursorStateResolver;
import org.jetbrains.annotations.Nullable;

/**
 * The cursor states a cursor set can provide.
 * <p>
 * <b>Declaration order is the resolution priority</b>: {@link CursorStateResolver} returns the
 * first state whose condition matches, so {@link #DRAG} outranks everything else and
 * {@link #DEFAULT} is the fallback.
 */
public enum CursorState {

    /** Left mouse button held while dragging something (item, slider). */
    DRAG("drag"),
    /** A text field / sign editor has focus. */
    TEXT("text"),
    /** The game is busy (loading), the only state that animates by default. */
    BUSY("busy"),
    /** The hovered widget is inactive or the item cannot be placed. */
    DISABLED("disabled"),
    /** The pointer hovers an interactive widget (button, slot, slider...). */
    CLICKABLE("clickable"),
    /** Nothing else matched; every set must provide this one. */
    DEFAULT("default");

    private final String id;

    CursorState(String id) {
        this.id = id;
    }

    /** The key used in cursor set JSON files. */
    public String id() {
        return this.id;
    }

    /** @return the state for a JSON key, or {@code null} when it is unknown */
    public static @Nullable CursorState byId(String id) {
        for (CursorState state : values()) {
            if (state.id.equals(id)) {
                return state;
            }
        }
        return null;
    }
}
