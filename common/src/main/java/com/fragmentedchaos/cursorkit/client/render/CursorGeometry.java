package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real size of a cursor image, measured when the texture is read.
 * <p>
 * The JSON can only say how many frames a state has, not how large one frame is: that is a property
 * of the PNG. Reading the image tells us, so a cursor whose frames are 32x32 (or 24x24, or 48x48)
 * works just like a 16x16 one - it is scaled down to {@link CursorImage#FRAME_SIZE} GUI units and
 * the hotspot, which is written in image pixels, is scaled with it.
 * <p>
 * Until a texture has been read the JSON's assumption (16x16) is used.
 */
public final class CursorGeometry {

    /** Frame size assumed before the image has been read. */
    public static final int DEFAULT_FRAME_SIZE = CursorImage.FRAME_SIZE;

    private static final Map<Identifier, CursorGeometry> MEASURED = new ConcurrentHashMap<>();

    /** Signature (8) + IHDR length (4) + "IHDR" (4) + width (4) + height (4). */
    private static final int PNG_HEADER_BYTES = 24;

    private final int frameSize;
    private final int frames;
    /** Click point read from a cursor file, or -1 when the file did not provide one. */
    private final int hotspotX;
    private final int hotspotY;

    private CursorGeometry(int frameSize, int frames, int hotspotX, int hotspotY) {
        this.frameSize = frameSize;
        this.frames = frames;
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    /** Records what an image really contains, called right after it was decoded. */
    public static void record(Identifier texture, int frameSize, int frames) {
        CursorGeometry previous = MEASURED.get(texture);
        MEASURED.put(texture, new CursorGeometry(frameSize, frames,
                previous == null ? -1 : previous.hotspotX,
                previous == null ? -1 : previous.hotspotY));
    }

    /** Records a click point that came from the image itself, as a {@code .cur} carries one. */
    public static void recordHotspot(Identifier texture, int hotspotX, int hotspotY) {
        CursorGeometry previous = MEASURED.get(texture);
        int frameSize = previous == null ? DEFAULT_FRAME_SIZE : previous.frameSize;
        int frames = previous == null ? 1 : previous.frames;
        MEASURED.put(texture, new CursorGeometry(frameSize, frames, hotspotX, hotspotY));
    }

    /**
     * Measures the size straight from a PNG header.
     * <p>
     * Needed for cursor sets inside a resource pack: those textures are loaded by Minecraft's own
     * texture manager, so the mod never decodes them and would otherwise keep assuming 16x16 - which
     * turned every high resolution cursor into a crop of its top left corner.
     *
     * @param texture the texture the image belongs to
     * @param stream  a stream of the PNG, only the header is read
     * @return true when the header could be read
     */
    /**
     * @param image the description from the JSON, whose hotspot may be "unset" (-1)
     * @return the click point to use: the one from the JSON, else the image's own, else 0
     */
    public int hotspotX(CursorImage image) {
        if (image.hotspotX() >= 0) {
            return image.hotspotX();
        }
        return this.hotspotX >= 0 ? this.hotspotX : 0;
    }

    /** @see #hotspotX(CursorImage) */
    public int hotspotY(CursorImage image) {
        if (image.hotspotY() >= 0) {
            return image.hotspotY();
        }
        return this.hotspotY >= 0 ? this.hotspotY : 0;
    }

    public static boolean recordPngHeader(Identifier texture, java.io.InputStream stream) {
        try {
            byte[] header = stream.readNBytes(PNG_HEADER_BYTES);
            if (header.length < PNG_HEADER_BYTES || header[0] != (byte) 0x89 || header[1] != 'P') {
                return false;
            }
            int width = readInt(header, 16);
            int height = readInt(header, 20);
            if (width <= 0 || height <= 0) {
                return false;
            }
            record(texture, height, Math.max(1, width / height));
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    /**
     * @param fallback the image description from the JSON, used before the texture was read
     * @return the measured geometry of {@code texture}, or the fallback when it is not known yet
     */
    public static CursorGeometry of(Identifier texture, CursorImage fallback) {
        CursorGeometry measured = texture == null ? null : MEASURED.get(texture);
        return measured != null ? measured
                : new CursorGeometry(DEFAULT_FRAME_SIZE, fallback.frames(), -1, -1);
    }

    /** @return width and height of one frame, in image pixels */
    public int frameSize() {
        return this.frameSize;
    }

    /** @return how many frames the image holds */
    public int frames() {
        return this.frames;
    }

    /**
     * @return how much a coordinate written in image pixels has to be multiplied with to land in the
     *         16 GUI units a cursor is drawn at
     */
    public float displayScale() {
        return (float) CursorImage.FRAME_SIZE / this.frameSize;
    }

    /** Forgets everything; only used when the game shuts down in tests. */
    static void clear() {
        MEASURED.clear();
    }
}
