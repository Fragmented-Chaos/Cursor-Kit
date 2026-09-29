package com.fragmentedchaos.cursorkit.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The button belongs in the video settings, including the replacements Sodium and Embeddium bring.
 */
class CursorEntryPointTest {

    @Test
    void theVanillaVideoSettingsScreenGetsTheButton() {
        assertTrue(CursorEntryPoint.isVideoSettingsScreen(
                "net.minecraft.client.gui.screens.options.VideoSettingsScreen"));
    }

    @Test
    void theScreensOtherModsShipGetItToo() {
        assertTrue(CursorEntryPoint.isVideoSettingsScreen(
                "net.caffeinemc.mods.sodium.client.gui.SodiumVideoOptionsScreen"));
        assertTrue(CursorEntryPoint.isVideoSettingsScreen(
                "me.jellysquid.mods.sodium.client.gui.SodiumVideoOptionsScreen"));
        assertTrue(CursorEntryPoint.isVideoSettingsScreen(
                "org.embeddedt.embeddium.client.gui.options.EmbeddiumOptionsScreen"));
    }

    @Test
    void otherScreensDoNot() {
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(
                "net.minecraft.client.gui.screens.OptionsScreen"));
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(
                "net.minecraft.client.gui.screens.TitleScreen"));
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(
                "net.minecraft.client.gui.screens.controls.ControlsScreen"));
    }

    @Test
    void theCursorPickerItselfNeverGetsOne() {
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(
                "com.fragmentedchaos.cursorkit.client.gui.CursorKitScreen"));
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(
                "com.fragmentedchaos.cursorkit.client.gui.CursorHotspotScreen"));
    }

    @Test
    void nonsenseIsRejected() {
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(null));
        assertFalse(CursorEntryPoint.isVideoSettingsScreen(""));
        assertFalse(CursorEntryPoint.isVideoSettingsScreen("   "));
    }

    @Test
    void sodiumScreensAreRecognisedSoTheFloatingButtonCanStepAside() {
        // Sodium lists the entry as a page of its own, so its screen must not get the button as well.
        assertTrue(CursorEntryPoint.isSodiumScreen(
                "net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen"));
        assertTrue(CursorEntryPoint.isSodiumScreen(
                "net.caffeinemc.mods.sodium.client.gui.widgets.PageListWidget"));
    }

    @Test
    void otherScreensAreNotMistakenForSodium() {
        assertFalse(CursorEntryPoint.isSodiumScreen(
                "net.minecraft.client.gui.screens.options.VideoSettingsScreen"));
        // Sodium before the config API: there is no page, the floating button has to stay.
        assertFalse(CursorEntryPoint.isSodiumScreen(
                "me.jellysquid.mods.sodium.client.gui.SodiumVideoOptionsScreen"));
        assertFalse(CursorEntryPoint.isSodiumScreen(
                "com.fragmentedchaos.cursorkit.client.gui.CursorKitScreen"));
        assertFalse(CursorEntryPoint.isSodiumScreen(null));
        assertFalse(CursorEntryPoint.isSodiumScreen(""));
    }

    @Test
    void sodiumTakingTheEntryIsRemembered() {
        assertFalse(CursorEntryPoint.isSodiumLinked());
        CursorEntryPoint.markSodiumLinked();
        assertTrue(CursorEntryPoint.isSodiumLinked());
    }
}
