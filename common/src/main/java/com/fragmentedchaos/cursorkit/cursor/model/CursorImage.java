package com.fragmentedchaos.cursorkit.cursor.model;

/**
 * One state's image inside a cursor set.
 * <p>
 * Animated images use a <b>single horizontal strip</b>: every frame is exactly one cursor square
 * (16x16) wide and the strip is {@code frames * 16} pixels wide. That keeps the frame geometry
 * derivable from the JSON alone, without decoding the PNG first.
 *
 * @param texture  path of the PNG, relative to the cursor set's own directory
 * @param hotspotX x of the click point inside the frame (0 = left edge)
 * @param hotspotY y of the click point inside the frame (0 = top edge)
 * @param frames   number of frames in the strip, at least 1
 * @param frameMs  how long each frame is shown, in milliseconds, at least 1
 */
public record CursorImage(String texture, int hotspotX, int hotspotY, int frames, int frameMs) {

    /** Value of {@link #hotspotX()} / {@link #hotspotY()} when the JSON did not specify one. */
    public static final int UNSET_HOTSPOT = -1;

    /**
     * Size a cursor is drawn at, in GUI units, and the frame size assumed while parsing. A texture
     * whose frames are larger (32x32, for example) is scaled down to this, see
     * {@code CursorGeometry}.
     */
    public static final int FRAME_SIZE = 16;

    public CursorImage {
        if (texture == null || texture.isBlank()) {
            throw new IllegalArgumentException("texture must not be blank");
        }
        // -1 means "not specified": a .cur file carries its own click point, which is used instead.
        if (hotspotX < -1 || hotspotY < -1) {
            throw new IllegalArgumentException("hotspot must not be negative");
        }

        if (frames < 1) {
            throw new IllegalArgumentException("frames must be at least 1");
        }
        if (frameMs < 1) {
            throw new IllegalArgumentException("frameMs must be at least 1");
        }
    }

    /** @return a copy of this image with the click point moved, used by the hotspot editor */
    public CursorImage withHotspot(int x, int y) {
        return new CursorImage(this.texture, x, y, this.frames, this.frameMs);
    }

    /** @return true when this image needs to be animated */
    public boolean animated() {
        return this.frames > 1;
    }

    /** Total animation duration in milliseconds, {@code 0} when static. */
    public long animationLengthMs() {
        return this.animated() ? (long) this.frames * this.frameMs : 0L;
    }

    /**
     * @param nowMs a monotonically increasing time in milliseconds
     * @return the frame index to draw for that moment
     */
    public int frameAt(long nowMs) {
        if (!this.animated()) {
            return 0;
        }
        long elapsed = nowMs % animationLengthMs();
        return (int) (elapsed / this.frameMs);
    }
}
