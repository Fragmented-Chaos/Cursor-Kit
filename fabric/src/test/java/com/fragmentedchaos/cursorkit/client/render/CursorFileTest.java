package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.client.render.CursorFile;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Windows cursor files carry their own size and click point, so a {@code .cur} can be dropped into
 * the config directory without converting it first.
 */
class CursorFileTest {

    private static final int HEADER = 22;   // icon directory (6) + one entry (16)

    @Test
    void aCursorFileIsRecognisedByItsHeader() {
        assertTrue(CursorFile.isCursor(cursor(4, 4, 1, 2, 0xFF112233)));
        assertFalse(CursorFile.isCursor(new byte[] {(byte) 0x89, 'P', 'N', 'G'}));
        assertFalse(CursorFile.isCursor(new byte[0]));
    }

    @Test
    void sizeAndClickPointComeFromTheDirectory() {
        CursorFile.Entry entry = CursorFile.largest(cursor(4, 4, 1, 2, 0xFF112233));

        assertNotNull(entry);
        assertEquals(4, entry.width());
        assertEquals(4, entry.height());
        assertEquals(1, entry.hotspotX());
        assertEquals(2, entry.hotspotY());
        assertFalse(entry.png());
    }

    @Test
    void aBitmapInfoHeaderSizeOfZeroMeansTwoHundredFiftySix() {
        // Some cursor files store 256 as a zero byte; the decoder must not treat it as empty.
        CursorFile.Entry entry = CursorFile.largest(cursor(0, 0, 0, 0, 0xFF000000));
        assertNotNull(entry);
        assertEquals(256, entry.width());
    }

    @Test
    void brokenFilesAreRejected() {
        assertNull(CursorFile.largest(new byte[] {0, 0, 2, 0, 1, 0}));
        assertNull(CursorFile.largest(new byte[0]));
    }

    /**
     * A 4x4 32bpp cursor whose every pixel has the given colour; rows are stored bottom up and the
     * mask follows the colour data, exactly like Windows writes them.
     */
    private static byte[] cursor(int width, int height, int hotspotX, int hotspotY, int argb) {
        int colourStride = width * 4;
        int maskStride = ((width + 31) / 32) * 4;
        int dibBytes = 40 + colourStride * height + maskStride * height;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER + dibBytes).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short) 0);          // reserved
        buffer.putShort((short) 2);          // type: cursor
        buffer.putShort((short) 1);          // one image
        buffer.put((byte) width);
        buffer.put((byte) height);
        buffer.put((byte) 0);                // palette size
        buffer.put((byte) 0);                // reserved
        buffer.putShort((short) hotspotX);
        buffer.putShort((short) hotspotY);
        buffer.putInt(dibBytes);
        buffer.putInt(HEADER);

        buffer.putInt(40);                   // BITMAPINFOHEADER
        buffer.putInt(width);
        buffer.putInt(height * 2);           // colour plus mask
        buffer.putShort((short) 1);
        buffer.putShort((short) 32);
        buffer.putInt(0);                    // BI_RGB
        buffer.putInt(colourStride * height);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);

        int blue = argb & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                buffer.put((byte) blue);
                buffer.put((byte) green);
                buffer.put((byte) red);
                buffer.put((byte) alpha);
            }
        }
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < maskStride; column++) {
                buffer.put((byte) 0);
            }
        }
        return buffer.array();
    }
}
