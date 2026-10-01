package com.fragmentedchaos.cursorkit.client.sodium;

import com.fragmentedchaos.cursorkit.client.CursorEntryPoint;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.gui.CursorKitScreen;
import com.fragmentedchaos.cursorkit.client.render.CursorIcon;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;


/**
 * Puts the cursor entry into Sodium's video settings.
 * <p>
 * Sodium replaces the vanilla video settings screen with its own, so the option row added by
 * {@code VideoSettingsScreenMixin} never appears while it is installed, and the floating button
 * {@code ScreenEntryMixin} falls back to looks out of place next to Sodium's option list. For that
 * case Sodium publishes a configuration API: this class is called by Sodium as its
 * {@code sodium:config_api_user} entry point and adds a page of its own. The page holds a single
 * is registered as an external page: Sodium then draws an arrow in its page list, which matches
 * the icon the other entry points show, and clicking the page opens the picker straight away.
 * <p>
 * Sodium is deliberately not a dependency of this mod, in the build or at runtime. The interfaces
 * implemented here are the compile-only stubs in {@code common/src/sodiumApi}, which never enter a
 * jar; at runtime Sodium supplies the real ones. Nothing outside this class references them, so a game
 * without Sodium never loads this class - and {@link CursorEntryPoint#isSodiumLinked()} tells the mixin
 * that this page took the entry over.
 * <p>
 * The one thing to keep in mind when touching this file: the stubs mirror Sodium's signatures, they do
 * not check them. If Sodium ever changes this API, the mismatch surfaces here.
 */
public final class CursorSodiumConfig implements ConfigEntryPoint {

    /** Where the icon lives inside this mod's own jar. */
    private static final String ICON_PATH = "/assets/cursorkit/textures/gui/icon.png";

    /**
     * The mod's accent colour in Sodium's list. Without it Sodium picks a random theme per install,
     * which would leave the icon and the {@code 光标} entry in a colour that has nothing to do with
     * the gold this mod uses in its own screens.
     */
    private static final int ACCENT_RGB = 0xE0A33C;

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        CursorIcon.ensureRegistered();
        // An option page holding one button rather than an external page: Sodium prefixes external
        // pages with an arrow in the page list, and a page of our own is the only way to avoid it.
        builder.registerOwnModOptions()
                .setIcon(CursorIcon.ID)
                .setColorTheme(builder.createColorTheme().setBaseThemeRGB(ACCENT_RGB))
                // An external page opens the picker in one click, and Sodium marks it with the arrow
                // in its page list - the same marker the vanilla entry row draws as its icon.
                .addPage(builder.createExternalPage()
                        .setName(CursorTranslations.get("cursorkit.entry.title", "Cursor"))
                        .setScreenConsumer(parent -> Minecraft.getInstance()
                                .setScreenAndShow(new CursorKitScreen(parent))));
        CursorEntryPoint.markSodiumLinked();
        Constants.LOG.info("Sodium video settings: added the cursor page");
    }
}
