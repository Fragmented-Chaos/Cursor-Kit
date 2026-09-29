package com.fragmentedchaos.cursorkit.cursor.config;

import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies the click points the player edited in the picker on top of the loaded cursor sets.
 * <p>
 * Overrides live in {@code config/cursorkit.json} and are keyed by set id and state id, so they
 * survive restarts and keep working for sets that come from a read-only resource pack. A set without
 * an override keeps the hotspot from its own JSON.
 * <p>
 * Minecraft-free, so the merging rules are unit tested.
 */
public final class CursorHotspots {

    private CursorHotspots() {
        throw new UnsupportedOperationException("CursorHotspots cannot be instantiated");
    }

    /**
     * Combines one configuration with the click points of another.
     * <p>
     * The picker's "Cancel" restores the settings it was opened with, but click points are edited in
     * their own screen and confirmed there, so they have to survive that restore.
     *
     * @param base  the configuration to keep everything but the click points from
     * @param edits the configuration whose click points win
     * @return a copy of {@code base} carrying {@code edits}' click points
     */
    public static CursorConfig mergedInto(CursorConfig base, CursorConfig edits) {
        CursorConfig merged = base.copy();
        merged.replaceHotspots(edits == null ? Map.of() : edits.hotspots());
        return merged;
    }

    /**
     * @param sets   the sets as they were loaded
     * @param config the configuration holding the edited click points
     * @return the same sets with every edited click point applied, or {@code sets} when there is
     *         nothing to apply
     */
    public static List<CursorSet> apply(List<CursorSet> sets, CursorConfig config) {
        Map<String, Map<String, int[]>> overrides = config == null ? Map.of() : config.hotspots();
        if (overrides.isEmpty()) {
            return sets;
        }
        List<CursorSet> result = new ArrayList<>(sets.size());
        for (CursorSet set : sets) {
            Map<String, int[]> states = overrides.get(set.id());
            if (states == null || states.isEmpty()) {
                result.add(set);
                continue;
            }
            CursorSet updated = set;
            for (Map.Entry<String, int[]> entry : states.entrySet()) {
                CursorState state = CursorState.byId(entry.getKey());
                if (state == null) {
                    continue;
                }
                updated = updated.withHotspot(state, entry.getValue()[0], entry.getValue()[1]);
            }
            result.add(updated);
        }
        return List.copyOf(result);
    }
}
