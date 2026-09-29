package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * The ripple a click leaves at the cursor's position.
 * <p>
 * This is the click feedback ("水波纹", click ripple) desktop cursor tools and Windows' own pen input
 * use: something expands from the point that was clicked and fades out. It comes from the cursor set
 * ({@link ClickEffect}), so switching sets switches the effect along with the cursor, and the cursor
 * itself is never touched - clicking must not make aiming harder.
 * <p>
 * The state and the curves live here and stay Minecraft-free; the renderer asks
 * {@link #effects(long)} for what is alive and draws it, reading the shape from
 * {@link #radius(ClickEffect, float)} and {@link #alpha(ClickEffect, float)}.
 */
public final class CursorClickAnimation {

    /** One ripple: where it started, when, and the effect it was spawned with. */
    public record Effect(double x, double y, long startMs, ClickEffect spec) {
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();

    /** Most recent press seen by {@code MouseHandlerMixin}. */
    private static volatile long lastPressMs = Long.MIN_VALUE;
    /** The press the renderer has already turned into a ripple. */
    private static long spawnedPressMs = Long.MIN_VALUE;

    private CursorClickAnimation() {
        throw new UnsupportedOperationException("CursorClickAnimation cannot be instantiated");
    }

    /** Called whenever a mouse button goes down. */
    public static void press() {
        pressAt(System.nanoTime() / 1_000_000L);
    }

    /** Arms the effect at a given time, used by tests. */
    public static void pressAt(long nowMs) {
        lastPressMs = nowMs;
    }

    /**
     * Called once per frame with where the cursor currently is.
     * <p>
     * A press that has not been turned into a ripple yet spawns one here, which is why the position
     * comes from the renderer: the mouse handler only knows that a button went down. The spec is
     * copied into the ripple, so changing sets while one is in flight does not warp it.
     *
     * @param x     cursor x in GUI units
     * @param y     cursor y in GUI units
     * @param nowMs wall-clock time in milliseconds
     * @param spec  the selected set's effect
     */
    public static void tick(double x, double y, long nowMs, ClickEffect spec) {
        if (spec != null && !spec.disabled() && lastPressMs > spawnedPressMs) {
            spawnedPressMs = lastPressMs;
            EFFECTS.add(new Effect(x, y, nowMs, spec));
            Constants.LOG.info("Click effect: {} colour #{} (set selected: {})", spec.type(),
                    String.format("%06X", spec.color() & 0xFFFFFF),
                    com.fragmentedchaos.cursorkit.cursor.CursorManager.get()
                            .findSelected().map(set -> set.id()).orElse("system"));
        } else if (lastPressMs > spawnedPressMs) {
            spawnedPressMs = lastPressMs;
        }
        EFFECTS.removeIf(effect -> progress(effect, nowMs) >= 1.0F);
    }

    /** @return the ripples that are still visible, oldest first */
    public static List<Effect> effects(long nowMs) {
        List<Effect> alive = new ArrayList<>(EFFECTS.size());
        for (Effect effect : EFFECTS) {
            if (progress(effect, nowMs) < 1.0F) {
                alive.add(effect);
            }
        }
        return alive;
    }

    /** Drops the ripples on screen and forgets the last press, without arming a new one. */
    public static void clear() {
        EFFECTS.clear();
        spawnedPressMs = lastPressMs;
    }

    /** Forgets everything, including which press was last seen; for tests and the debug aid. */
    static void reset() {
        EFFECTS.clear();
        lastPressMs = Long.MIN_VALUE;
        spawnedPressMs = Long.MIN_VALUE;
    }

    /** @return how far through its life the ripple is, 0 at the start and 1 when it is gone */
    public static float progress(Effect effect, long nowMs) {
        long duration = effect.spec().durationMs();
        if (duration <= 0L) {
            return 1.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, (float) (nowMs - effect.startMs()) / duration));
    }

    /**
     * @param spec     the effect a set asked for
     * @param progress {@link #progress(Effect, long)}
     * @return how far the effect has travelled, in GUI units: quick at first, slowing down as it fades
     */
    public static float radius(ClickEffect spec, float progress) {
        float inverse = 1.0F - progress;
        return spec.radius() * (1.0F - inverse * inverse * inverse);
    }

    /**
     * @param spec     the effect a set asked for
     * @param progress {@link #progress(Effect, long)}
     * @return the alpha to draw with, 0-255: solid at the start, gone by the end
     */
    public static int alpha(ClickEffect spec, float progress) {
        return Math.round(255.0F * (1.0F - progress) * (1.0F - progress));
    }
}
