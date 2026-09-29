package com.fragmentedchaos.cursorkit.cursor.config;

import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.config.CursorHotspots;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Click points the player edits in the picker are stored in the config and applied on top of the
 * loaded sets.
 */
class CursorHotspotsTest {

    @Test
    void editedHotspotReplacesTheOneFromTheSet() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("mine", "default", 3, 4);

        List<CursorSet> applied = CursorHotspots.apply(List.of(set("mine")), config);

        CursorImage image = applied.get(0).images().get(CursorState.DEFAULT);
        assertEquals(3, image.hotspotX());
        assertEquals(4, image.hotspotY());
        // The original set is untouched, the manager keeps it as the base for "reset".
        assertEquals(0, set("mine").images().get(CursorState.DEFAULT).hotspotX());
    }

    @Test
    void aClickPointForAFallbackStateLeavesDefaultAlone() {
        CursorConfig config = new CursorConfig();
        // "drag" has no picture of its own in this set: it borrows default's.
        config.setHotspot("mine", "drag", 4, 5);

        CursorSet result = CursorHotspots.apply(List.of(set("mine")), config).get(0);

        assertEquals(4, result.image(CursorState.DRAG).orElseThrow().hotspotX());
        assertEquals(5, result.image(CursorState.DRAG).orElseThrow().hotspotY());
        assertEquals(0, result.image(CursorState.DEFAULT).orElseThrow().hotspotX(),
                "editing one state must not move default");
    }

    @Test
    void setsWithoutAnEditKeepTheirOwnHotspot() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("other", "default", 9, 9);

        List<CursorSet> applied = CursorHotspots.apply(List.of(set("mine")), config);

        assertEquals(0, applied.get(0).images().get(CursorState.DEFAULT).hotspotX());
    }

    @Test
    void unknownStatesAndSetsAreIgnored() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("mine", "not-a-state", 1, 1);

        List<CursorSet> applied = CursorHotspots.apply(List.of(set("mine")), config);

        assertEquals(1, applied.size());
        assertEquals(0, applied.get(0).images().get(CursorState.DEFAULT).hotspotX());
    }

    @Test
    void aMissingEditClearsItAgain() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("mine", "default", 3, 4);
        config.clearHotspot("mine", "default");

        assertNull(config.hotspot("mine", "default"));
        assertTrue(CursorHotspots.apply(List.of(set("mine")), config).get(0).images()
                .get(CursorState.DEFAULT).hotspotX() == 0);
    }

    @Test
    void theConfigCopiesEditsDeeply() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("mine", "default", 3, 4);

        CursorConfig copy = config.copy();
        copy.setHotspot("mine", "default", 8, 9);
        config.clearHotspot("mine", "default");

        assertEquals(8, copy.hotspot("mine", "default")[0]);
        assertNull(config.hotspot("mine", "default"));
    }

    @Test
    void clearingOneStateKeepsTheOther() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("mine", "default", 1, 1);
        config.setHotspot("mine", "clickable", 2, 2);

        config.clearHotspot("mine", "default");

        assertNull(config.hotspot("mine", "default"));
        assertEquals(2, config.hotspot("mine", "clickable")[0]);
        assertEquals(1, config.hotspots().get("mine").size());
    }

    @Test
    void configRoundTripsThroughJson() {
        CursorConfig config = new CursorConfig();
        config.setHotspot("mine", "default", 5, 6);
        config.setSelectedSet("mine");

        CursorConfig reloaded = CursorConfig.fromJson(config.toJson());

        assertEquals(5, reloaded.hotspot("mine", "default")[0]);
        assertEquals(6, reloaded.hotspot("mine", "default")[1]);
        assertEquals("mine", reloaded.selectedSet());
    }

    @Test
    void brokenJsonEntriesAreSkipped() {
        var root = com.google.gson.JsonParser.parseString("""
                { "hotspots": { "mine": { "default": [1, 2], "busy": "nonsense" },
                                "other": 5 } }
                """).getAsJsonObject();

        CursorConfig config = CursorConfig.fromJson(root);

        assertEquals(1, config.hotspot("mine", "default")[0]);
        assertNull(config.hotspot("mine", "busy"));
        assertTrue(config.hotspots().getOrDefault("other", Map.of()).isEmpty());
    }

    private static CursorSet set(String id) {
        return new CursorSet(id, id, "cursorkit", CursorSetOrigin.RESOURCE_PACK, "test", 1,
                Map.of(CursorState.DEFAULT, new CursorImage("arrow.png", 0, 0, 1, 100)));
    }

    @Test
    void applyingNothingKeepsTheSameList() {
        List<CursorSet> sets = List.of(set("mine"));

        assertTrue(CursorHotspots.apply(sets, new CursorConfig()) == sets);
        assertFalse(CursorHotspots.apply(sets, null).isEmpty());
    }
}
