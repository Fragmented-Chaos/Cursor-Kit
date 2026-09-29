package com.fragmentedchaos.cursorkit.cursor.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * A complete set of cursor images, as offered by a resource pack, a built-in preset or a PNG
 * dropped into {@code config/cursorkit/}.
 *
 * @param id        identity of the set: the file name without extension. This is what the config
 *                  stores and what decides which set wins when two share the name, so a resource
 *                  pack can replace a built-in simply by using the same file name
 * @param name      display name shown in the selection screen
 * @param namespace resource namespace the set belongs to ({@code cursorkit} for built-ins)
 * @param origin    where the set came from; decides texture resolution and name precedence
 * @param source    human readable origin, used in log messages ("vanilla", pack id, file path)
 * @param scale     extra integer scale on top of the GUI scale, at least 1
 * @param images    the images this set provides, always containing {@link CursorState#DEFAULT}
 * @param clickEffect the click feedback this set asks for, so switching sets switches the effect
 */
public record CursorSet(String id, String name, String namespace, CursorSetOrigin origin,
                        String source, int scale, Map<CursorState, CursorImage> images,
                        ClickEffect clickEffect) {

    /** A set that does not care about the click effect gets the mod's own. */
    public CursorSet(String id, String name, String namespace, CursorSetOrigin origin,
                     String source, int scale, Map<CursorState, CursorImage> images) {
        this(id, name, namespace, origin, source, scale, images, ClickEffect.DEFAULT);
    }

    public CursorSet {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (scale < 1) {
            throw new IllegalArgumentException("scale must be at least 1");
        }
        clickEffect = clickEffect == null ? ClickEffect.DEFAULT : clickEffect;
        images = Map.copyOf(images);
        if (!images.containsKey(CursorState.DEFAULT)) {
            throw new IllegalArgumentException("a cursor set must provide the default state");
        }
    }

    /** @return true when this set provides a dedicated image for the state */
    public boolean has(CursorState state) {
        return this.images.containsKey(state);
    }

    /**
     * @return the image to draw for the state, falling back to the default image when the set does
     *         not provide that state
     */
    public Optional<CursorImage> image(CursorState state) {
        CursorImage image = this.images.get(state);
        if (image == null) {
            image = this.images.get(CursorState.DEFAULT);
        }
        return Optional.ofNullable(image);
    }

    /** @return a copy of this set with an extra image, used to layer config PNGs over presets */
    public CursorSet with(CursorState state, CursorImage image) {
        EnumMap<CursorState, CursorImage> merged = new EnumMap<>(CursorState.class);
        merged.putAll(this.images);
        merged.put(state, image);
        return new CursorSet(this.id, this.name, this.namespace, this.origin, this.source,
                this.scale, merged);
    }

    /**
     * @return a copy of this set with one state's click point moved; the set itself is unchanged
     */
    public CursorSet withHotspot(CursorState state, int x, int y) {
        CursorImage image = this.images.get(state);
        if (image == null) {
            // A state that only borrows default's picture still gets its own click point, instead of
            // silently editing default and dragging every other fallback state along.
            image = this.images.get(CursorState.DEFAULT);
            if (image == null) {
                return this;
            }
        }
        Map<CursorState, CursorImage> moved = new EnumMap<>(this.images);
        moved.put(state, image.withHotspot(x, y));
        // The effect has to travel along: dropping it here silently reset every set that had a click
        // point edited back to the mod's own ripple.
        return new CursorSet(this.id, this.name, this.namespace, this.origin, this.source,
                this.scale, moved, this.clickEffect);
    }

    /** @return a short description for log messages */
    public String describe() {
        return this.id + " '" + this.name + "' (" + this.origin.label() + ", " + this.source + ")";
    }
}
