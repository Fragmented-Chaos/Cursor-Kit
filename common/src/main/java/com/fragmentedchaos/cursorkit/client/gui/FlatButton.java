package com.fragmentedchaos.cursorkit.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A button in the same flat, dark style as the rest of the picker, instead of vanilla's raised grey
 * sprite: a translucent panel fill, a hairline border that brightens on hover, and centred text.
 * <p>
 * Keeps everything else about a button - press handling, narration, focus and the cursor request -
 * because it is still a {@link Button}, only the drawing is replaced.
 */
public class FlatButton extends Button {

    private static final int FILL = 0x99000000;
    private static final int FILL_HOVERED = 0xBB1E1E1E;
    private static final int FILL_PRESSED = 0xCC2E2E2E;
    private static final int FILL_DISABLED = 0x55000000;
    private static final int BORDER = 0x38FFFFFF;
    private static final int BORDER_HOVERED = 0x90FFFFFF;
    private static final int BORDER_DISABLED = 0x18FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DISABLED = 0xFF8A8A8A;

    /** Corner radius in GUI units. */
    private static final int RADIUS = 4;

    public FlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
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
        int text;
        if (!this.active) {
            fill = FILL_DISABLED;
            border = BORDER_DISABLED;
            text = TEXT_DISABLED;
        } else if (hovered && isPressed()) {
            fill = FILL_PRESSED;
            border = BORDER_HOVERED;
            text = TEXT;
        } else if (hovered) {
            fill = FILL_HOVERED;
            border = BORDER_HOVERED;
            text = TEXT;
        } else {
            fill = FILL;
            border = BORDER;
            text = TEXT;
        }

        // Two passes: the border as a rounded rectangle, the fill as a slightly smaller one inside.
        roundedRect(extractor, x, y, width, height, RADIUS, border);
        roundedRect(extractor, x + 1, y + 1, width - 2, height - 2, RADIUS - 1, fill);

        extractor.enableScissor(x + 2, y, x + width - 2, y + height);
        extractor.centeredText(Minecraft.getInstance().font, getMessage(), x + width / 2,
                y + (height - 8) / 2, text);
        extractor.disableScissor();
    }

    /**
     * Draws a filled rectangle with rounded corners.
     * <p>
     * The extractor only knows plain rectangles, so each row is drawn with the horizontal inset a
     * circle of {@code radius} needs at that height - a handful of rows, so it costs nothing.
     */
    static void roundedRect(GuiGraphicsExtractor extractor, int x, int y, int width, int height,
                            int radius, int colour) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            extractor.fill(x, y, x + width, y + height, colour);
            return;
        }
        for (int row = 0; row < height; row++) {
            int inset = cornerInset(row, height, r);
            extractor.fill(x + inset, y + row, x + width - inset, y + row + 1, colour);
        }
    }

    /** @return how far the row is inset from the corners of a rounded rectangle */
    private static int cornerInset(int row, int height, int radius) {
        int fromEdge = Math.min(row, height - 1 - row);
        if (fromEdge >= radius) {
            return 0;
        }
        double dy = radius - fromEdge - 0.5D;
        double dx = Math.sqrt(Math.max(0.0D, radius * (double) radius - dy * dy));
        return (int) Math.round(radius - dx);
    }

    private boolean isPressed() {
        return Minecraft.getInstance().mouseHandler != null
                && Minecraft.getInstance().mouseHandler.isLeftPressed();
    }
}
