package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.client.gui.CursorStatesScreen;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The parts of the picker's effect controls that are logic rather than drawing.
 */
class ClickEffectControlsTest {

    @Test
    void cyclingWalksThroughEveryEffectAndBack() {
        ClickEffect effect = ClickEffect.DEFAULT;
        List<String> seen = new ArrayList<>();
        for (int step = 0; step < 5; step++) {
            effect = CursorStatesScreen.nextEffect(effect);
            seen.add(effect.type());
        }

        assertEquals(List.of("burst", "pulse", "image", "none", "ripple"), seen,
                "every effect is reachable and the last step wraps around");
    }

    @Test
    void theImageStepAsksForAnAnimation() {
        ClickEffect image = CursorStatesScreen.nextEffect(
                CursorStatesScreen.nextEffect(CursorStatesScreen.nextEffect(ClickEffect.DEFAULT)));

        assertEquals("image", image.type());
        assertTrue(image.isImage());
        assertTrue(image.durationMs() > 0L, "and has a length to play");
    }

    @Test
    void cyclingKeepsTheColourButGoesBackToTheDefaultShape() {
        ClickEffect custom = new ClickEffect("ripple", 0x123456, 40.0F, 900L, 12, "", 1, 60, 32);

        ClickEffect next = CursorStatesScreen.nextEffect(custom);

        assertEquals(0x123456, next.color(), "the colour the player picked sticks");
        assertEquals(ClickEffect.DEFAULT.radius(), next.radius(), 0.001F);
    }

    @Test
    void coloursAreReadBackFromTheField() {
        assertEquals(0xFFD479, CursorStatesScreen.parseColour("#FFD479"));
        assertEquals(0xFFD479, CursorStatesScreen.parseColour("ffd479"));
        assertNull(CursorStatesScreen.parseColour("#FFD47"), "half typed colours are ignored");
        assertNull(CursorStatesScreen.parseColour("blue"));
        assertNull(CursorStatesScreen.parseColour(null));
    }
}
