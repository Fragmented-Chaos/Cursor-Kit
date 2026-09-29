package com.fragmentedchaos.cursorkit.cursor;

import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.config.CursorHotspots;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Click points are edited in their own screen and confirmed there, so the picker's "Cancel" has to
 * keep them while it takes back everything else it changed.
 */
class CursorHotspotsCancelTest {

    private static final String SET_JSON = """
            { "name": "Some Set",
              "states": { "default": { "texture": "arrow.png" } } }
            """;

    @Test
    void pickerCancelKeepsTheConfirmedClickPoints() {
        CursorConfig original = new CursorConfig();
        original.setSelectedSet("alpha");

        CursorConfig draft = original.copy();
        draft.setSelectedSet("beta");
        draft.setHotspot("beta", "default", 4, 5);

        CursorConfig restored = CursorHotspots.mergedInto(original, draft);

        assertEquals("alpha", restored.selectedSet(), "the picker's own change is taken back");
        assertEquals(4, restored.hotspot("beta", "default")[0], "the click point stays");
        assertEquals(5, restored.hotspot("beta", "default")[1]);
    }

    @Test
    void aResetInTheEditorAlsoSurvivesTheCancel() {
        CursorConfig original = new CursorConfig();
        original.setHotspot("alpha", "default", 1, 1);

        CursorConfig draft = original.copy();
        draft.clearHotspot("alpha", "default");

        CursorConfig restored = CursorHotspots.mergedInto(original, draft);

        assertNull(restored.hotspot("alpha", "default"),
                "a click point the player reset must not come back either");
    }

    @Test
    void mergedClickPointsDoNotAliasTheDraft() {
        CursorConfig original = new CursorConfig();
        CursorConfig draft = new CursorConfig();
        draft.setHotspot("alpha", "default", 2, 3);

        CursorConfig restored = CursorHotspots.mergedInto(original, draft);
        draft.setHotspot("alpha", "default", 9, 9);

        assertEquals(2, restored.hotspot("alpha", "default")[0]);
    }

    @Test
    void theEditorSnapshotOnlyUndoesItsOwnEdits(@TempDir Path configDirectory) throws IOException {
        CursorManager manager = CursorManager.get();
        Files.writeString(configDirectory.resolve("some.json"), SET_JSON);
        manager.rescan(configDirectory);

        // What the picker hands over: a draft that already has the player's click point in it.
        CursorConfig draft = new CursorConfig();
        draft.setHotspot("some", "default", 1, 2);
        CursorConfig editorSnapshot = draft.copy();

        draft.setHotspot("some", "default", 7, 8);

        CursorConfig restored = CursorHotspots.mergedInto(draft, editorSnapshot);
        assertEquals(1, restored.hotspot("some", "default")[0],
                "cancelling the editor goes back to how it was opened, not to an empty config");
    }
}
