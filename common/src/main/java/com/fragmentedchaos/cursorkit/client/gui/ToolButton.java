package com.fragmentedchaos.cursorkit.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The small square button in the picker's header: a hand-drawn glyph instead of a label.
 * <p>
 * Two labelled buttons next to the title looked like a second footer, and a fixed width would cut
 * long translations off. A glyph has neither problem; the text lives in the tooltip, which is also
 * where a player looks for it.
 */
public class ToolButton extends FlatButton {

    /** Which glyph to draw. */
    public enum Glyph {
        /** A folder: the config directory. */
        FOLDER,
        /** A crosshair: the click point editor. */
        HOTSPOT,
        /** A lowercase i: what this mod is, and where its files live. */
        ABOUT
    }

    private static final int FILL = 0x99000000;
    private static final int FILL_HOVERED = 0xBB1E1E1E;
    private static final int FILL_DISABLED = 0x55000000;
    private static final int BORDER = 0x38FFFFFF;
    private static final int BORDER_HOVERED = 0x90FFFFFF;
    private static final int BORDER_DISABLED = 0x18FFFFFF;
    private static final int GLYPH = 0xFFFFFFFF;
    private static final int GLYPH_DISABLED = 0xFF8A8A8A;
    private static final int RADIUS = 4;

    private final Glyph glyph;

    public ToolButton(int x, int y, int size, Glyph glyph, Component tooltip, OnPress onPress) {
        super(x, y, size, size, tooltip, onPress);
        this.glyph = glyph;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        boolean hovered = isHoveredOrFocused();

        int fill;
        int border;
        int glyphColour;
        if (!this.active) {
            fill = FILL_DISABLED;
            border = BORDER_DISABLED;
            glyphColour = GLYPH_DISABLED;
        } else if (hovered) {
            fill = FILL_HOVERED;
            border = BORDER_HOVERED;
            glyphColour = GLYPH;
        } else {
            fill = FILL;
            border = BORDER;
            glyphColour = GLYPH;
        }

        roundedRect(extractor, x, y, width, height, RADIUS, border);
        roundedRect(extractor, x + 1, y + 1, width - 2, height - 2, RADIUS - 1, fill);

        int cx = x + width / 2;
        int cy = y + height / 2;
        if (this.glyph == Glyph.FOLDER) {
            // A tab peeking over a body: reads as a folder even at 18 pixels.
            extractor.fill(cx - 4, cy - 5, cx - 1, cy - 3, glyphColour);
            extractor.fill(cx - 5, cy - 3, cx + 5, cy + 4, glyphColour);
        } else if (this.glyph == Glyph.ABOUT) {
            // A dot over a bar: the usual "information" mark, readable at 18 pixels.
            extractor.fill(cx - 1, cy - 5, cx + 1, cy - 3, glyphColour);
            extractor.fill(cx - 1, cy - 1, cx + 1, cy + 5, glyphColour);
        } else {
            // Four ticks around a centre dot: the click point marker the editor uses.
            extractor.fill(cx - 1, cy - 5, cx + 1, cy - 2, glyphColour);
            extractor.fill(cx - 1, cy + 3, cx + 1, cy + 6, glyphColour);
            extractor.fill(cx - 5, cy - 1, cx - 2, cy + 1, glyphColour);
            extractor.fill(cx + 3, cy - 1, cx + 6, cy + 1, glyphColour);
            extractor.fill(cx - 1, cy - 1, cx + 1, cy + 1, glyphColour);
        }
    }
}
