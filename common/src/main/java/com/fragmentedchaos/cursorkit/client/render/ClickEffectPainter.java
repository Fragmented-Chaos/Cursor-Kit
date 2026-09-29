package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Draws one moment of a click effect.
 * <p>
 * Shared by the live cursor and by the picker's preview of it, so what a set shows in the list is
 * exactly what happens when the player clicks. A built-in effect is a handful of small squares - the
 * pack's own animation is a texture the set already registered.
 */
public final class ClickEffectPainter {

    /** How many squares the ring is made of. */
    private static final int RING_SEGMENTS = 24;

    private ClickEffectPainter() {
        throw new UnsupportedOperationException("ClickEffectPainter cannot be instantiated");
    }

    /**
     * @param extractor the frame's extractor
     * @param set       the set the effect belongs to, needed for the pack's own animation
     * @param spec      the effect to draw
     * @param centreX   where the effect is centred, in GUI units
     * @param centreY   where the effect is centred, in GUI units
     * @param progress  0 at the start of the effect, 1 when it is over
     * @param guiScale  the window's GUI scale, for the texture route
     */
    public static void paint(GuiGraphicsExtractor extractor, @Nullable CursorSet set,
                             ClickEffect spec, double centreX, double centreY, float progress,
                             int guiScale) {
        if (spec == null || spec.disabled() || progress < 0.0F || progress >= 1.0F) {
            return;
        }
        if (spec.isImage()) {
            if (set != null) {
                paintImage(extractor, set, spec, centreX, centreY, progress, guiScale);
            }
            return;
        }

        int alpha = CursorClickAnimation.alpha(spec, progress);
        if (alpha <= 8) {
            return;
        }
        int centreXInt = (int) Math.round(centreX);
        int centreYInt = (int) Math.round(centreY);
        int radius = Math.max(1, Math.round(CursorClickAnimation.radius(spec, progress)));
        int rgb = spec.color() & 0xFFFFFF;
        int colour = (alpha << 24) | rgb;

        if ("pulse".equals(spec.type())) {
            // A filled dot that grows and fades: the quietest of the three.
            int half = Math.max(1, radius / 2);
            extractor.fill(centreXInt - half, centreYInt - half, centreXInt + half, centreYInt + half,
                    ((alpha / 2) << 24) | rgb);
        } else if (!"burst".equals(spec.type())) {
            int thickness = progress < 0.45F ? 2 : 1;
            for (int step = 0; step < RING_SEGMENTS; step++) {
                double angle = step * 2.0 * Math.PI / RING_SEGMENTS;
                int x = centreXInt + (int) Math.round(Math.cos(angle) * radius);
                int y = centreYInt + (int) Math.round(Math.sin(angle) * radius);
                extractor.fill(x, y, x + thickness, y + thickness, colour);
            }
        }

        int droplets = "pulse".equals(spec.type()) ? 0 : spec.particles();
        if (droplets > 0 && progress < 0.7F) {
            int drop = (((int) (alpha * 0.8F)) << 24) | rgb;
            float dropRadius = "burst".equals(spec.type()) ? radius * 1.1F : radius * 1.35F;
            for (int step = 0; step < droplets; step++) {
                double angle = step * 2.0 * Math.PI / droplets + Math.PI / droplets;
                int x = centreXInt + (int) Math.round(Math.cos(angle) * dropRadius);
                int y = centreYInt + (int) Math.round(Math.sin(angle) * dropRadius);
                extractor.fill(x, y, x + 1, y + 1, drop);
            }
        }
    }

    /** Plays the frame of the pack's own animation that belongs to this moment. */
    private static void paintImage(GuiGraphicsExtractor extractor, CursorSet set, ClickEffect spec,
                                   double centreX, double centreY, float progress, int guiScale) {
        CursorImage image = spec.asImage();
        if (image == null) {
            return;
        }
        Identifier texture = CursorTextures.resolve(set, image);
        if (texture == null) {
            return;
        }
        CursorGeometry geometry = CursorGeometry.of(texture, image);
        int frames = Math.max(1, geometry.frames());
        int frame = Math.min((int) (progress * frames), frames - 1);
        int frameSize = geometry.frameSize();
        int multiple = Math.max(1, spec.size() / CursorImage.FRAME_SIZE);
        int size = CursorRenderer.scaledSize(frameSize, multiple, guiScale);
        int x = (int) Math.round(centreX - size / 2.0D);
        int y = (int) Math.round(centreY - size / 2.0D);
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y,
                (float) (frame * frameSize), 0.0F,
                size, size,
                frameSize, frameSize,
                frameSize * frames, frameSize);
    }
}
