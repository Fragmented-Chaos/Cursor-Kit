package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.client.render.CursorAssetSource;
import com.fragmentedchaos.cursorkit.client.render.CursorTextures;
import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSetLoader;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSetRegistry;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSets;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand-made cursor set: one image path per state, filled in by the player in the picker.
 */
class CursorSetsTest {

    @Test
    void buildsASetFromTheFilesThatExist(@TempDir Path root) throws IOException {
        Path arrow = root.resolve("arrow.png");
        Files.write(arrow, new byte[] {1});
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", arrow.toString());
        config.setCustomState("clickable", root.resolve("missing.png").toString());
        config.setCustomState("text", "");

        CursorSet set = CursorSets.custom(config, root, "config").orElseThrow();

        assertEquals(CursorSets.CUSTOM_ID, set.id());
        assertEquals(CursorSetOrigin.CUSTOM_STATES, set.origin());
        assertEquals(1, set.images().size(), "only the state with a real file is drawn");
        assertEquals(arrow.toString(), set.image(CursorState.DEFAULT).orElseThrow().texture());
        assertEquals(set.image(CursorState.DEFAULT), set.image(CursorState.CLICKABLE),
                "a state without a file falls back to default, like in any other set");
    }

    @Test
    void relativePathsAreResolvedAgainstTheGameDirectory(@TempDir Path game) throws IOException {
        Path cursors = Files.createDirectories(game.resolve("my-cursors"));
        Files.write(cursors.resolve("hand.png"), new byte[] {2});
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", "my-cursors/hand.png");

        CursorSet set = CursorSets.custom(config, game, "config").orElseThrow();

        assertEquals(cursors.resolve("hand.png").toString(),
                set.image(CursorState.DEFAULT).orElseThrow().texture());
    }

    @Test
    void withoutUsablePathsThereIsNoSetAtAll(@TempDir Path root) {
        assertTrue(CursorSets.custom(new CursorConfig(), root, "config").isEmpty());

        CursorConfig config = new CursorConfig();
        config.setCustomState("default", "\0not a path");
        assertTrue(CursorSets.custom(config, null, "config").isEmpty(),
                "a path the platform cannot parse is reported as no path, not as a crash");
    }

    @Test
    void pathsAreStoredTrimmedAndOmittedWhenEmpty(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cursorkit.json");
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", "  /tmp/arrow.png  ");
        config.setCustomState("text", "   ");
        config.save(file);

        String written = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(written.contains("custom_states"), written);
        assertFalse(written.contains("\"text\""), "a cleared state should not be in the file");

        CursorConfig loaded = CursorConfig.load(file);
        assertEquals("/tmp/arrow.png", loaded.customState("default"));
        assertEquals("", loaded.customState("text"));

        // And the map view used by the picker agrees.
        assertEquals(Map.of("default", "/tmp/arrow.png"), loaded.customStates());
    }

    @Test
    void copiesAreIndependent(@TempDir Path root) {
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", "/tmp/a.png");
        CursorConfig copy = config.copy();
        copy.setCustomState("default", "/tmp/b.png");

        assertEquals("/tmp/a.png", config.customState("default"));
        assertEquals("/tmp/b.png", copy.customState("default"));
    }

    @Test
    void theHandMadeSetKeepsItsOwnTextureIds(@TempDir Path root) throws IOException {
        Path arrow = Files.write(root.resolve("arrow.png"), new byte[] {3});
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", arrow.toString());
        CursorSet set = CursorSets.custom(config, root, "config").orElseThrow();
        CursorImage image = set.image(CursorState.DEFAULT).orElseThrow();

        String id = com.fragmentedchaos.cursorkit.client.render.CursorTextures.resolve(set, image).toString();

        assertTrue(id.startsWith("cursorkit:custom/"), id);
        assertNotNull(com.fragmentedchaos.cursorkit.client.render.CursorAssetSource
                .forImage(set, image, null), "the file is read straight from disk");
    }

    @Test
    void aConfigSetOfTheSameNameStillWins(@TempDir Path root) throws IOException {
        Files.write(root.resolve("arrow.png"), new byte[] {4});
        Files.writeString(root.resolve("custom.json"), """
                {
                  "states": { "default": { "texture": "arrow.png" } }
                }
                """, StandardCharsets.UTF_8);
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", root.resolve("arrow.png").toString());
        CursorSet handMade = CursorSets.custom(config, root, "config").orElseThrow();
        CursorSet fromFile = CursorSetLoader.loadFromConfigDirectory(root).get(0);

        List<CursorSet> merged = CursorSetRegistry.merge(List.of(handMade, fromFile));

        // The hand-made set has a reserved id, so a file named "custom.json" is a set of its own.
        assertEquals(2, merged.size(), "different ids, so both survive");
        assertTrue(merged.stream().anyMatch(set -> set.origin() == CursorSetOrigin.CUSTOM_STATES));
        assertTrue(merged.stream().anyMatch(set -> set.origin() == CursorSetOrigin.CONFIG));
    }

    @Test
    void aHandMadeSetCanPointAtItsOwnEffectAnimation(@TempDir Path game) throws IOException {
        Path strip = Files.write(game.resolve("click.png"), new byte[] {9});
        CursorConfig config = new CursorConfig();
        Files.write(game.resolve("arrow.png"), new byte[] {1});
        config.setCustomState("default", "arrow.png");
        config.setCustomEffect(new ClickEffect("image", 0xFFFFFF, 15.0F, 400L, 6,
                "click.png", 8, 50, 48));

        CursorSet set = CursorSets.custom(config, game, "config").orElseThrow();

        assertEquals(strip.toString(), set.clickEffect().texture(),
                "the animation path is resolved like the per-state ones");
        assertTrue(set.clickEffect().isImage());
    }

    @Test
    void aMissingAnimationFallsBackToTheDefaultEffect(@TempDir Path game) throws IOException {
        Files.write(game.resolve("arrow.png"), new byte[] {1});
        CursorConfig config = new CursorConfig();
        config.setCustomState("default", "arrow.png");
        config.setCustomEffect(new ClickEffect("image", 0xFFFFFF, 15.0F, 400L, 6,
                "nope.png", 8, 50, 48));

        CursorSet set = CursorSets.custom(config, game, "config").orElseThrow();

        assertEquals(ClickEffect.DEFAULT, set.clickEffect(),
                "better the mod's own effect than nothing at all");
    }
}
