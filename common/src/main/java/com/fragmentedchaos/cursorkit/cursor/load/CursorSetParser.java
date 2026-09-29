package com.fragmentedchaos.cursorkit.cursor.load;

import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * Parses a cursor set JSON document into a {@link CursorSet}.
 * <p>
 * Deliberately free of Minecraft classes so it can be unit tested without a game instance.
 * <p>
 * Expected shape:
 * <pre>{@code
 * {
 *   "name": "demo",                       // optional display name, defaults to the file name
 *   "scale": 1,                           // optional extra integer scale, defaults to 1
 *   "states": {
 *     "default":   { "texture": "arrow.png", "hotspot": [0, 0] },
 *     "busy":      { "texture": "busy.png", "frames": 8, "frame_ms": 80 }
 *   }
 * }
 * }</pre>
 */
public final class CursorSetParser {

    private static final int DEFAULT_FRAME_MS = 100;

    /** Sanity limit for a hotspot coordinate; the real frame size is known once the PNG is read. */
    private static final int MAX_HOTSPOT = 1024;

    private CursorSetParser() {
        throw new UnsupportedOperationException("CursorSetParser cannot be instantiated");
    }

    /**
     * @param id        set identity, normally the file name without extension
     * @param namespace resource namespace the file was found in
     * @param origin    where the document came from
     * @param source    human readable origin for diagnostics
     * @param root      the parsed JSON document
     * @throws CursorSetFormatException when the document is structurally invalid
     */
    public static CursorSet parse(String id, String namespace, CursorSetOrigin origin,
                                 String source, JsonObject root) throws CursorSetFormatException {
        if (root == null) {
            throw new CursorSetFormatException("cursor set '" + id + "' is empty");
        }

        String name = id;
        String displayName = optionalString(root, "name", name);
        int scale = optionalInt(root, "scale", 1, 1, Integer.MAX_VALUE);

        JsonElement statesElement = root.get("states");
        if (statesElement == null || !statesElement.isJsonObject()) {
            throw new CursorSetFormatException(
                    "cursor set '" + id + "' has no 'states' object");
        }

        Map<CursorState, CursorImage> images = new EnumMap<>(CursorState.class);
        for (Map.Entry<String, JsonElement> entry : statesElement.getAsJsonObject().entrySet()) {
            CursorState state = CursorState.byId(entry.getKey());
            if (state == null) {
                Constants.LOG.warn("Cursor set '{}' declares unknown state '{}', ignored",
                        name, entry.getKey());
                continue;
            }
            images.put(state, parseImage(name, state, entry.getValue()));
        }

        if (!images.containsKey(CursorState.DEFAULT)) {
            throw new CursorSetFormatException(
                    "cursor set '" + id + "' does not provide the required 'default' state");
        }

        // Optional: a set that says nothing gets the mod's own click effect.
        ClickEffect clickEffect = ClickEffect.parse(root.get("click_effect"),
                "cursor set '" + id + "'");

        return new CursorSet(id, displayName, namespace, origin, source, scale, images, clickEffect);
    }

    private static CursorImage parseImage(String setName, CursorState state, JsonElement element)
            throws CursorSetFormatException {
        if (element == null || !element.isJsonObject()) {
            throw new CursorSetFormatException(where(setName, state) + " must be an object");
        }
        JsonObject object = element.getAsJsonObject();

        String texture = optionalString(object, "texture", null);
        if (texture == null || texture.isBlank()) {
            throw new CursorSetFormatException(where(setName, state) + " has no 'texture'");
        }

        // Left unset when the JSON has no hotspot: a cursor file brings its own.
        int hotspotX = CursorImage.UNSET_HOTSPOT;
        int hotspotY = CursorImage.UNSET_HOTSPOT;
        JsonElement hotspot = object.get("hotspot");
        if (hotspot != null) {
            if (!hotspot.isJsonArray() || hotspot.getAsJsonArray().size() != 2) {
                throw new CursorSetFormatException(
                        where(setName, state) + " 'hotspot' must be an array of two numbers");
            }
            JsonArray array = hotspot.getAsJsonArray();
            // Hotspot is in image pixels. How large one frame really is only shows up when the
            // texture is read (a frame may be 16, 32, ... pixels), so the upper bound here is only
            // a sanity limit; the loaded image is checked against the hotspot afterwards.
            hotspotX = requireInt(setName, state, "hotspot[0]", array.get(0), 0, MAX_HOTSPOT);
            hotspotY = requireInt(setName, state, "hotspot[1]", array.get(1), 0, MAX_HOTSPOT);
        }

        int frames = optionalInt(object, "frames", 1, 1, Integer.MAX_VALUE);
        int frameMs = optionalInt(object, "frame_ms", DEFAULT_FRAME_MS, 1, Integer.MAX_VALUE);

        try {
            return new CursorImage(texture, hotspotX, hotspotY, frames, frameMs);
        } catch (IllegalArgumentException e) {
            throw new CursorSetFormatException(where(setName, state) + " " + e.getMessage(), e);
        }
    }

    private static String where(String setName, CursorState state) {
        return "cursor set '" + setName + "' state '" + state.id() + "':";
    }

    private static String optionalString(JsonObject object, String key, String fallback)
            throws CursorSetFormatException {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new CursorSetFormatException("'" + key + "' must be a string");
        }
        return element.getAsString();
    }

    private static int optionalInt(JsonObject object, String key, int fallback, int min, int max)
            throws CursorSetFormatException {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return requireInt(null, null, key, element, min, max);
    }

    private static int requireInt(String setName, CursorState state, String key,
                                  JsonElement element, int min, int max)
            throws CursorSetFormatException {
        String prefix = (setName == null) ? "" : where(setName, state) + " ";
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) {
            throw new CursorSetFormatException(prefix + "'" + key + "' must be a number");
        }
        int value = element.getAsInt();
        if (value < min || value > max) {
            throw new CursorSetFormatException(
                    prefix + "'" + key + "' must be between " + min + " and " + max
                            + " (got " + value + ")");
        }
        return value;
    }
}
