package com.fragmentedchaos.cursorkit.cursor;

import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live reloading: {@code rescan} re-reads only the config directory and tells the caller whether
 * anything actually changed.
 * <p>
 * The manager is a singleton, so every test first syncs it with its own directory and only measures
 * what happens afterwards.
 */
class CursorManagerRescanTest {

    /**
     * The sets a test cares about.
     * <p>
     * The hand-made row is always offered - even before any path is usable, so the picker can open its
     * editor - which means it shows up in every list here and would throw the counts off.
     */
    private static java.util.List<com.fragmentedchaos.cursorkit.cursor.model.CursorSet> loaded(
            CursorManager manager) {
        return manager.sets().stream()
                .filter(set -> !com.fragmentedchaos.cursorkit.cursor.load.CursorSets.CUSTOM_ID.equals(set.id()))
                .toList();
    }


    private static final String SET_JSON = """
            { "name": "Watched",
              "states": { "default": { "texture": "arrow.png" } } }
            """;

    @Test
    void rescanPicksUpSetsThatAppearWhileRunning(@TempDir Path configDirectory) throws IOException {
        CursorManager manager = CursorManager.get();
        manager.rescan(configDirectory);
        assertTrue(loaded(manager).isEmpty());
        int generation = manager.generation();

        Files.writeString(configDirectory.resolve("watched.json"), SET_JSON);

        assertTrue(manager.rescan(configDirectory), "the new file has to be reported as a change");
        assertEquals(1, loaded(manager).size());
        assertEquals("watched", loaded(manager).get(0).id());
        assertEquals(generation + 1, manager.generation());

        assertFalse(manager.rescan(configDirectory), "nothing changed since the last scan");
        assertEquals(generation + 1, manager.generation());
    }

    @Test
    void rescanPicksUpCursorPacks(@TempDir Path configDirectory) throws IOException {
        CursorManager manager = CursorManager.get();
        Files.createDirectories(configDirectory.resolve("packs/mine/assets/testpack/cursor"));
        Files.writeString(configDirectory.resolve("packs/mine/assets/testpack/cursor/packed.json"),
                SET_JSON);

        manager.rescan(configDirectory);

        assertEquals(1, loaded(manager).size());
        assertEquals("packed", loaded(manager).get(0).id());
        assertEquals(CursorSetOrigin.CONFIG_PACK, loaded(manager).get(0).origin());
    }

    @Test
    void rescanNeverRewritesTheStoredSelection(@TempDir Path configDirectory) throws IOException {
        CursorManager manager = CursorManager.get();
        Files.writeString(configDirectory.resolve("watched.json"), SET_JSON);
        Files.writeString(configDirectory.resolve("other.json"), SET_JSON);
        manager.rescan(configDirectory);

        assertTrue(manager.select("watched"));
        assertEquals("watched", manager.config().selectedSet());

        // Deleting the selected set must not push the player onto another one behind their back:
        // the cursor falls back at runtime, but the stored choice only changes on a real reload.
        Files.delete(configDirectory.resolve("watched.json"));
        assertTrue(manager.rescan(configDirectory));

        assertEquals(1, loaded(manager).size());
        assertEquals("other", loaded(manager).get(0).id());
        assertEquals("watched", manager.config().selectedSet());
    }
}
