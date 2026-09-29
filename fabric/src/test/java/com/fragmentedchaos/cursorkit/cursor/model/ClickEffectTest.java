package com.fragmentedchaos.cursorkit.cursor.model;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetFormatException;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The click feedback a cursor set can ask for: switching sets switches the effect.
 */
class ClickEffectTest {

    private static ClickEffect parse(String json) throws CursorSetFormatException {
        return ClickEffect.parse(JsonParser.parseString(json), "test");
    }

    @Test
    void aSetWithoutOneGetsTheDefault() throws Exception {
        assertEquals(ClickEffect.DEFAULT, ClickEffect.parse(null, "test"));
        assertEquals(ClickEffect.DEFAULT, parse("{}"));
    }

    @Test
    void everyFieldCanBeSet() throws Exception {
        ClickEffect effect = parse("""
                { "type": "burst", "color": "#FFD479", "radius": 24, "duration_ms": 600, "particles": 10 }
                """);

        assertEquals("burst", effect.type());
        assertEquals(0xFFD479, effect.color());
        assertEquals(24.0F, effect.radius(), 0.001F);
        assertEquals(600L, effect.durationMs());
        assertEquals(10, effect.particles());
    }

    @Test
    void coloursAreAcceptedInSeveralShapes() throws Exception {
        assertEquals(0xFFD479, parse("{\"color\": \"#ffd479\"}").color());
        assertEquals(0xFFD479, parse("{\"color\": \"FFD479\"}").color());
        assertEquals(0x123456, parse("{\"color\": 1193046}").color());
    }

    @Test
    void nonsenseValuesAreRejected() {
        assertThrows(CursorSetFormatException.class, () -> parse("{\"type\": \"sparkles\"}"));
        assertThrows(CursorSetFormatException.class, () -> parse("{\"color\": \"blue\"}"));
        assertThrows(CursorSetFormatException.class, () -> parse("{\"radius\": \"wide\"}"));
        assertThrows(CursorSetFormatException.class, () -> parse("[]"));
    }

    @Test
    void valuesAreClampedToSomethingDrawable() throws Exception {
        assertEquals(1.0F, parse("{\"radius\": -5}").radius(), 0.001F);
        assertEquals(30L, parse("{\"duration_ms\": 1}").durationMs());
        assertEquals(0, parse("{\"particles\": -3}").particles());
    }

    @Test
    void aSetCanBringItsOwnAnimation() throws Exception {
        ClickEffect effect = parse("""
                { "type": "image", "texture": "click/ring.png", "frames": 6, "frame_ms": 45, "size": 48 }
                """);

        assertTrue(effect.isImage());
        assertEquals("click/ring.png", effect.asImage().texture());
        assertEquals(6, effect.asImage().frames());
        assertEquals(45, effect.asImage().frameMs());
        assertEquals(6 * 45L, effect.durationMs(),
                "without an explicit duration the animation's own length is its life");
        assertEquals(48, effect.size());
    }

    @Test
    void anImageEffectWithoutATextureIsRejected() {
        assertThrows(CursorSetFormatException.class, () -> parse("{\"type\": \"image\"}"));
    }

    @Test
    void onlyTheImageTypeHasAnImage() throws Exception {
        assertNull(ClickEffect.DEFAULT.asImage());
        assertNotNull(parse("{\"type\": \"image\", \"texture\": \"a.png\"}").asImage());
    }

    @Test
    void typeNoneDrawsNothing() throws Exception {
        ClickEffect effect = parse("{\"type\": \"none\"}");

        assertTrue(effect.disabled());
        assertFalse(ClickEffect.DEFAULT.disabled());
    }
}
