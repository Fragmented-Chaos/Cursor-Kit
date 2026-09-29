package com.fragmentedchaos.cursorkit.cursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The picker's "Default" row hands the cursor back to the system: nothing is drawn even though sets
 * are available, and that choice is remembered like any other.
 */
class CursorManagerSystemSelectionTest {

    private static final String SET_JSON = """
            { "name": "Some Set",
              "states": { "default": { "texture": "arrow.png" } } }
            """;

    @Test
    void systemSelectionDrawsNothingEvenWhenSetsExist(@TempDir Path configDirectory)
            throws IOException {
        CursorManager manager = CursorManager.get();
        Files.writeString(configDirectory.resolve("some.json"), SET_JSON);
        manager.rescan(configDirectory);
        assertEquals(1, manager.sets().size());

        manager.selectSystem();

        assertTrue(manager.isSystemSelected());
        assertEquals(CursorManager.SYSTEM_SELECTION, manager.config().selectedSet());
        assertTrue(manager.findSelected().isEmpty());
        assertTrue(manager.selectedId().isEmpty());
    }

    @Test
    void aStoredSetIsNotTheSystemSelection(@TempDir Path configDirectory) throws IOException {
        CursorManager manager = CursorManager.get();
        Files.writeString(configDirectory.resolve("some.json"), SET_JSON);
        manager.rescan(configDirectory);

        assertTrue(manager.select("some"));

        assertFalse(manager.isSystemSelected());
        assertEquals("some", manager.findSelected().orElseThrow().id());
    }

    @Test
    void selectingAnEmptyIdMeansTheSystemCursor(@TempDir Path configDirectory) throws IOException {
        CursorManager manager = CursorManager.get();
        Files.writeString(configDirectory.resolve("some.json"), SET_JSON);
        manager.rescan(configDirectory);

        assertTrue(manager.select("some"));
        assertTrue(manager.select(""));

        assertTrue(manager.isSystemSelected());
        assertTrue(manager.findSelected().isEmpty());
    }
}
