package com.fragmentedchaos.cursorkit.client;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fabric and Quilt only load a mod's language files with Fabric API installed, so this mod reads its
 * own files instead. These tests cover that reading and the formatting around it.
 */
class CursorTranslationsTest {

    @Test
    void theBundledLanguageFilesAreReadable() {
        Map<String, String> english = CursorTranslations.load("en_us");
        Map<String, String> chinese = CursorTranslations.load("zh_cn");

        assertFalse(english.isEmpty(), "en_us has to be in the jar");
        assertFalse(chinese.isEmpty(), "zh_cn has to be in the jar");
        assertTrue(english.containsKey("cursorkit.screen.title"));
        assertTrue(chinese.containsKey("cursorkit.screen.title"));
    }

    @Test
    void bothLanguagesCoverTheSameKeys() {
        Set<String> english = CursorTranslations.load("en_us").keySet();
        Set<String> chinese = CursorTranslations.load("zh_cn").keySet();

        assertEquals(english, chinese, "a key missing in one file shows up as English text");
    }

    @Test
    void anUnknownLanguageLoadsNothing() {
        assertTrue(CursorTranslations.load("xx_yy").isEmpty());
    }

    @Test
    void placeholdersAreFilled() {
        assertEquals("Animate: ON", CursorTranslations.format("Animate: %s", new Object[] {"ON"}));
        assertEquals("1 个状态", CursorTranslations.format("%s 个状态", new Object[] {1}));
    }

    @Test
    void componentsAreResolvedBeforeFormatting() {
        Component on = Component.literal("ON");

        assertEquals("Animate: ON", CursorTranslations.format("Animate: %s", new Object[] {on}));
    }

    @Test
    void aBrokenPlaceholderDoesNotThrow() {
        assertEquals("100% done", CursorTranslations.format("100% done", new Object[0]));
    }
}
