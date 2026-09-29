package com.fragmentedchaos.cursorkit.cursor.model;

/**
 * Mirror of the cursor kinds vanilla requests each frame
 * ({@code com.mojang.blaze3d.platform.cursor.CursorTypes}).
 * <p>
 * Kept Minecraft-free so state resolution can be unit tested. Vanilla already decides most of this
 * for us - widgets request {@code POINTING_HAND}, text fields {@code IBEAM}, unusable targets
 * {@code NOT_ALLOWED} - which means custom screens from other mods are covered for free.
 */
public enum VanillaCursor {

    /** Nothing (or only the background) requested a specific cursor. */
    DEFAULT,
    /** A text field has focus. */
    IBEAM,
    /** The pointer is over something clickable. */
    POINTING_HAND,
    /** The target cannot be used right now. */
    NOT_ALLOWED,
    CROSSHAIR,
    RESIZE_NS,
    RESIZE_EW,
    RESIZE_ALL
}
