package com.fragmentedchaos.cursorkit.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Button rows must fit the window they are drawn in - a fixed minimum width made four footer buttons
 * wider than a small window, so they overlapped.
 */
class ScreenLayoutTest {

    @Test
    void aRowNeverBecomesWiderThanTheSpaceItHas() {
        for (int available = 40; available <= 900; available += 7) {
            for (int count : new int[] {3, 4}) {
                int gap = 4;
                int width = ScreenLayout.buttonWidth(available, count, gap);
                int used = count * width + (count - 1) * gap;

                assertTrue(used <= available,
                        "available=" + available + " count=" + count + " used=" + used);
                assertTrue(width >= 1);
            }
        }
    }

    @Test
    void buttonsSitNextToEachOtherWithoutTouching() {
        int width = ScreenLayout.buttonWidth(400, 4, 4);

        assertEquals(97, width);
        assertEquals(12, ScreenLayout.buttonX(12, 0, width, 4));
        assertEquals(113, ScreenLayout.buttonX(12, 1, width, 4));
        assertEquals(214, ScreenLayout.buttonX(12, 2, width, 4));
        assertEquals(315, ScreenLayout.buttonX(12, 3, width, 4));
        assertEquals(412, ScreenLayout.buttonX(12, 3, width, 4) + width);
    }

    @Test
    void tinyWindowsStillGetUsableButtons() {
        assertEquals(1, ScreenLayout.buttonWidth(2, 4, 4));
        assertEquals(17, ScreenLayout.buttonWidth(80, 4, 3));   // 4*17 + 3*3 = 77 <= 80
    }
}
