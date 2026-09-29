package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.cursor.model.VanillaCursor;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

/**
 * Maps vanilla's {@link CursorType} singletons onto our Minecraft-free {@link VanillaCursor}.
 * <p>
 * Identity comparison is correct here: {@link CursorTypes} holds static singletons.
 */
public final class VanillaCursorMapper {

    private VanillaCursorMapper() {
        throw new UnsupportedOperationException("VanillaCursorMapper cannot be instantiated");
    }

    public static VanillaCursor map(CursorType type) {
        if (type == null || type == CursorTypes.ARROW) {
            return VanillaCursor.DEFAULT;
        }
        if (type == CursorTypes.IBEAM) {
            return VanillaCursor.IBEAM;
        }
        if (type == CursorTypes.POINTING_HAND) {
            return VanillaCursor.POINTING_HAND;
        }
        if (type == CursorTypes.NOT_ALLOWED) {
            return VanillaCursor.NOT_ALLOWED;
        }
        if (type == CursorTypes.CROSSHAIR) {
            return VanillaCursor.CROSSHAIR;
        }
        if (type == CursorTypes.RESIZE_NS) {
            return VanillaCursor.RESIZE_NS;
        }
        if (type == CursorTypes.RESIZE_EW) {
            return VanillaCursor.RESIZE_EW;
        }
        if (type == CursorTypes.RESIZE_ALL) {
            return VanillaCursor.RESIZE_ALL;
        }
        return VanillaCursor.DEFAULT;
    }
}
