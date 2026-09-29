package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.client.render.CursorRenderer;

/**
 * Marker for screens that want the window's own cursor while they are open.
 * <p>
 * The click point editor implements this: the custom cursor is exactly what the player is trying to
 * position, so it would cover the pixel about to be clicked. The custom cursor comes back as soon as
 * another screen is shown, because {@link CursorRenderer} only has to look at the screen that is
 * open right now.
 */
public interface SystemCursorScreen {
}
