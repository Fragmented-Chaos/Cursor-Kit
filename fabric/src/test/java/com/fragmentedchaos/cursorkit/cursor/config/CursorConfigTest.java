package com.fragmentedchaos.cursorkit.cursor.config;

import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorConfigTest {

    @Test
    void defaultsAreSane() {
        CursorConfig config = new CursorConfig();
        assertTrue(config.enabled());
        assertEquals("", config.selectedSet());
        assertEquals(1, config.scale());
        assertTrue(config.animate());
        assertEquals(2, config.edgeMargin());
    }

    @Test
    void roundTripsThroughJson(@TempDir Path dir) {
        Path file = dir.resolve("cursorkit.json");
        CursorConfig config = new CursorConfig();
        config.setEnabled(false);
        config.setSelectedSet("demo");
        config.setScale(3);
        config.setAnimate(false);
        config.setEdgeMargin(6);
        config.save(file);

        assertTrue(Files.isRegularFile(file));
        CursorConfig loaded = CursorConfig.load(file);
        assertFalse(loaded.enabled());
        assertEquals("demo", loaded.selectedSet());
        assertEquals(3, loaded.scale());
        assertFalse(loaded.animate());
        assertEquals(6, loaded.edgeMargin());
    }

    @Test
    void missingFileIsCreatedWithDefaults(@TempDir Path dir) {
        Path file = dir.resolve("nested").resolve("cursorkit.json");
        CursorConfig loaded = CursorConfig.load(file);

        assertTrue(Files.isRegularFile(file), "load() should create a default config");
        assertTrue(loaded.enabled());
        assertEquals(1, loaded.scale());
    }

    @Test
    void brokenFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cursorkit.json");
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        CursorConfig loaded = CursorConfig.load(file);
        assertTrue(loaded.enabled());
        assertEquals(1, loaded.scale());
    }

    @Test
    void unknownFieldsAreIgnored() {
        CursorConfig loaded = CursorConfig.fromJson(JsonParser.parseString("""
                { "enabled": false, "something_else": 42 }
                """).getAsJsonObject());
        assertFalse(loaded.enabled());
        assertEquals("", loaded.selectedSet());
    }

    @Test
    void valuesAreClampedToSaneRanges() {
        CursorConfig config = new CursorConfig();
        config.setScale(0);
        assertEquals(1, config.scale());
        config.setScale(-5);
        assertEquals(1, config.scale());
        config.setEdgeMargin(-3);
        assertEquals(0, config.edgeMargin());
        config.setSelectedSet(null);
        assertEquals("", config.selectedSet());
    }

    @Test
    void copyIsIndependent() {
        CursorConfig config = new CursorConfig();
        config.setSelectedSet("demo");
        CursorConfig copy = config.copy();
        copy.setSelectedSet("crosshair");
        assertEquals("demo", config.selectedSet());
        assertEquals("crosshair", copy.selectedSet());
    }

    @Test
    void savingNullPathIsHarmless() {
        new CursorConfig().save(null);
        assertEquals(1, CursorConfig.load(null).scale());
    }

    @Test
    void animationIsWrittenUnderItsOwnKey(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cursorkit.json");
        CursorConfig config = new CursorConfig();
        config.setAnimate(false);
        config.save(file);

        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("\"animate\""),
                "the setting covers every animated state, not just the busy one");
    }

    @Test
    void theOldBusyKeyIsStillRead(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cursorkit.json");
        Files.writeString(file, "{\"animate_busy\": false}", StandardCharsets.UTF_8);

        assertFalse(CursorConfig.load(file).animate(),
                "configs written before the switch covered all states must keep working");
    }
}
