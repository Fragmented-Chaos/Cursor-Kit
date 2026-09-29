package com.fragmentedchaos.cursorkit.client.gui;

/**
 * Shared button row arithmetic.
 * <p>
 * Small windows are the interesting case: the width has to be derived from what is actually
 * available, because a fixed minimum made four buttons wider than the window and they overlapped.
 */
public final class ScreenLayout {

    private ScreenLayout() {
        throw new UnsupportedOperationException("ScreenLayout cannot be instantiated");
    }

    /**
     * @param available the space the whole row may use
     * @param count     how many buttons the row holds
     * @param gap       space between two buttons
     * @return the width every button gets, at least one pixel
     */
    public static int buttonWidth(int available, int count, int gap) {
        int gaps = Math.max(0, count - 1) * gap;
        return Math.max(1, (available - gaps) / count);
    }

    /** @return the x of the button at {@code index} in a row that starts at {@code start} */
    public static int buttonX(int start, int index, int width, int gap) {
        return start + index * (width + gap);
    }
}
