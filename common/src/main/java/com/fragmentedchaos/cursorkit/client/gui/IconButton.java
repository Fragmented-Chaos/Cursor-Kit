package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.client.render.CursorIcon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A vanilla button with the mod's arrow icon on its left.
 * <p>
 * This is the entry row in the vanilla video settings screen - the screen NeoForge and a plain Fabric
 * install use. Drawing the vanilla sprite and label through the inherited helpers keeps the row looking
 * like the options around it, and the icon is the same one Sodium shows next to the cursor page, so the
 * entry is recognisable whichever entry point the game ended up with.
 */
public class IconButton extends Button {

    /** Distance from the left edge to the icon. */
    private static final int INSET = 5;

    public IconButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        extractDefaultSprite(extractor);
        extractDefaultLabel(extractor.textRendererForWidget(this,
                GuiGraphicsExtractor.HoveredTextEffects.NONE));
        CursorIcon.draw(extractor, getX() + INSET, getY() + (getHeight() - CursorIcon.SIZE) / 2);
    }
}
