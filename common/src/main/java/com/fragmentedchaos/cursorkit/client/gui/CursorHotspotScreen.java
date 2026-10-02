package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.client.SystemCursorScreen;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.client.render.CursorGeometry;
import com.fragmentedchaos.cursorkit.client.render.CursorTextures;
import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Editor for one cursor set's click points, opened from the picker.
 * <p>
 * Picking a click point by clicking a tiny 20 pixel preview turned out to be too fiddly, so this
 * screen shows one state's image blown up with a pixel grid: click or drag anywhere inside it to
 * place the click point, use the arrow keys to nudge it by one pixel, and "Center" to jump to the
 * middle. Left is the list of states with their current value; states the player changed are shown
 * in gold and "Reset" hands them back to the cursor set's own JSON.
 * <p>
 * Changes are applied live - the real cursor follows while editing - and written to
 * {@code config/cursorkit.json} when the screen is closed with "Done".
 * <p>
 * While this screen is open the mod hands the window's own cursor back (see
 * {@link com.fragmentedchaos.cursorkit.client.SystemCursorScreen}): the custom cursor would cover
 * the very pixel the player is about to click.
 */
public class CursorHotspotScreen extends Screen
        implements com.fragmentedchaos.cursorkit.client.SystemCursorScreen {

    private static final int MARGIN = 12;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_BUTTON_WIDTH = 120;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_WIDTH = 170;
    private static final int TOOL_BUTTON_WIDTH = 100;
    /** How large the preview may become, in GUI units. */
    private static final int MAX_PREVIEW = 180;

    /**
     * Mouse button number of a left click.
     * <p>
     * Minecraft reports 1 for the left button (see {@code AbstractWidget.isValidClickButton}), not 0
     * - comparing against 0 silently ignored every real left click in here.
     */
    private static final int LEFT_BUTTON = 1;

    private final @Nullable Screen parent;
    private final String setId;
    /** Set as it looked when the screen opened, only used if the manager no longer has it. */
    private final CursorSet fallbackSet;
    private final CursorConfig draft;
    private final CursorConfig original;

    private @Nullable StateList list;
    private CursorState state;
    private boolean dragging;
    private int seenGeneration = -1;
    /** True while an edit has not been written to disk yet. */
    private boolean dirty;

    public CursorHotspotScreen(@Nullable Screen parent, CursorSet set, CursorConfig draft,
                               CursorConfig original) {
        super(CursorTranslations.get("cursorkit.hotspot.title", "Edit click point"));
        this.parent = parent;
        this.setId = set.id();
        this.fallbackSet = set;
        this.draft = draft;
        this.original = original;
        this.state = firstState();
    }

    /**
     * The edited set, re-read from the manager on every use.
     * <p>
     * Editing replaces the loaded set (the config override is applied to a fresh copy), so holding
     * on to the instance from when the screen opened would mean drawing a frozen preview and the
     * edits would look like they did nothing.
     */
    private CursorSet set() {
        for (CursorSet candidate : CursorManager.get().sets()) {
            if (candidate.id().equals(this.setId)) {
                return candidate;
            }
        }
        return this.fallbackSet;
    }

    @Override
    protected void init() {
        int listHeight = this.height - HEADER_HEIGHT - 34;
        this.list = new StateList(this.minecraft, LIST_WIDTH, listHeight, HEADER_HEIGHT, ROW_HEIGHT);
        this.list.updateSizeAndPosition(LIST_WIDTH, listHeight, MARGIN, HEADER_HEIGHT);
        addRenderableWidget(this.list);
        this.list.populate();

        // Center and Reset live under the preview: at small window sizes the footer cannot hold
        // four buttons without them overlapping.
        int toolsY = previewTop() + previewSize() + 47;
        addRenderableWidget(new FlatButton(previewLeft(), toolsY, TOOL_BUTTON_WIDTH, 20,
                CursorTranslations.get("cursorkit.hotspot.center", "Center"),
                button -> place(frameSize() / 2, frameSize() / 2)));
        addRenderableWidget(new FlatButton(previewLeft() + TOOL_BUTTON_WIDTH + 4, toolsY,
                TOOL_BUTTON_WIDTH, 20,
                CursorTranslations.get("cursorkit.hotspot.reset", "Reset"),
                button -> reset()));
        int y = this.height - 26;
        addRenderableWidget(new FlatButton(this.width - 2 * FOOTER_BUTTON_WIDTH - 16, y,
                FOOTER_BUTTON_WIDTH, 20,
                CursorTranslations.get("cursorkit.button.done", "Done"),
                button -> onClose()));
        addRenderableWidget(new FlatButton(this.width - FOOTER_BUTTON_WIDTH - MARGIN, y,
                FOOTER_BUTTON_WIDTH, 20,
                CursorTranslations.get("cursorkit.button.cancel", "Cancel"),
                button -> cancel()));
    }

    // ---------------------------------------------------------------- editing

    /** @return the state whose click point is edited right now */
    CursorState state() {
        return this.state;
    }

    CursorImage image() {
        return set().image(this.state).orElse(null);
    }

    /** @return the state an edit changes: a fallback row really edits {@code default} */
    CursorState target() {
        return set().has(this.state) ? this.state : CursorState.DEFAULT;
    }

    int frameSize() {
        Identifier texture = texture();
        CursorImage image = image();
        return texture == null || image == null ? CursorGeometry.DEFAULT_FRAME_SIZE
                : CursorGeometry.of(texture, image).frameSize();
    }

    /**
     * Click point one state actually uses: the value from the JSON, else the one the image file
     * carries, else the top left corner.
     * <p>
     * Takes the state as a parameter because the list draws every row with its own value - using the
     * selected state's would print the same number on all of them.
     */
    int hotspotX(CursorState state) {
        CursorImage image = set().image(state).orElse(null);
        if (image == null) {
            return 0;
        }
        Identifier texture = CursorTextures.resolve(set(), image);
        return texture == null ? Math.max(0, image.hotspotX())
                : CursorGeometry.of(texture, image).hotspotX(image);
    }

    /** @see #hotspotX(CursorState) */
    int hotspotY(CursorState state) {
        CursorImage image = set().image(state).orElse(null);
        if (image == null) {
            return 0;
        }
        Identifier texture = CursorTextures.resolve(set(), image);
        return texture == null ? Math.max(0, image.hotspotY())
                : CursorGeometry.of(texture, image).hotspotY(image);
    }

    /** @return the click point of the state being edited */
    int hotspotX() {
        return hotspotX(this.state);
    }

    /** @see #hotspotX() */
    int hotspotY() {
        return hotspotY(this.state);
    }

    int frames() {
        Identifier texture = texture();
        CursorImage image = image();
        return texture == null || image == null ? 1 : CursorGeometry.of(texture, image).frames();
    }

    @Nullable Identifier texture() {
        CursorImage image = image();
        return image == null ? null : CursorTextures.resolve(set(), image);
    }

    void select(CursorState state) {
        this.state = state;
        if (this.list != null) {
            this.list.populate();
        }
    }

    private CursorState firstState() {
        for (CursorState candidate : CursorState.values()) {
            if (set().image(candidate).isPresent()) {
                return candidate;
            }
        }
        return CursorState.DEFAULT;
    }

    private void place(int x, int y) {
        int size = frameSize();
        int clampedX = Math.max(0, Math.min(size - 1, x));
        int clampedY = Math.max(0, Math.min(size - 1, y));
        this.draft.setHotspot(set().id(), target().id(), clampedX, clampedY);
        this.dirty = true;
        // Live: the cursor on screen follows the edit, the file is written a moment later.
        CursorManager.get().previewConfig(this.draft);
    }

    private void reset() {
        this.draft.clearHotspot(set().id(), target().id());
        this.dirty = true;
        CursorManager.get().previewConfig(this.draft);
        save();
        if (this.list != null) {
            this.list.populate();
        }
    }

    private void cancel() {
        CursorManager.get().applyConfig(this.original);
        this.dirty = false;
        onClose();
    }

    /** Writes the current click points to disk; called from every way out of this screen. */
    private void save() {
        if (!this.dirty) {
            return;
        }
        this.dirty = false;
        CursorManager.get().applyConfig(this.draft);
    }

    /**
     * Safety net: a screen that is replaced or closed never reports its edits otherwise, so a change
     * made with the arrow keys right before alt+F4 would be lost.
     */
    @Override
    public void removed() {
        save();
        super.removed();
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int step = switch (event.key()) {
            case 263 -> -1;   // left
            case 262 -> 1;    // right
            default -> 0;
        };
        int vertical = switch (event.key()) {
            case 265 -> -1;   // up
            case 264 -> 1;    // down
            default -> 0;
        };
        if (step != 0 || vertical != 0) {
            if (image() != null) {
                place(hotspotX() + step, hotspotY() + vertical);
                // A keystroke is a discrete step, so it is written right away.
                save();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    // ---------------------------------------------------------------- geometry

    /** Left edge of the big preview. */
    private int previewLeft() {
        return MARGIN * 2 + LIST_WIDTH;
    }

    /** Side length of the preview, in GUI units. */
    private int previewSize() {
        // 130px are kept for the three value lines and the Center/Reset row underneath.
        int available = Math.min(this.width - previewLeft() - MARGIN * 2,
                this.height - HEADER_HEIGHT - 130);
        return Math.max(48, Math.min(MAX_PREVIEW, available));
    }

    private int previewTop() {
        return HEADER_HEIGHT + 22;
    }

    private float previewScale() {
        return (float) previewSize() / frameSize();
    }

    private boolean insidePreview(double mouseX, double mouseY) {
        int left = previewLeft();
        int top = previewTop();
        int size = previewSize();
        return mouseX >= left && mouseX < left + size && mouseY >= top && mouseY < top + size;
    }

    private void placeFromMouse(double mouseX, double mouseY) {
        float scale = previewScale();
        place((int) ((mouseX - previewLeft()) / scale), (int) ((mouseY - previewTop()) / scale));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == LEFT_BUTTON && insidePreview(event.x(), event.y())) {
            this.dragging = true;
            placeFromMouse(event.x(), event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            placeFromMouse(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    /**
     * Keeps following the pointer while the button is held.
     * <p>
     * Not every environment delivers {@code mouseDragged} for a click that is held and moved (some
     * only report movement), so the drag is driven from both callbacks.
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        net.minecraft.client.MouseHandler mouse = net.minecraft.client.Minecraft.getInstance()
                .mouseHandler;
        if (this.dragging && mouse != null && mouse.isLeftPressed()) {
            placeFromMouse(mouseX, mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging) {
            this.dragging = false;
            // Written once per drag instead of on every mouse step, so a crash or a quick alt+F4
            // cannot lose the edit either.
            save();
            return true;
        }
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        // Values change while dragging, so the state list is rebuilt when the manager reports a new
        // generation - the same mechanism the picker uses.
        CursorManager manager = CursorManager.get();
        if (this.list != null && manager.generation() != this.seenGeneration) {
            this.seenGeneration = manager.generation();
            this.list.populate();
        }
        Component title = CursorTranslations.get("cursorkit.hotspot.title",
                        "Edit click point")
                .copy()
                .append(Component.literal("  —  " + set().name()));
        extractor.centeredText(this.font, title, this.width / 2, 14, 0xFFFFFFFF);

        CursorImage image = image();
        if (image == null) {
            return;
        }
        int left = previewLeft();
        int top = previewTop();
        int size = previewSize();
        int frameSize = frameSize();
        float scale = previewScale();

        extractor.fill(left - 1, top - 1, left + size + 1, top + size + 1, 0x66000000);
        extractor.text(this.font,
                CursorTranslations.get("cursorkit.hotspot.hint",
                        "Click or drag to place the click point"),
                left, top - 12, 0xFF8899AA);

        // One line per image pixel, so the value below is easy to hit exactly.
        if (scale >= 4.0F) {
            for (int pixel = 1; pixel < frameSize; pixel++) {
                int x = left + Math.round(pixel * scale);
                int y = top + Math.round(pixel * scale);
                extractor.fill(x, top, x + 1, top + size, 0x22FFFFFF);
                extractor.fill(left, y, left + size, y + 1, 0x22FFFFFF);
            }
        }

        Identifier texture = texture();
        if (texture != null) {
            int frame = Math.min(image.frameAt(System.nanoTime() / 1_000_000L), frames() - 1);
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                    left, top, (float) (frame * frameSize), 0.0F, size, size,
                    frameSize, frameSize, frameSize * frames(), frameSize);
        }

        int hotX = left + Math.round(hotspotX() * scale);
        int hotY = top + Math.round(hotspotY() * scale);
        extractor.fill(left, hotY, left + size, hotY + 1, 0xAAFF5555);
        extractor.fill(hotX, top, hotX + 1, top + size, 0xAAFF5555);
        extractor.fill(hotX - 2, hotY - 2, hotX + 3, hotY + 3, 0xFFFF5555);

        boolean edited = CursorManager.get().hasHotspotOverride(set().id(), target());
        int infoY = top + size + 8;
        extractor.text(this.font,
                CursorTranslations.get("cursorkit.hotspot.value",
                        "click point: %s,%s", hotspotX(), hotspotY()),
                left, infoY, edited ? 0xFFFFD479 : 0xFFDDDDDD);
        extractor.text(this.font,
                CursorTranslations.get("cursorkit.hotspot.frame",
                        "%sx%s per frame, %s frame(s)", frameSize, frameSize, frames()),
                left, infoY + 11, 0xFF8899AA);
        if (!set().has(this.state)) {
            extractor.text(this.font,
                    CursorTranslations.get("cursorkit.hotspot.fallback",
                            "This state borrows default's picture; its click point is its own"),
                    left, infoY + 22, 0xFF8899AA);
        }
    }

    /** The states of the set with their current click point. */
    private final class StateList extends ObjectSelectionList<StateList.Row> {

        StateList(net.minecraft.client.Minecraft minecraft, int width, int height, int y,
                  int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            this.centerListVertically = false;
        }

        void populate() {
            clearEntries();
            Row toSelect = null;
            for (CursorState candidate : CursorState.values()) {
                if (CursorHotspotScreen.this.set().image(candidate).isEmpty()) {
                    continue;
                }
                Row row = new Row(candidate);
                addEntry(row);
                if (candidate == CursorHotspotScreen.this.state) {
                    toSelect = row;
                }
            }
            if (toSelect != null) {
                setSelected(toSelect);
            }
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 12;
        }

        final class Row extends ObjectSelectionList.Entry<Row> {

            private final CursorState rowState;

            Row(CursorState state) {
                this.rowState = state;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                boolean active = this.rowState == CursorHotspotScreen.this.state;
                // The row stands for its own state, even when it borrows default's picture: a fallback
                // row must not be shown as edited just because default was.
                boolean edited = CursorManager.get()
                        .hasHotspotOverride(CursorHotspotScreen.this.set().id(), this.rowState);
                CursorImage image = CursorHotspotScreen.this.set().image(this.rowState).orElse(null);
                int colour = active ? 0xFFFFD479 : (hovered ? 0xFFFFFFFF : 0xFFBFBFBF);
                String label = (active ? "> " : "  ") + CursorTranslations.state(this.rowState).getString();
                if (!CursorHotspotScreen.this.set().has(this.rowState)) {
                    label += "  " + CursorTranslations.get("cursorkit.state.fallback",
                            "(default)").getString();
                }
                extractor.text(CursorHotspotScreen.this.font, Component.literal(label),
                        this.getContentX() + 2, this.getContentY() + 2, colour);
                extractor.text(CursorHotspotScreen.this.font,
                        Component.literal(image == null ? ""
                                : hotspotX(this.rowState) + "," + hotspotY(this.rowState)),
                        this.getContentX() + 2, this.getContentY() + 11,
                        edited ? 0xFFFFD479 : 0xFF8899AA);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                CursorHotspotScreen.this.select(this.rowState);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.rowState.id());
            }
        }
    }
}
