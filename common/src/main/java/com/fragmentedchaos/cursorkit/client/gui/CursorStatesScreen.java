package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSets;
import com.fragmentedchaos.cursorkit.client.render.CursorGeometry;
import com.fragmentedchaos.cursorkit.client.render.ClickEffectPainter;
import com.fragmentedchaos.cursorkit.client.CursorReloadListener;
import com.fragmentedchaos.cursorkit.client.render.CursorTextures;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for the cursor the player assembles by hand: one image path per state.
 * <p>
 * Every state gets a row with a path field, because that is the whole point of this set - a single
 * folder would not let a player mix an arrow from one place with an I-beam from another. The row
 * shows what the path currently amounts to: a preview of the image at the right, and the state name
 * at the left in gold when the file is there, red with a {@code ?} when it is not, grey while the
 * state is simply left out.
 * <p>
 * Live like the other editors: a path is applied as it is typed (a rescan of the config directory
 * only happens once the text leads somewhere), and "Done" writes the configuration. "Cancel" hands
 * the configuration back as it was when the screen opened.
 * <p>
 * It is a screen of its own rather than fields inside the picker's details panel: at GUI scale 3 a
 * small window leaves that panel around a hundred pixels tall, which is not enough for six rows.
 */
public class CursorStatesScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int PANEL_TOP = 10;
    private static final int TITLE_HEIGHT = 30;
    private static final int FIELD_HEIGHT = 18;
    private static final int LABEL_WIDTH = 76;
    private static final int PREVIEW_SIZE = 16;
    private static final int FOOTER_BUTTON_WIDTH = 120;
    /** Width of the effect colour field, and what the path field starts after. */
    private static final int COLOUR_WIDTH = 84;
    /** Size of the effect preview box in the corner, which no field may run under. */
    private static final int PREVIEW_BOX = 22;
    private static final int FOOTER_GAP = 8;
    /** Widest the panel ever gets: a path does not need a thousand pixel field. */
    private static final int MAX_PANEL_WIDTH = 460;
    private static final int MIN_ROW_PITCH = 18;
    private static final int MAX_ROW_PITCH = 26;

    private final @Nullable Screen parent;
    private final CursorConfig original;
    private final CursorConfig draft;
    private final List<EditBox> fields = new ArrayList<>();
    private @Nullable EditBox colourField;
    private @Nullable EditBox animationField;
    private @Nullable FlatButton effectButton;

    public CursorStatesScreen(@Nullable Screen parent, CursorConfig draft, CursorConfig original) {
        super(CursorTranslations.get("cursorkit.custom.title", "One image per state"));
        this.parent = parent;
        this.draft = draft;
        this.original = original;
    }

    // ------------------------------------------------------------------ geometry

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelWidth() {
        return Math.max(200, Math.min(this.width - 2 * MARGIN, MAX_PANEL_WIDTH));
    }

    private int panelBottom() {
        return this.height - 34;
    }

    /** @return vertical distance between two state rows, as tight as the window allows */
    private int rowPitch() {
        int rows = CursorState.values().length;
        int available = panelBottom() - rowsTop() - 62;
        return Math.max(MIN_ROW_PITCH, Math.min(MAX_ROW_PITCH, available / rows));
    }

    /** @return y of the first state row */
    private int rowsTop() {
        return PANEL_TOP + TITLE_HEIGHT;
    }

    /** @return y of the effect row, just under the state rows */
    private int effectRowY() {
        return Math.min(rowsTop() + CursorState.values().length * rowPitch() + 14,
                panelBottom() - 22);
    }

    private int fieldLeft() {
        return panelLeft() + 8 + LABEL_WIDTH;
    }

    private int fieldWidth() {
        return panelWidth() - 16 - LABEL_WIDTH - PREVIEW_SIZE - 6;
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        this.fields.clear();
        int rowY = rowsTop();
        for (CursorState state : CursorState.values()) {
            EditBox field = new EditBox(this.font, fieldLeft(), rowY, fieldWidth(), FIELD_HEIGHT,
                    Component.literal(state.id()));
            field.setMaxLength(512);
            field.setValue(this.draft.customState(state.id()));
            field.setHint(CursorTranslations.get("cursorkit.custom.field", "image file"));
            field.setResponder(value -> {
                this.draft.setCustomState(state.id(), value);
                applyLive();
            });
            addRenderableWidget(field);
            this.fields.add(field);
            rowY += rowPitch();
        }

        // The effect this hand-made set plays: a type, a colour for the built-in shapes, and - for
        // the image type - the animation strip the player points at.
        int effectY = effectRowY();
        this.effectButton = new FlatButton(MARGIN, effectY, 150, 18,
                CursorTranslations.get("cursorkit.effect.label", "Effect: %s",
                        effectLabel(this.draft.customEffect())),
                button -> {
                    this.draft.setCustomEffect(nextEffect(this.draft.customEffect()));
                    applyLive();
                    rebuildWidgets();
                });
        addRenderableWidget(this.effectButton);

        ClickEffect current = this.draft.customEffect();
        this.colourField = new EditBox(this.font, MARGIN + 154, effectY, COLOUR_WIDTH, 18,
                CursorTranslations.get("cursorkit.effect.colour", "colour"));
        this.colourField.setMaxLength(7);
        this.colourField.setValue(String.format("#%06X", current.color() & 0xFFFFFF));
        this.colourField.setHint(CursorTranslations.get("cursorkit.effect.colour", "colour"));
        this.colourField.setResponder(value -> {
            Integer colour = parseColour(value);
            if (colour != null) {
                ClickEffect effect = this.draft.customEffect();
                this.draft.setCustomEffect(new ClickEffect(effect.type(), colour, effect.radius(),
                        effect.durationMs(), effect.particles(), effect.texture(), effect.frames(),
                        effect.frameMs(), effect.size()));
                applyLive();
            }
        });
        // Always on screen: hiding it made it look like colours could not be changed at all. An
        // animation ignores it (a texture cannot be tinted), which the hint line says.
        this.colourField.visible = true;
        addRenderableWidget(this.colourField);

        // Right next to the controls that are actually on screen: with the colour field hidden there
        // is no reason to leave its space empty and push the path out to the window edge.
        int pathLeft = MARGIN + 154 + COLOUR_WIDTH + 4;
        this.animationField = new EditBox(this.font, pathLeft, effectY,
                Math.max(60, this.width - MARGIN - PREVIEW_BOX - 6 - pathLeft), 18,
                CursorTranslations.get("cursorkit.effect.animation", "animation file"));
        this.animationField.setMaxLength(512);
        this.animationField.setValue(current.texture());
        this.animationField.setHint(CursorTranslations.get("cursorkit.effect.animation",
                "animation file"));
        this.animationField.setResponder(value -> {
            ClickEffect effect = this.draft.customEffect();
            this.draft.setCustomEffect(new ClickEffect(effect.type(), effect.color(),
                    effect.radius(), effect.durationMs(), effect.particles(), value.trim(),
                    effect.frames(), effect.frameMs(), effect.size()));
            applyLive();
        });
        this.animationField.visible = current.isImage();
        addRenderableWidget(this.animationField);

        int buttonY = this.height - 26;
        // Centred pair: buttonX() takes a button's index in a row, its second argument is not a
        // count - passing one there pushed both buttons off the right edge.
        int rowWidth = FOOTER_BUTTON_WIDTH * 2 + FOOTER_GAP;
        int doneX = Math.max(MARGIN, (this.width - rowWidth) / 2);
        addRenderableWidget(new FlatButton(doneX, buttonY, FOOTER_BUTTON_WIDTH, 20,
                CursorTranslations.get("cursorkit.button.done", "Done"),
                button -> {
                    applyToDisk();
                    onClose();
                }));
        addRenderableWidget(new FlatButton(doneX + FOOTER_BUTTON_WIDTH + FOOTER_GAP, buttonY,
                FOOTER_BUTTON_WIDTH, 20,
                CursorTranslations.get("cursorkit.button.cancel", "Cancel"),
                button -> cancel()));
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int left = panelLeft();
        int right = left + panelWidth();
        extractor.fill(left, PANEL_TOP, right, panelBottom(), 0x66000000);
        extractor.text(this.font,
                CursorTranslations.get("cursorkit.custom.title", "One image per state"),
                left + 8, PANEL_TOP + 8, 0xFFFFFFFF);
        extractor.text(this.font,
                CursorTranslations.get("cursorkit.custom.hint", "each path points at a PNG or .cur"),
                left + 8, PANEL_TOP + 20, 0xFF8899AA);

        Path base = CursorManager.get().gameDirectory();
        int rowY = rowsTop();
        for (CursorState state : CursorState.values()) {
            String configured = this.draft.customState(state.id());
            Path file = CursorSets.resolve(configured, base);
            boolean set = !configured.isBlank();
            boolean found = set && file != null && Files.isRegularFile(file);
            // Grey: nothing set, gold: the file is there, red with a "?": it is not.
            int colour = !set ? 0xFF8899AA : (found ? 0xFFFFD479 : 0xFFFF7777);
            extractor.text(this.font,
                    Component.literal(state.id() + (set && !found ? " ?" : "")),
                    left + 8, rowY + 5, colour);
            drawPreview(extractor, state, found, right - 8 - PREVIEW_SIZE, rowY);
            rowY += rowPitch();
        }

        extractor.text(this.font,
                CursorTranslations.get("cursorkit.custom.hint2",
                        "Empty states fall back to the default image."),
                left + 8, Math.max(rowsTop(), Math.min(rowY + 4, effectRowY() - 11)), 0xFF8899AA);

        if (!CursorManager.get().config().clickEffect()) {
            // Nothing here will be visible while the master switch is off, and there is no way to
            // tell that from this screen - it lives in the picker's switch row.
            extractor.text(this.font,
                    CursorTranslations.get("cursorkit.effect.master_off",
                            "Click effects are switched off in the picker (bottom switch)"),
                    MARGIN, effectRowY() + 20, 0xFFFF7777);
        }

        // The effect row's own preview, playing on its own next to its controls.
        ClickEffect effect = this.draft.customEffect();
        int effectY = effectRowY();
        int boxX = this.width - MARGIN - 26;
        extractor.fill(boxX, effectY, boxX + PREVIEW_BOX, effectY + 18, 0x44000000);
        // Built-in shapes only: an animation is a texture, and while the player is still typing the
        // path there is nothing registered to draw - the missing texture looks worse than no preview.
        if (!effect.disabled() && !effect.isImage()) {
            long now = System.nanoTime() / 1_000_000L;
            long duration = Math.max(1L, effect.durationMs());
            float progress = (float) (now % duration) / duration;
            ClickEffect preview = new ClickEffect(effect.type(), effect.color(),
                    Math.min(effect.radius(), 9.0F), effect.durationMs(), effect.particles(),
                    effect.texture(), effect.frames(), effect.frameMs(),
                    Math.min(effect.size(), 20));
            CursorSet live = CursorManager.get().sets().stream()
                    .filter(set -> set.id().equals(CursorSets.CUSTOM_ID)).findFirst().orElse(null);
            ClickEffectPainter.paint(extractor, live, preview, boxX + PREVIEW_BOX / 2.0F,
                    effectY + 9, progress,
                    this.minecraft == null ? 1 : this.minecraft.getWindow().getGuiScale());
        }
    }

    /**
     * Draws the image a state currently points at, at the far right of its row.
     * <p>
     * Taken from the registered texture instead of reading the file again: paths are applied as they
     * are typed, so what is registered is exactly what the cursor is being drawn with.
     */
    private void drawPreview(GuiGraphicsExtractor extractor, CursorState state, boolean found,
                             int x, int rowY) {
        if (!found) {
            return;
        }
        CursorSet live = CursorManager.get().sets().stream()
                .filter(set -> set.id().equals(CursorSets.CUSTOM_ID))
                .findFirst().orElse(null);
        if (live == null || !live.has(state)) {
            return;
        }
        CursorImage image = live.image(state).orElse(null);
        if (image == null) {
            return;
        }
        Identifier texture = CursorTextures.resolve(live, image);
        if (texture == null) {
            return;
        }
        CursorGeometry geometry = CursorGeometry.of(texture, image);
        int frame = Math.min(this.draft.animate()
                ? image.frameAt(System.nanoTime() / 1_000_000L) : 0, geometry.frames() - 1);
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, rowY + 1,
                (float) (frame * geometry.frameSize()), 0.0F,
                PREVIEW_SIZE, PREVIEW_SIZE,
                geometry.frameSize(), geometry.frameSize(),
                geometry.frameSize() * geometry.frames(), geometry.frameSize());
    }

    /**
     * The label of one effect type, translated.
     * <p>
     * The animation route reads "custom" here rather than "pack animation": in this editor it is the
     * player's own file, and the field next to the button is where its path goes.
     */
    static Component effectLabel(ClickEffect effect) {
        if (effect.isImage()) {
            return CursorTranslations.get("cursorkit.effect.custom", "custom");
        }
        return CursorTranslations.get("cursorkit.effect." + effect.type(), effect.type());
    }

    /** Cycles through the effects a hand-made set can use; its own animation needs a pack. */
    public static ClickEffect nextEffect(ClickEffect current) {
        String[] order = {"ripple", "burst", "pulse", "image", "none"};
        int index = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(current.type())) {
                index = i;
                break;
            }
        }
        String next = order[(index + 1) % order.length];
        if ("none".equals(next)) {
            return ClickEffect.NONE;
        }
        ClickEffect base = ClickEffect.DEFAULT;
        if ("image".equals(next)) {
            // The pack's own animation route, for a set the player builds by hand: eight frames at
            // 50ms is a sensible starting point, the path is what they fill in.
            return new ClickEffect(next, current.color(), base.radius(), 8 * 50L, base.particles(),
                    current.isImage() ? current.texture() : "", 8, 50, 48);
        }
        return new ClickEffect(next, current.color(), base.radius(), base.durationMs(),
                base.particles(), "", 1, 60, base.size());
    }

    /** @return the colour in {@code #RRGGBB}, or {@code null} while the text is not one yet */
    public static @Nullable Integer parseColour(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(text, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ actions

    /** Applies the paths in memory and rebuilds the set, without writing the file. */
    private void applyLive() {
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        if (directory == null) {
            return;
        }
        CursorManager manager = CursorManager.get();
        manager.previewConfig(this.draft);
        manager.rescan(directory);
        CursorReloadListener.rescanTextures(this.minecraft, directory);
    }

    /** Writes the paths to disk; the picker's Done does the same. */
    private void applyToDisk() {
        CursorManager manager = CursorManager.get();
        manager.applyConfig(this.draft);
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        if (directory != null) {
            manager.rescan(directory);
            CursorReloadListener.rescanTextures(this.minecraft, directory);
        }
    }

    private void cancel() {
        CursorManager manager = CursorManager.get();
        manager.applyConfig(this.original);
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        if (directory != null) {
            manager.rescan(directory);
            CursorReloadListener.rescanTextures(this.minecraft, directory);
        }
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreenAndShow(this.parent);
        }
    }
}
