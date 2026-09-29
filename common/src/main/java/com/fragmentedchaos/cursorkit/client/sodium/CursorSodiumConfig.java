package com.fragmentedchaos.cursorkit.client.sodium;

import com.fragmentedchaos.cursorkit.client.CursorEntryPoint;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.gui.CursorKitScreen;
import com.mojang.blaze3d.platform.NativeImage;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * Puts the cursor entry into Sodium's video settings.
 * <p>
 * Sodium replaces the vanilla video settings screen with its own, so the option row added by
 * {@code VideoSettingsScreenMixin} never appears while it is installed, and the floating button
 * {@code ScreenEntryMixin} falls back to looks out of place next to Sodium's option list. For that
 * case Sodium publishes a configuration API: this class is called by Sodium as its
 * {@code sodium:config_api_user} entry point and adds a page of its own. The page holds a single
 * button rather than being an external page, because Sodium marks external pages with an arrow in
 * the page list and a normal option page has no such marker.
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

    /** The mod's square icon in Sodium's list, uploaded by {@link #registerIcon}. */
    private static final Identifier ICON = Identifier.parse("cursorkit:textures/gui/icon.png");

    /** ID of the button row; Sodium wants one, the picker does not use it for anything else. */
    private static final Identifier ENTRY = Identifier.parse("cursorkit:cursor_picker");

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
        registerIcon(Minecraft.getInstance().getTextureManager());
        // An option page holding one button rather than an external page: Sodium prefixes external
        // pages with an arrow in the page list, and a page of our own is the only way to avoid it.
        builder.registerOwnModOptions()
                .setIcon(ICON)
                .setColorTheme(builder.createColorTheme().setBaseThemeRGB(ACCENT_RGB))
                .addPage(builder.createOptionPage()
                        .setName(CursorTranslations.get("cursorkit.entry.title", "Cursor"))
                        .addOption(builder.createExternalButtonOption(ENTRY)
                                .setName(CursorTranslations.get("cursorkit.entry.button", "Open the cursor picker"))
                                .setTooltip(CursorTranslations.get("cursorkit.entry.tooltip",
                                        "Choose the cursor set, its scale and its animation"))
                                .setScreenConsumer(parent -> Minecraft.getInstance()
                                        .setScreenAndShow(new CursorKitScreen(parent)))));
        CursorEntryPoint.markSodiumLinked();
        Constants.LOG.info("Sodium video settings: added the cursor page");
    }

    /**
     * Uploads the mod icon under {@link #ICON} before Sodium draws its list.
     * <p>
     * The icon cannot be left to the resource manager. Without Fabric API a mod's assets never enter
     * it, so the header Sodium draws would be a missing texture - and Sodium reads the texture straight
     * from the texture manager without a null check. Reading the file from this mod's own jar works on
     * every loader, the same way {@link CursorTranslations} gets its language files.
     */
    private static void registerIcon(TextureManager textures) {
        textures.registerAndLoad(ICON, new JarIcon());
    }

    /** The icon texture: read from this mod's jar, and re-read from there whenever packs reload. */
    private static final class JarIcon extends ReloadableTexture {

        private JarIcon() {
            super(ICON);
        }

        @Override
        public TextureContents loadContents(ResourceManager manager) throws IOException {
            try (InputStream stream = CursorSodiumConfig.class.getResourceAsStream(ICON_PATH)) {
                if (stream == null) {
                    throw new IOException("Missing " + ICON_PATH);
                }
                // Same setup as Sodium's own icon: a monochrome square that is neither blurred nor
                // clamped, so it stays crisp when the list scales it down.
                return new TextureContents(NativeImage.read(stream), new TextureMetadataSection(
                        false, false, MipmapStrategy.AUTO, 0.1F));
            }
        }
    }
}
