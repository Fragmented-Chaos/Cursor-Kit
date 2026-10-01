package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.client.render.CursorVisibilityPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorVisibilityPolicyTest {

    private static final int W = 320;
    private static final int H = 240;

    @Test
    void middleOfTheWindowIsTakenOver() {
        assertTrue(CursorVisibilityPolicy.shouldTakeOver(true, false, 160, 120, W, H));
    }

    @Test
    void unfocusedWindowIsNeverTakenOver() {
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(false, false, 160, 120, W, H));
    }

    @Test
    void lockedMouseIsNeverTakenOver() {
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, true, 160, 120, W, H));
    }

    @Test
    void outsideTheWindowIsNeverTakenOver() {
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, -5, 120, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 160, -5, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, W + 5, 120, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 160, H + 5, W, H));
    }

    @Test
    void windowBorderKeepsTheSystemCursor() {
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 0, 120, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 1, 120, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 160, 1, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, W - 1, 120, W, H));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 160, H - 1, W, H));
    }

    @Test
    void marginIsConfigurableAndInclusiveInside() {
        assertTrue(CursorVisibilityPolicy.shouldTakeOver(true, false, 4, 120, W, H, 4));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 3, 120, W, H, 4));
        assertTrue(CursorVisibilityPolicy.shouldTakeOver(true, false, 2, 120, W, H));
    }

    @Test
    void degenerateWindowSizeIsNeverTakenOver() {
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 0, 0, 0, 0));
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 0, 0, -10, 240));
    }

    @Test
    void aSetThatCannotDrawAnythingKeepsTheSystemCursor() {
        // The hand-made set is offered before any path is usable, and a pack may ship no image at
        // all: hiding the system cursor and then drawing nothing would leave no pointer on screen.
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 100.0D, 100.0D,
                400, 300, 2, false, false));
        assertTrue(CursorVisibilityPolicy.shouldTakeOver(true, false, 100.0D, 100.0D,
                400, 300, 2, false, true));
    }

    @Test
    void aScreenThatNeedsTheRealCursorIsLeftAlone() {
        // The click point editor asks for the system cursor, otherwise the custom one would cover
        // the pixel being clicked.
        assertFalse(CursorVisibilityPolicy.shouldTakeOver(true, false, 100.0D, 100.0D,
                400, 300, 2, true));
        assertTrue(CursorVisibilityPolicy.shouldTakeOver(true, false, 100.0D, 100.0D,
                400, 300, 2, false));
    }
}
