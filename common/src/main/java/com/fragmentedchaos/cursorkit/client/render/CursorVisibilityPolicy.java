package com.fragmentedchaos.cursorkit.client.render;

/**
 * Decides whether the mod may take over the cursor for this frame.
 * <p>
 * The system cursor is only hidden while we are certain we can draw ours: the window is focused,
 * the mouse is not locked into "look around" mode and the pointer is comfortably inside the window.
 * Near the window border the system cursor is left alone so it can never get lost, and we skip
 * drawing ours in that case to avoid two visible pointers.
 * <p>
 * Minecraft-free on purpose, so every rule below is unit tested.
 */
public final class CursorVisibilityPolicy {

    /** Distance from the window border, in GUI-scaled pixels, where the system cursor stays. */
    public static final int DEFAULT_EDGE_MARGIN = 2;

    private CursorVisibilityPolicy() {
        throw new UnsupportedOperationException("CursorVisibilityPolicy cannot be instantiated");
    }

    public static boolean shouldTakeOver(boolean windowFocused, boolean mouseGrabbed,
                                         double mouseX, double mouseY,
                                         int guiWidth, int guiHeight) {
        return shouldTakeOver(windowFocused, mouseGrabbed, mouseX, mouseY, guiWidth, guiHeight,
                DEFAULT_EDGE_MARGIN);
    }

    public static boolean shouldTakeOver(boolean windowFocused, boolean mouseGrabbed,
                                         double mouseX, double mouseY,
                                         int guiWidth, int guiHeight, int edgeMargin) {
        return shouldTakeOver(windowFocused, mouseGrabbed, mouseX, mouseY, guiWidth, guiHeight,
                edgeMargin, false);
    }

    /**
     * @param screenWantsSystemCursor true while a screen that needs the real cursor is open, for
     *                                example the click point editor
     */
    public static boolean shouldTakeOver(boolean windowFocused, boolean mouseGrabbed,
                                         double mouseX, double mouseY,
                                         int guiWidth, int guiHeight, int edgeMargin,
                                         boolean screenWantsSystemCursor) {
        return shouldTakeOver(windowFocused, mouseGrabbed, mouseX, mouseY, guiWidth, guiHeight,
                edgeMargin, screenWantsSystemCursor, true);
    }

    /**
     * @param drawable true when the selected set can actually draw something for the current state.
     *                 The hand-made set is offered before any path is usable, and a pack may ship no
     *                 image at all, so this has to be part of the decision: hiding the system cursor
     *                 and then drawing nothing would leave the player without any pointer.
     */
    public static boolean shouldTakeOver(boolean windowFocused, boolean mouseGrabbed,
                                         double mouseX, double mouseY,
                                         int guiWidth, int guiHeight, int edgeMargin,
                                         boolean screenWantsSystemCursor, boolean drawable) {
        if (!drawable || screenWantsSystemCursor) {
            return false;
        }
        if (!windowFocused || mouseGrabbed) {
            return false;
        }
        if (guiWidth <= 0 || guiHeight <= 0) {
            return false;
        }
        if (mouseX < edgeMargin || mouseY < edgeMargin) {
            return false;
        }
        if (mouseX > guiWidth - edgeMargin || mouseY > guiHeight - edgeMargin) {
            return false;
        }
        return true;
    }
}
