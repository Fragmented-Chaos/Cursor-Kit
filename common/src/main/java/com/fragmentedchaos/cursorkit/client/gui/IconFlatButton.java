package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.client.render.CursorIcon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The flat button with the mod's arrow icon on its left.
 * <p>
 * Used for the cursor entry on screens that replace the video settings screen but are not Sodium
 * (Embeddium and friends), where the picker's own flat style fits better than a vanilla row. The other
 * flat buttons - the picker's own controls - keep {@link FlatButton} without an icon.
 */
public class IconFlatButton extends FlatButton {

    /** Distance from the left edge to the icon. */
    private static final int INSET = 5;

    public IconFlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        CursorIcon.draw(extractor, getX() + INSET, getY() + (getHeight() - CursorIcon.SIZE) / 2);
    }
}
