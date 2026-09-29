package com.fragmentedchaos.cursorkit.cursor.model;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetFormatException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The click feedback a cursor set asks for.
 * <p>
 * Part of the cursor set JSON, so switching sets switches the effect with it:
 *
 * <pre>
 * "click_effect": {
 *   "type": "ripple",      // ripple, burst, pulse, image or none
 *   "color": "#FFD479",    // any RGB colour, the built-in types only
 *   "radius": 15,          // how far it travels, in GUI units
 *   "duration_ms": 450,    // how long it lives
 *   "particles": 6,        // dots for the ripple and burst types
 *   "texture": "click/ripple.png",  // the pack's own animation, for "image"
 *   "frames": 6,           // frames in that horizontal strip
 *   "frame_ms": 40,        // how long each of them is shown
 *   "size": 32             // how large it is drawn, in GUI units
 * }
 * </pre>
 * <p>
 * The {@code image} type is the real customisation: the pack ships a strip of frames and the mod
 * plays it at the click point, exactly like it plays a cursor's own animation. Its frames should
 * carry their own fade, since drawing a strip cannot fade it.
 * <p>
 * Every field is optional; a set that says nothing gets {@link #DEFAULT}, which is the effect the
 * mod always had. Deliberately Minecraft-free, so the parsing rules are unit testable.
 *
 * @param type       one of {@link #TYPES}
 * @param color      RGB, the alpha is the effect's own
 * @param radius     how far the effect travels, in GUI units
 * @param durationMs how long one effect lives
 * @param particles  how many dots fly out
 * @param texture    path of the pack's own animation, relative to the texture directory
 * @param frames     frames in that strip
 * @param frameMs    how long each frame is shown
 * @param size       how large the image is drawn, in GUI units
 */
public record ClickEffect(String type, int color, float radius, long durationMs, int particles,
                          String texture, int frames, int frameMs, int size) {

    /** Built-in effects a set can ask for. */
    public static final List<String> TYPES = List.of("ripple", "burst", "pulse", "image", "none");

    /** The effect used when a set does not define one: the ring with droplets. */
    public static final ClickEffect DEFAULT =
            new ClickEffect("ripple", 0xFFFFFF, 15.0F, 450L, 6, "", 1, 60, 32);

    /** No effect at all, which is what {@code "type": "none"} means. */
    public static final ClickEffect NONE =
            new ClickEffect("none", 0xFFFFFF, 0.0F, 0L, 0, "", 1, 60, 32);

    public ClickEffect {
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("unknown click effect type: " + type);
        }
    }

    /** @return true when this asks for nothing to be drawn */
    public boolean disabled() {
        return "none".equals(this.type) || this.durationMs <= 0L;
    }

    /** @return true when the pack brings its own animation instead of a built-in shape */
    public boolean isImage() {
        return "image".equals(this.type);
    }

    /**
     * The pack's own animation, described the way the rest of the mod describes an image.
     *
     * @return the image, or {@code null} when this effect is not one
     */
    public CursorImage asImage() {
        if (!isImage() || this.texture == null || this.texture.isBlank()) {
            return null;
        }
        return new CursorImage(this.texture, CursorImage.UNSET_HOTSPOT, CursorImage.UNSET_HOTSPOT,
                this.frames, this.frameMs);
    }

    /**
     * Reads a {@code click_effect} object.
     *
     * @param element the value found under {@code click_effect}, may be {@code null}
     * @param where   what to name in an error message
     * @return the effect, {@link #DEFAULT} when the set does not define one
     * @throws CursorSetFormatException when the object is malformed
     */
    public static ClickEffect parse(@Nullable JsonElement element, String where)
            throws CursorSetFormatException {
        if (element == null || element.isJsonNull()) {
            return DEFAULT;
        }
        if (!element.isJsonObject()) {
            throw new CursorSetFormatException(where + " 'click_effect' must be an object");
        }
        JsonObject object = element.getAsJsonObject();

        String type = DEFAULT.type();
        if (object.has("type")) {
            type = string(object, "type", where).toLowerCase(Locale.ROOT);
            if (!TYPES.contains(type)) {
                throw new CursorSetFormatException(where + " 'click_effect.type' must be one of "
                        + TYPES + ", not '" + type + "'");
            }
        }
        int color = object.has("color") ? color(object.get("color"), where) : DEFAULT.color();
        float radius = object.has("radius")
                ? clamp(number(object, "radius", where), 1.0F, 64.0F) : DEFAULT.radius();
        long duration = object.has("duration_ms")
                ? (long) clamp(number(object, "duration_ms", where), 30.0F, 5000.0F)
                : DEFAULT.durationMs();
        int particles = object.has("particles")
                ? (int) clamp(number(object, "particles", where), 0.0F, 32.0F) : DEFAULT.particles();
        String texture = object.has("texture") ? string(object, "texture", where) : "";
        int frames = object.has("frames")
                ? (int) clamp(number(object, "frames", where), 1.0F, 256.0F) : 1;
        int frameMs = object.has("frame_ms")
                ? (int) clamp(number(object, "frame_ms", where), 10.0F, 5000.0F) : 60;
        int size = object.has("size")
                ? (int) clamp(number(object, "size", where), 4.0F, 256.0F) : DEFAULT.size();

        if ("none".equals(type)) {
            return NONE;
        }
        if ("image".equals(type)) {
            if (texture.isBlank()) {
                throw new CursorSetFormatException(where
                        + " 'click_effect.type' is 'image', which needs a 'texture'");
            }
            // The animation's own length is the effect's life unless the set says otherwise.
            if (!object.has("duration_ms")) {
                duration = (long) frames * frameMs;
            }
        }
        return new ClickEffect(type, color, radius, duration, particles, texture, frames, frameMs,
                size);
    }

    /** @return this effect as the JSON a cursor set (or the config) would carry */
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.type);
        if (!"none".equals(this.type)) {
            object.addProperty("color", String.format("#%06X", this.color & 0xFFFFFF));
            object.addProperty("radius", this.radius);
            object.addProperty("duration_ms", this.durationMs);
            object.addProperty("particles", this.particles);
            if (this.isImage()) {
                object.addProperty("texture", this.texture);
                object.addProperty("frames", this.frames);
                object.addProperty("frame_ms", this.frameMs);
                object.addProperty("size", this.size);
            }
        }
        return object;
    }

    private static String string(JsonObject object, String key, String where)
            throws CursorSetFormatException {
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new CursorSetFormatException(where + " 'click_effect." + key + "' must be text");
        }
        return value.getAsString();
    }

    private static float number(JsonObject object, String key, String where)
            throws CursorSetFormatException {
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new CursorSetFormatException(where + " 'click_effect." + key + "' must be a number");
        }
        return value.getAsFloat();
    }

    /** Accepts {@code "#RRGGBB"}, {@code "RRGGBB"} or a plain decimal number. */
    private static int color(JsonElement value, String where) throws CursorSetFormatException {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsInt() & 0xFFFFFF;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new CursorSetFormatException(where + " 'click_effect.color' must be text or a number");
        }
        String text = value.getAsString().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6) {
            throw new CursorSetFormatException(where
                    + " 'click_effect.color' must look like '#RRGGBB', not '" + value.getAsString() + "'");
        }
        try {
            return Integer.parseInt(text, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            throw new CursorSetFormatException(where + " 'click_effect.color' is not a colour: '"
                    + value.getAsString() + "'");
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
