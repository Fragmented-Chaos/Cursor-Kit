package com.fragmentedchaos.cursorkit.client.render;

import org.lwjgl.sdl.SDLMouse;

/**
 * Hides / restores the system cursor through SDL.
 * <p>
 * Minecraft 26.3 moved from GLFW to SDL and never hides the cursor itself (in-game it uses relative
 * mouse mode instead), so {@code SDL_HideCursor} is the only way to get rid of the pointer while a
 * screen is open. Calls are only issued when the desired state changes.
 */
public final class CursorVisibility {

    private static boolean hidden;

    private CursorVisibility() {
        throw new UnsupportedOperationException("CursorVisibility cannot be instantiated");
    }

    /** @param hide true to hide the system cursor, false to show it again */
    public static void setHidden(boolean hide) {
        if (hide == hidden) {
            return;
        }
        if (hide) {
            SDLMouse.SDL_HideCursor();
        } else {
            SDLMouse.SDL_ShowCursor();
        }
        hidden = hide;
    }

    /** @return true while the system cursor is hidden by this mod */
    public static boolean isHidden() {
        return hidden;
    }
}
