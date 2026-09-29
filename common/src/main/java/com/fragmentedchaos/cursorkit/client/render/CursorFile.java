package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Reader for Windows cursor files ({@code .cur}), so existing system cursors can be dropped into
 * {@code config/cursorkit/} or a cursor pack without converting them first.
 * <p>
 * A {@code .cur} is an icon directory that holds one or more images (16x16, 32x32, 48x48...) plus the
 * click point, either as a PNG or as a bottom-up DIB with a separate one bit transparency mask. The
 * largest image is used; the size and the click point are read from the directory.
 * <p>
 * Minecraft's own texture loader only understands PNG, so a {@code .cur} only works from this mod's
 * own sources (the config directory, cursor packs and the built-in sets) - a resource pack would
 * have to ship a PNG.
 */
public final class CursorFile {

    private static final int TYPE_CURSOR = 2;
    private static final int DIRECTORY_ENTRY_BYTES = 16;
    private static final int BITMAP_INFO_HEADER_BYTES = 40;

    private CursorFile() {
        throw new UnsupportedOperationException("CursorFile cannot be instantiated");
    }

    /** @return true when the bytes start with a cursor directory */
    public static boolean isCursor(byte[] data) {
        return data.length > 6 && data[0] == 0 && data[1] == 0
                && data[2] == TYPE_CURSOR && data[3] == 0;
    }

    /** One image inside a cursor file. */
    public record Entry(int width, int height, int hotspotX, int hotspotY, int bytes, int offset,
                        boolean png) {
    }

    /**
     * @return the largest image in the file, or {@code null} when the file cannot be read
     */
    public static @Nullable Entry largest(byte[] data) {
        if (!isCursor(data)) {
            return null;
        }
        int count = readShort(data, 4);
        Entry best = null;
        for (int index = 0; index < count; index++) {
            int at = 6 + index * DIRECTORY_ENTRY_BYTES;
            if (at + DIRECTORY_ENTRY_BYTES > data.length) {
                break;
            }
            int width = data[at] & 0xFF;
            int height = data[at + 1] & 0xFF;
            // A zero size means 256 in the icon format.
            width = width == 0 ? 256 : width;
            height = height == 0 ? 256 : height;
            int hotspotX = readShort(data, at + 4);
            int hotspotY = readShort(data, at + 6);
            int bytes = readInt(data, at + 8);
            int offset = readInt(data, at + 12);
            if (offset <= 0 || offset >= data.length || bytes <= 0) {
                continue;
            }
            boolean png = bytes >= 8 && data[offset] == (byte) 0x89 && data[offset + 1] == 'P';
            Entry entry = new Entry(width, height, hotspotX, hotspotY,
                    Math.min(bytes, data.length - offset), offset, png);
            if (best == null || entry.width() * entry.height() > best.width() * best.height()) {
                best = entry;
            }
        }
        return best;
    }

    /**
     * @return the largest image decoded as a texture ready image, or {@code null} when the format is
     *         not supported
     */
    public static @Nullable NativeImage decode(byte[] data) {
        Entry entry = largest(data);
        if (entry == null) {
            return null;
        }
        try {
            if (entry.png()) {
                return NativeImage.read(new ByteArrayInputStream(data, entry.offset(), entry.bytes()));
            }
            return decodeDib(data, entry);
        } catch (IOException | RuntimeException e) {
            Constants.LOG.warn("Could not read the cursor image at {}: {}", entry.offset(),
                    e.toString());
            return null;
        }
    }

    /**
     * Decodes a bottom-up DIB with an AND mask, the format every Windows cursor uses.
     * <p>
     * Rows are stored bottom to top and padded to four bytes, the mask follows the colour data, and a
     * set mask bit means "transparent". When the colour data carries an alpha channel that is not
     * entirely empty it wins, which is what Windows does as well.
     */
    private static NativeImage decodeDib(byte[] data, Entry entry) {
        int base = entry.offset();
        int headerSize = readInt(data, base);
        int width = readInt(data, base + 4);
        int height = readInt(data, base + 8) / 2;
        int bitCount = readShort(data, base + 14);
        int compression = readInt(data, base + 16);
        if (headerSize < BITMAP_INFO_HEADER_BYTES || width <= 0 || height <= 0 || compression != 0
                || (bitCount != 32 && bitCount != 24)) {
            Constants.LOG.warn("Unsupported cursor image: {}x{} {}bpp compression {}",
                    width, height, bitCount, compression);
            return null;
        }

        int colourStride = ((width * bitCount + 31) / 32) * 4;
        int maskStride = ((width + 31) / 32) * 4;
        int colourStart = base + headerSize;
        int maskStart = colourStart + colourStride * height;
        int bytesPerPixel = bitCount / 8;

        int[][] pixels = new int[height][width];
        boolean alphaUsed = false;
        for (int row = 0; row < height; row++) {
            int source = colourStart + (height - 1 - row) * colourStride;
            for (int column = 0; column < width; column++) {
                int at = source + column * bytesPerPixel;
                if (at + bytesPerPixel > data.length) {
                    return null;
                }
                int blue = data[at] & 0xFF;
                int green = data[at + 1] & 0xFF;
                int red = data[at + 2] & 0xFF;
                int alpha = bytesPerPixel == 4 ? data[at + 3] & 0xFF : 255;
                alphaUsed |= alpha != 0;
                pixels[row][column] = (alpha << 24) | (blue << 16) | (green << 8) | red;
            }
        }

        NativeImage image = new NativeImage(width, height, false);
        for (int row = 0; row < height; row++) {
            int maskRow = maskStart + (height - 1 - row) * maskStride;
            for (int column = 0; column < width; column++) {
                int packed = pixels[row][column];
                int alpha = packed >>> 24;
                if (!alphaUsed) {
                    int maskAt = maskRow + column / 8;
                    boolean transparent = maskAt >= data.length
                            || ((data[maskAt] >> (7 - column % 8)) & 1) != 0;
                    alpha = transparent ? 0 : 255;
                }
                image.setPixelABGR(column, row, (alpha << 24) | (packed & 0x00FFFFFF));
            }
        }
        return image;
    }

    private static int readShort(byte[] data, int at) {
        return (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8);
    }

    private static int readInt(byte[] data, int at) {
        return (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8) | ((data[at + 2] & 0xFF) << 16)
                | ((data[at + 3] & 0xFF) << 24);
    }
}
