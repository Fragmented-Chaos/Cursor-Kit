package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetFormatException;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSetParser;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorSetParserTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void parsesMinimalSetWithDefaults() throws Exception {
        CursorSet set = CursorSetParser.parse("demo", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                { "states": { "default": { "texture": "arrow.png" } } }
                """));

        assertEquals("demo", set.name());
        assertEquals("cursorkit", set.namespace());
        assertEquals(1, set.scale());

        CursorImage image = set.image(CursorState.DEFAULT).orElseThrow();
        assertEquals("arrow.png", image.texture());
        // No hotspot in the JSON means "unset": a .cur file would bring its own, everything else
        // falls back to the top left corner.
        assertEquals(CursorImage.UNSET_HOTSPOT, image.hotspotX());
        assertEquals(CursorImage.UNSET_HOTSPOT, image.hotspotY());
        assertEquals(1, image.frames());
        assertFalse(image.animated());
    }

    @Test
    void explicitNameWinsOverFileName() throws Exception {
        CursorSet set = CursorSetParser.parse("file-name", "cursorkit", CursorSetOrigin.RESOURCE_PACK,
                "test", json("""
                { "name": "Display Name",
                  "states": { "default": { "texture": "arrow.png" } } }
                """));
        assertEquals("file-name", set.id());
        assertEquals("Display Name", set.name());
    }

    @Test
    void parsesAllSixStates() throws Exception {
        CursorSet set = CursorSetParser.parse("full", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                {
                  "states": {
                    "default":   { "texture": "arrow.png" },
                    "clickable": { "texture": "hand.png" },
                    "text":      { "texture": "ibeam.png", "hotspot": [4, 8] },
                    "drag":      { "texture": "grab.png" },
                    "busy":      { "texture": "busy.png", "frames": 8, "frame_ms": 80 },
                    "disabled":  { "texture": "disabled.png" }
                  }
                }
                """));

        for (CursorState state : CursorState.values()) {
            assertTrue(set.has(state), "missing state " + state.id());
        }
        CursorImage text = set.image(CursorState.TEXT).orElseThrow();
        assertEquals(4, text.hotspotX());
        assertEquals(8, text.hotspotY());
    }

    @Test
    void unknownStatesAreIgnored() throws Exception {
        CursorSet set = CursorSetParser.parse("demo", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                { "states": {
                    "default": { "texture": "arrow.png" },
                    "definitely-not-a-state": { "texture": "x.png" } } }
                """));
        assertEquals(1, set.images().size());
    }

    @Test
    void missingStatesAreAnError() {
        assertThrows(CursorSetFormatException.class, () -> CursorSetParser.parse(
                "broken", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("{ }")));
    }

    @Test
    void missingDefaultIsAnError() {
        assertThrows(CursorSetFormatException.class, () -> CursorSetParser.parse(
                "broken", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                        { "states": { "clickable": { "texture": "hand.png" } } }
                        """)));
    }

    @Test
    void missingTextureIsAnError() {
        assertThrows(CursorSetFormatException.class, () -> CursorSetParser.parse(
                "broken", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                        { "states": { "default": { "hotspot": [1, 1] } } }
                        """)));
    }

    @Test
    void wrongTypesAreAnError() {
        assertThrows(CursorSetFormatException.class, () -> CursorSetParser.parse(
                "broken", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                        { "states": { "default": { "texture": "a.png", "frames": "many" } } }
                        """)));
    }

    @Test
    void statesFallBackToDefault() throws Exception {
        CursorSet set = CursorSetParser.parse("demo", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                { "states": { "default": { "texture": "arrow.png" } } }
                """));
        assertEquals("arrow.png", set.image(CursorState.BUSY).orElseThrow().texture());
    }

    @Test
    void animationFrameTimingWrapsAround() {
        CursorImage image = new CursorImage("busy.png", 0, 0, 4, 100);
        assertTrue(image.animated());
        assertEquals(400L, image.animationLengthMs());
        assertEquals(0, image.frameAt(0));
        assertEquals(0, image.frameAt(99));
        assertEquals(1, image.frameAt(100));
        assertEquals(3, image.frameAt(399));
        assertEquals(0, image.frameAt(400));
        assertEquals(2, image.frameAt(1050));
    }

    @Test
    void staticImageAlwaysReportsFrameZero() {
        CursorImage image = new CursorImage("arrow.png", 0, 0, 1, 100);
        assertFalse(image.animated());
        assertEquals(0L, image.animationLengthMs());
        assertEquals(0, image.frameAt(12345));
    }

    @Test
    void hotspotMustNotBeNegative() {
        assertThrows(CursorSetFormatException.class, () -> CursorSetParser.parse(
                "broken", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                        { "states": { "default": { "texture": "a.png", "hotspot": [-1, 0] } } }
                        """)));
    }

    @Test
    void scaleMustBePositive() {
        assertThrows(CursorSetFormatException.class, () -> CursorSetParser.parse(
                "broken", "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", json("""
                        { "scale": 0, "states": { "default": { "texture": "a.png" } } }
                        """)));
    }
}
