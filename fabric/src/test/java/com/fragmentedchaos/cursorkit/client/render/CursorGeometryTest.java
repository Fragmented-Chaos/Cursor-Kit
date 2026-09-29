package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.client.render.CursorGeometry;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * High resolution cursors: the frame size is a property of the PNG, and for resource packs the mod
 * never decodes the image itself, so it measures the header instead.
 */
class CursorGeometryTest {

    private static final CursorImage FALLBACK = new CursorImage("arrow.png", 0, 0, 1, 100);

    @Test
    void headerOfAHighResolutionImageIsRead() {
        Identifier id = Identifier.fromNamespaceAndPath("cursorkit", "test/hd");

        assertTrue(CursorGeometry.recordPngHeader(id, png(32, 32)));

        CursorGeometry geometry = CursorGeometry.of(id, FALLBACK);
        assertEquals(32, geometry.frameSize());
        assertEquals(1, geometry.frames());
        assertEquals(0.5F, geometry.displayScale(), 1.0E-6);
    }

    @Test
    void anAnimationStripIsMeasuredToo() {
        Identifier id = Identifier.fromNamespaceAndPath("cursorkit", "test/strip");

        assertTrue(CursorGeometry.recordPngHeader(id, png(128, 16)));

        CursorGeometry geometry = CursorGeometry.of(id, new CursorImage("busy.png", 0, 0, 8, 80));
        assertEquals(16, geometry.frameSize());
        assertEquals(8, geometry.frames());
    }

    @Test
    void anythingThatIsNotAPngIsIgnored() {
        Identifier id = Identifier.fromNamespaceAndPath("cursorkit", "test/broken");

        assertFalse(CursorGeometry.recordPngHeader(id, new ByteArrayInputStream("nope".getBytes())));
        assertFalse(CursorGeometry.recordPngHeader(id, new ByteArrayInputStream(new byte[3])));

        // Falls back to what the JSON said.
        assertEquals(CursorGeometry.DEFAULT_FRAME_SIZE, CursorGeometry.of(id, FALLBACK).frameSize());
    }

    @Test
    void unknownTexturesUseTheDocumentedDefault() {
        Identifier id = Identifier.fromNamespaceAndPath("cursorkit", "test/never-read");

        assertEquals(CursorGeometry.DEFAULT_FRAME_SIZE, CursorGeometry.of(id, FALLBACK).frameSize());
        assertEquals(4, CursorGeometry.of(id, new CursorImage("x.png", 0, 0, 4, 100)).frames());
    }

    /** A stream that starts like a PNG and carries the given size in its IHDR. */
    private static InputStream png(int width, int height) {
        byte[] header = new byte[24];
        header[0] = (byte) 0x89;
        header[1] = 'P';
        header[2] = 'N';
        header[3] = 'G';
        header[12] = 'I';
        header[13] = 'H';
        header[14] = 'D';
        header[15] = 'R';
        putInt(header, 16, width);
        putInt(header, 20, height);
        return new ByteArrayInputStream(header);
    }

    private static void putInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }
}
