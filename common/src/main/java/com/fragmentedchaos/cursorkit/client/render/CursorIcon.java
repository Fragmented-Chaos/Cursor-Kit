package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
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
 * The mod's square arrow icon, uploaded so the GUI can draw it: Sodium's page list uses it, and so
 * does the vanilla video settings entry row.
 * <p>
 * It cannot be left to the resource manager. Without Fabric API a mod's own assets never enter it, so
 * the file is read straight from this mod's jar - the same way {@code CursorTranslations} gets its
 * language files. Sodium reads the texture from the texture manager without a null check, so whoever
 * draws it must call {@link #ensureRegistered()} first.
 */
public final class CursorIcon {

    /** Identifier of the icon, which is also its path inside this mod's jar. */
    public static final Identifier ID = Identifier.parse("cursorkit:textures/gui/icon.png");

    /** Side of the icon as it is drawn, in GUI units. */
    public static final int SIZE = 16;

    /** Edge of the source image, used to scale it down to {@link #SIZE}. */
    public static final int SOURCE_SIZE = 128;

    private static final String PATH = "/assets/cursorkit/textures/gui/icon.png";

    private static boolean registered;

    private CursorIcon() {
        throw new UnsupportedOperationException("CursorIcon cannot be instantiated");
    }

    /**
     * Draws the icon at {@code x}/{@code y}, uploading it first if needed.
     *
     * @param extractor the GUI extractor of the screen being drawn
     * @param x         left edge in GUI units
     * @param y         top edge in GUI units
     */
    public static void draw(GuiGraphicsExtractor extractor, int x, int y) {
        ensureRegistered();
        extractor.blit(RenderPipelines.GUI_TEXTURED, ID,
                x, y,
                0.0F, 0.0F,
                SIZE, SIZE,
                SOURCE_SIZE, SOURCE_SIZE,
                SOURCE_SIZE, SOURCE_SIZE);
    }

    /** Uploads the icon once; safe to call from every screen that wants to draw it. */
    public static void ensureRegistered() {
        if (registered) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        TextureManager textures = minecraft.getTextureManager();
        if (textures == null) {
            return;
        }
        textures.registerAndLoad(ID, new JarIcon());
        registered = true;
        Constants.LOG.debug("Uploaded the mod icon {}", ID);
    }

    /** The icon texture: read from this mod's jar, and re-read from there whenever packs reload. */
    private static final class JarIcon extends ReloadableTexture {

        private JarIcon() {
            super(ID);
        }

        @Override
        public TextureContents loadContents(ResourceManager manager) throws IOException {
            try (InputStream stream = CursorIcon.class.getResourceAsStream(PATH)) {
                if (stream == null) {
                    throw new IOException("Missing " + PATH);
                }
                // Same setup as Sodium's own icon: a monochrome square that is neither blurred nor
                // clamped, so it stays crisp when the list scales it down.
                return new TextureContents(NativeImage.read(stream), new TextureMetadataSection(
                        false, false, MipmapStrategy.AUTO, 0.1F));
            }
        }
    }
}
