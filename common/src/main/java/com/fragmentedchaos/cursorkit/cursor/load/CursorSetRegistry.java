package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Merges cursor sets coming from every source into the final, name-keyed list that the mod uses.
 * <p>
 * Precedence when two sets share a name: config beats resource pack beats built-in preset. The
 * losing set is logged, never silently dropped.
 * <p>
 * Deliberately Minecraft-free so the precedence rules can be unit tested.
 */
public final class CursorSetRegistry {

    private CursorSetRegistry() {
        throw new UnsupportedOperationException("CursorSetRegistry cannot be instantiated");
    }

    /**
     * @param sets every set that was found, in any order
     * @return the winning sets, sorted by name
     */
    public static List<CursorSet> merge(List<CursorSet> sets) {
        Map<String, CursorSet> winners = new LinkedHashMap<>();
        for (CursorSet candidate : sets) {
            CursorSet current = winners.get(candidate.id());
            if (current == null) {
                winners.put(candidate.id(), candidate);
                continue;
            }
            if (candidate.origin() == current.origin()) {
                // Same layer twice: NeoForge exposes the mod's own resources through the resource
                // manager while we also read them from the classpath. The resource manager copy
                // wins, so a resource pack overriding our file keeps working on every loader.
                if (candidate.source().equals(current.source())) {
                    continue;
                }
                Constants.LOG.debug("Cursor set '{}' from {} takes over the other {} copy",
                        candidate.id(), candidate.source(), current.origin().label());
                winners.put(candidate.id(), candidate);
                continue;
            }
            if (candidate.origin().beats(current.origin())) {
                Constants.LOG.info("Cursor set '{}' from {} overrides the one from {}",
                        candidate.id(), candidate.origin().label(), current.origin().label());
                winners.put(candidate.id(), candidate);
            } else {
                Constants.LOG.info("Cursor set '{}' from {} is overridden by {}",
                        candidate.id(), candidate.origin().label(), current.origin().label());
            }
        }

        List<CursorSet> result = new ArrayList<>(winners.values());
        result.sort(Comparator.comparing(CursorSet::id));
        return List.copyOf(result);
    }

    /**
     * @param sets the merged sets
     * @param id   the requested set id, may be {@code null}
     * @return the requested set, the first available set, or empty when nothing was loaded
     */
    public static Optional<CursorSet> find(List<CursorSet> sets, String id) {
        if (sets.isEmpty()) {
            return Optional.empty();
        }
        if (id != null) {
            for (CursorSet set : sets) {
                if (set.id().equals(id)) {
                    return Optional.of(set);
                }
            }
        }
        return Optional.of(sets.get(0));
    }
}
