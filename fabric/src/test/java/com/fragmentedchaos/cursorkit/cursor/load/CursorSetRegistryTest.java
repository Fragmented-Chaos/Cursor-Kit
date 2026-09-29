package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetRegistry;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorSetRegistryTest {

    private static CursorSet set(String id, CursorSetOrigin origin) {
        return new CursorSet(id, id, "cursorkit", origin, origin.label(), 1,
                Map.of(CursorState.DEFAULT, new CursorImage("arrow.png", 0, 0, 1, 100)));
    }

    @Test
    void configBeatsResourcePackBeatsBuiltin() {
        List<CursorSet> merged = CursorSetRegistry.merge(List.of(
                set("demo", CursorSetOrigin.RESOURCE_PACK),
                set("demo", CursorSetOrigin.RESOURCE_PACK),
                set("demo", CursorSetOrigin.CONFIG)));

        assertEquals(1, merged.size());
        assertEquals(CursorSetOrigin.CONFIG, merged.get(0).origin());
    }

    @Test
    void resourcePackBeatsBuiltin() {
        List<CursorSet> merged = CursorSetRegistry.merge(List.of(
                set("demo", CursorSetOrigin.RESOURCE_PACK),
                set("demo", CursorSetOrigin.RESOURCE_PACK)));

        assertEquals(CursorSetOrigin.RESOURCE_PACK, merged.get(0).origin());
    }

    @Test
    void everyLayerBeatsTheOnesBelowIt() {
        List<CursorSetOrigin> order = List.of(CursorSetOrigin.CONFIG, CursorSetOrigin.CONFIG_PACK,
                CursorSetOrigin.RESOURCE_PACK, CursorSetOrigin.RESOURCE_PACK);

        for (int i = 0; i < order.size(); i++) {
            for (int j = i + 1; j < order.size(); j++) {
                CursorSetOrigin higher = order.get(i);
                CursorSetOrigin lower = order.get(j);
                assertEquals(higher, CursorSetRegistry.merge(List.of(
                        set("demo", lower), set("demo", higher))).get(0).origin(),
                        higher + " should beat " + lower);
                assertEquals(higher, CursorSetRegistry.merge(List.of(
                        set("demo", higher), set("demo", lower))).get(0).origin(),
                        higher + " should beat " + lower + " whatever the load order is");
            }
        }
    }

    @Test
    void builtinSurvivesWhenNothingOverridesIt() {
        List<CursorSet> merged = CursorSetRegistry.merge(List.of(
                set("alpha", CursorSetOrigin.RESOURCE_PACK),
                set("beta", CursorSetOrigin.RESOURCE_PACK)));

        assertEquals(List.of("alpha", "beta"), merged.stream().map(CursorSet::id).toList());
    }

    @Test
    void resultsAreSortedByName() {
        List<CursorSet> merged = CursorSetRegistry.merge(List.of(
                set("zeta", CursorSetOrigin.RESOURCE_PACK),
                set("alpha", CursorSetOrigin.RESOURCE_PACK),
                set("mid", CursorSetOrigin.RESOURCE_PACK)));

        assertEquals(List.of("alpha", "mid", "zeta"), merged.stream().map(CursorSet::id).toList());
    }

    @Test
    void findFallsBackToFirstSet() {
        List<CursorSet> sets = List.of(set("alpha", CursorSetOrigin.RESOURCE_PACK),
                set("beta", CursorSetOrigin.RESOURCE_PACK));

        assertEquals("beta", CursorSetRegistry.find(sets, "beta").orElseThrow().id());
        assertEquals("alpha", CursorSetRegistry.find(sets, "missing").orElseThrow().id());
        assertEquals("alpha", CursorSetRegistry.find(sets, null).orElseThrow().id());
        assertTrue(CursorSetRegistry.find(List.of(), "anything").isEmpty());
    }
}
