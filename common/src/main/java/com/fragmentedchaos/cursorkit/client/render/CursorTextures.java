package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import net.minecraft.resources.Identifier;

/**
 * Turns the texture path stored in a cursor set into a texture {@link Identifier}.
 * <ul>
 *   <li>bundled and resource pack sets keep their images under
 *       {@code assets/<namespace>/textures/cursor/...}</li>
 *   <li>cursor packs in {@code config/cursorkit/packs/} are not part of any pack either, but they
 *       carry a namespace of their own, so they only need their path sanitised</li>
 *   <li>loose config directory sets are not part of any pack, so they get a synthetic id under
 *       {@code cursorkit:config/<set>/...} that we register ourselves</li>
 *   <li>the images the player picked per state live anywhere on disk, so they get their own id
 *       under {@code cursorkit:custom/...}</li>
 * </ul>
 */
public final class CursorTextures {

    /** Directory inside {@code textures/} that holds cursor set images. */
    public static final String TEXTURE_DIRECTORY = "textures/cursor/";

    /** Prefix for textures we register ourselves (config directory sets). */
    public static final String CONFIG_PREFIX = "config/";

    /** Prefix for the images the player picked state by state. */
    public static final String CUSTOM_PREFIX = "custom/";

    private CursorTextures() {
        throw new UnsupportedOperationException("CursorTextures cannot be instantiated");
    }

    public static Identifier resolve(CursorSet set, CursorImage image) {
        if (set.origin() == CursorSetOrigin.CUSTOM_STATES) {
            // One id per image, not per state: two states may well share a file, and the id has to
            // stay stable while the player edits the path next to it.
            return Identifier.fromNamespaceAndPath(Constants.MOD_ID,
                    CUSTOM_PREFIX + sanitize(image.texture().replace(':', '_')));
        }
        if (set.origin() == CursorSetOrigin.CONFIG) {
            return Identifier.fromNamespaceAndPath(Constants.MOD_ID,
                    CONFIG_PREFIX + sanitize(set.id()) + "/" + sanitize(image.texture()));
        }
        if (set.origin() == CursorSetOrigin.CONFIG_PACK) {
            return Identifier.fromNamespaceAndPath(set.namespace(),
                    TEXTURE_DIRECTORY + sanitize(image.texture()));
        }
        // Resource pack sets have to keep the pack's own id byte for byte, that is the id the
        // texture manager already loaded them under.
        return Identifier.fromNamespaceAndPath(set.namespace(), TEXTURE_DIRECTORY + image.texture());
    }

    /**
     * Makes a set id usable as an identifier path segment. Config file names are free-form, but
     * identifiers only accept {@code [a-z0-9/._-]}. The result is only a registry key, the pixels
     * always come from {@link CursorAssetSource}.
     */
    private static String sanitize(String id) {
        StringBuilder result = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = Character.toLowerCase(id.charAt(i));
            result.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '/' || c == '.' || c == '_' || c == '-' ? c : '_');
        }
        return result.toString();
    }
}
