package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.client.CursorStateTracker;
import com.fragmentedchaos.cursorkit.client.SystemCursorScreen;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the selected cursor set's image for the current state and manages the system cursor.
 * <p>
 * Called at the very end of {@code Gui.extractRenderState}, right after vanilla applied its own
 * cursor, so our icon ends up on top of every screen, overlay, toast and debug overlay.
 */
public final class CursorRenderer {

    private CursorRenderer() {
        throw new UnsupportedOperationException("CursorRenderer cannot be instantiated");
    }

    /**
     * Hides the system cursor exactly while this mod draws its own, and restores it in every other
     * case. Called once per frame even when no screen is open, so the pointer can never stay
     * invisible.
     *
     * @return true when the custom cursor should be drawn this frame
     */
    public static boolean updateVisibility(@Nullable Window window) {
        CursorStateTracker.Resolved resolved = CursorStateTracker.resolve();
        MouseHandler mouse = CursorStateTracker.mouse();

        if (resolved.screen() == null || mouse == null || window == null) {
            // No screen open (or the game is still starting up): never keep the cursor hidden.
            CursorVisibility.setHidden(false);
            return false;
        }

        CursorConfig config = CursorManager.get().config();
        CursorSet selected = CursorManager.get().findSelected().orElse(null);
        if (!config.enabled() || selected == null) {
            // Switched off, or "Default" is selected: nothing of ours is drawn.
            CursorVisibility.setHidden(false);
            return false;
        }

        // The hand-made set is offered even before a path is usable, and a pack may ship no image at
        // all. Asking first keeps the system cursor in that case instead of hiding it and then
        // drawing nothing, which would leave the player with no pointer at all.
        boolean drawable = selected.image(drawnState(resolved.state(), config.animate())).isPresent();

        double mouseX = mouse.getScaledXPos(window);
        double mouseY = mouse.getScaledYPos(window);
        boolean takeOver = CursorVisibilityPolicy.shouldTakeOver(window.isFocused(),
                mouse.isMouseGrabbed(), mouseX, mouseY,
                window.getGuiScaledWidth(), window.getGuiScaledHeight(), config.edgeMargin(),
                resolved.screen() instanceof SystemCursorScreen, drawable);
        CursorVisibility.setHidden(takeOver);
        return takeOver;
    }

    /** @param extractor the frame's extractor, {@code this} of the injection point */
    public static void render(GuiGraphicsExtractor extractor, Window window) {
        CursorStateTracker.Resolved resolved = CursorStateTracker.resolve();
        MouseHandler mouse = CursorStateTracker.mouse();
        CursorConfig config = CursorManager.get().config();

        boolean takeOver = updateVisibility(window);
        if (mouse == null || window == null) {
            return;
        }

        CursorSet set = CursorManager.get().findSelected().orElse(null);

        double mouseX = mouse.getScaledXPos(window);
        double mouseY = mouse.getScaledYPos(window);
        long nowMs = System.nanoTime() / 1_000_000L;
        if (config.clickEffect()) {
            // Also while the system cursor is in charge: the click feedback is a mod feature of its
            // own, and a player who keeps Minecraft's cursor still wants to see their clicks land.
            ClickEffect effect = set == null ? ClickEffect.DEFAULT : set.clickEffect();
            drawClickRipples(extractor, mouseX, mouseY, nowMs, set, effect,
                    Math.max(1, window.getGuiScale()));
        }
        if (!takeOver || set == null) {
            return;
        }

        CursorImage image = set.image(drawnState(resolved.state(), config.animate())).orElse(null);
        if (image == null) {
            return;
        }
        Identifier texture = CursorTextures.resolve(set, image);
        if (texture == null) {
            return;
        }

        // Drawn at an exact multiple of the image's own pixels, like the system cursor: 16 GUI
        // units' worth of pixels at 1x, rounded to the nearest whole multiple of the frame. That
        // keeps every pixel square instead of resampling the image.
        CursorGeometry geometry = CursorGeometry.of(texture, image);
        int frameSize = geometry.frameSize();
        int guiScale = Math.max(1, window.getGuiScale());
        int size = scaledSize(frameSize, set.scale() * config.scale(), guiScale);
        float guiPerPixel = (float) size / frameSize;
        int x = (int) Math.round(mouseX - geometry.hotspotX(image) * (double) guiPerPixel);
        int y = (int) Math.round(mouseY - geometry.hotspotY(image) * (double) guiPerPixel);

        int frame = Math.min(frameFor(image, config.animate(), nowMs), geometry.frames() - 1);
        int textureWidth = frameSize * geometry.frames();

        // blit(pipeline, texture, x, y, u, v, width, height, sourceWidth, sourceHeight,
        //      textureWidth, textureHeight): u/v are in texture pixels, which lets us pick one frame
        // out of the horizontal strip and scale it up in one call.
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y,
                (float) (frame * frameSize), 0.0F,
                size, size,
                frameSize, frameSize,
                textureWidth, frameSize);
    }

    /**
     * Picks the icon's side length in GUI units.
     * <p>
     * A cursor keeps the size a cursor normally has; the image resolution only decides how sharp it
     * is. Around the requested size (16 GUI units times the multiplier) the closest size whose
     * physical pixels are a whole multiple - or a whole fraction - of the frame is chosen, so the
     * image is never resampled by a fractional ratio: a 32x32 cursor on a GUI scale of 2 lands on
     * exactly 32 physical pixels, one image pixel per screen pixel, at the usual size.
     *
     * @param frameSize image pixels of one frame
     * @param wanted    size multiplier the player picked (1x, 2x, 3x)
     * @param guiScale  the window's GUI scale
     * @return side length in GUI units
     */
    static int scaledSize(int frameSize, int wanted, int guiScale) {
        int intended = Math.max(1, CursorImage.FRAME_SIZE * Math.max(1, wanted));
        int best = intended;
        int bestError = Integer.MAX_VALUE;
        for (int size = Math.max(1, intended - 6); size <= intended + 6; size++) {
            int physical = size * guiScale;
            boolean exact = physical % frameSize == 0 || frameSize % physical == 0;
            int error = Math.abs(size - intended);
            if (exact && error < bestError) {
                best = size;
                bestError = error;
            }
        }
        return best;
    }

    /**
     * Draws the click feedback: a ring expanding from the point that was clicked, with a few
     * droplets just ahead of it.
     * <p>
     * Drawn before the cursor so the cursor keeps its place on top, and only through
     * {@code fill} - a ripple is a handful of small squares, which is cheaper than any texture and
     * needs no art from the cursor set.
     *
     * @param mouseX cursor x in GUI units
     * @param mouseY cursor y in GUI units
     * @param nowMs  wall-clock time in milliseconds
     */
    private static void drawClickRipples(GuiGraphicsExtractor extractor, double mouseX, double mouseY,
                                         long nowMs, @Nullable CursorSet set, ClickEffect spec,
                                         int guiScale) {
        CursorClickAnimation.tick(mouseX, mouseY, nowMs, spec);
        for (CursorClickAnimation.Effect effect : CursorClickAnimation.effects(nowMs)) {
            ClickEffectPainter.paint(extractor, set, effect.spec(), effect.x(), effect.y(),
                    CursorClickAnimation.progress(effect, nowMs), guiScale);
        }
    }

    /**
     * The frame of an animated image to draw.
     * <p>
     * Animation is driven by wall-clock time, so it keeps running while the game is paused. With the
     * switch off every cursor stands still on its first frame.
     *
     * @param image   the image about to be drawn
     * @param animate the player's animation switch
     * @param nowMs   wall-clock time in milliseconds
     * @return the frame index
     */
    static int frameFor(CursorImage image, boolean animate, long nowMs) {
        return animate ? image.frameAt(nowMs) : 0;
    }

    /**
     * The state whose image is drawn.
     * <p>
     * With animation switched off the cursor is the plain arrow and nothing else: frame 0 of
     * {@link CursorState#DEFAULT}, no matter which state the game is in.
     *
     * @param resolved the state the game would use
     * @param animate  the player's animation switch
     * @return the state to take the image from
     */
    static CursorState drawnState(CursorState resolved, boolean animate) {
        return animate ? resolved : CursorState.DEFAULT;
    }

    /** Logs what would be drawn; used by the debug command until the selection screen exists. */
    public static String describeCurrent() {
        CursorStateTracker.Resolved resolved = CursorStateTracker.resolve();
        CursorSet set = CursorManager.get().findSelected().orElse(null);
        return "state=" + resolved.state() + " set=" + (set == null ? "none" : set.describe())
                + " vanilla=" + resolved.context().vanilla()
                + " dragging=" + resolved.context().dragging()
                + " busy=" + resolved.context().busy()
                + " hidden=" + CursorVisibility.isHidden();
    }

    static {
        Constants.LOG.debug("Cursor kit renderer initialised");
    }
}
