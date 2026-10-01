package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.Constants;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The cursor set the player assembles by hand: one image file per state, each anywhere on disk.
 * <p>
 * It is rebuilt from the configuration every time the sets are read, and behaves like any other set
 * afterwards - the picker lists it, the renderer draws it and the state resolution picks whichever of
 * its states the game is in. States the player left empty simply are not part of it, so the usual
 * fallback to {@code default} applies.
 * <p>
 * Deliberately Minecraft-free, so the rules are unit testable.
 */
public final class CursorSets {

    /** Id of the hand-made set; it is not a file name, so nothing on disk can collide with it. */
    public static final String CUSTOM_ID = "custom";

    /** Name shown for it; the picker translates this id instead of using the text. */
    public static final String CUSTOM_NAME = "Custom";

    private CursorSets() {
        throw new UnsupportedOperationException("CursorSets cannot be instantiated");
    }

    /**
     * Builds the hand-made set from the configuration.
     *
     * @param config        the loaded configuration
     * @param gameDirectory relative paths are taken from here, may be {@code null}
     * @param source        what to show as its source, usually the configuration file
     * @return the set, which may carry no images at all: the picker always offers the hand-made row
     *         so the player can open the path editor and fill it in
     */
    public static CursorSet custom(CursorConfig config, @Nullable Path gameDirectory,
                                   String source) {
        Map<CursorState, CursorImage> images = new LinkedHashMap<>();
        for (CursorState state : CursorState.values()) {
            String configured = config.customState(state.id());
            if (configured.isBlank()) {
                continue;
            }
            Path file = resolve(configured, gameDirectory);
            if (file == null || !Files.isRegularFile(file)) {
                // A path that leads nowhere is kept in the configuration - the player may just be
                // half way through fixing a typo - but it cannot be drawn.
                continue;
            }
            // The path doubles as the image's texture: the renderer reads that file directly.
            // Frames and click point are measured from the file like for any other loose set.
            images.put(state, new CursorImage(file.toString(), CursorImage.UNSET_HOTSPOT,
                    CursorImage.UNSET_HOTSPOT, 1, 100));
        }
        return new CursorSet(CUSTOM_ID, CUSTOM_NAME, Constants.MOD_ID,
                CursorSetOrigin.CUSTOM_STATES, source, 1, Map.copyOf(images),
                effect(config.customEffect(), gameDirectory));
    }

    /**
     * Resolves the animation of a hand-made effect, the same way the per-state paths are resolved:
     * the file may sit anywhere, and a relative path starts at the game directory.
     *
     * @param effect        the effect the player configured
     * @param gameDirectory what relative paths are resolved against, may be {@code null}
     * @return the effect to use, with an absolute path; a missing file simply drops the animation
     */
    static ClickEffect effect(ClickEffect effect, @Nullable Path gameDirectory) {
        if (effect == null || !effect.isImage() || effect.texture().isBlank()) {
            return effect == null ? ClickEffect.DEFAULT : effect;
        }
        Path file = resolve(effect.texture(), gameDirectory);
        if (file == null || !Files.isRegularFile(file)) {
            // Nothing to play: fall back to the mod's own effect rather than drawing nothing at all.
            return ClickEffect.DEFAULT;
        }
        return new ClickEffect(effect.type(), effect.color(), effect.radius(), effect.durationMs(),
                effect.particles(), file.toString(), effect.frames(), effect.frameMs(),
                effect.size());
    }

    /**
     * @param configured    the path as typed
     * @param gameDirectory what relative paths are resolved against, may be {@code null}
     * @return the absolute path, or {@code null} when the text is not a path at all
     */
    public static @Nullable Path resolve(String configured, @Nullable Path gameDirectory) {
        try {
            Path path = Path.of(configured);
            if (!path.isAbsolute() && gameDirectory != null) {
                path = gameDirectory.resolve(path);
            }
            return path.normalize();
        } catch (RuntimeException e) {
            // A NUL byte, a broken drive letter, a path longer than the platform allows: the picker
            // shows the text back and nothing is drawn from it.
            return null;
        }
    }
}
