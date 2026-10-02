package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSets;
import com.fragmentedchaos.cursorkit.client.render.ClickEffectPainter;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.render.CursorGeometry;
import com.fragmentedchaos.cursorkit.client.CursorReloadListener;
import com.fragmentedchaos.cursorkit.client.CursorWatcher;
import com.fragmentedchaos.cursorkit.client.render.CursorTextures;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.config.CursorHotspots;
import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import com.fragmentedchaos.cursorkit.cursor.load.CursorPackInstaller;
import com.fragmentedchaos.cursorkit.cursor.load.CursorPackLoader;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSetOrigin;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cursor picker, modelled after a shader pack screen: a scrollable, searchable list of cursor sets
 * on the left, details (including a six state preview with hotspot markers) on the right, and
 * switches along the bottom.
 * <p>
 * Selecting a row applies that set immediately; "Done" keeps it and "Cancel" restores the
 * configuration that was active when the screen was opened.
 */
public class CursorKitScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int HEADER_HEIGHT = 36;

    /** Size of the click effect preview box in the details panel. */
    private static final int EFFECT_BOX = 30;

    /** How long a status line stays on screen after a drop. */
    private static final long STATUS_MS = 6_000L;

    /** Height of one state row in the hand-made set's editor. */
    private static final int STATE_ROW_HEIGHT = 20;

    /** Width of the state name column in that editor; the path field takes the rest. */
    private static final int STATE_LABEL_WIDTH = 62;
    private static final int FOOTER_HEIGHT = 32;
    private static final int SEARCH_HEIGHT = 18;
    private static final int PREVIEW_ICON_SIZE = 20;
    private static final int PREVIEW_ROW_HEIGHT = 22;
    /** Smallest a preview row may shrink to on a short window; below this the labels would not fit. */
    private static final int MIN_PREVIEW_ROW_HEIGHT = 13;
    /** Height the footer (switches, done/cancel) needs, so the previews never run into it. */
    private static final int FOOTER_RESERVED = 56;
    private static final int SWITCH_GAP = 4;
    private static final int FOOTER_BUTTON_WIDTH = 120;
    private static final int FOOTER_GAP = 4;
    /** Y of the tool buttons in the header, so they line up with the title. */
    private static final int TOOL_Y = 6;
    /** Side of the square tool buttons in the header. */
    private static final int TOOL_SIZE = 18;
    private static final int[] EDGE_MARGINS = {0, 1, 2, 4, 8};
    private static final int MAX_SCALE = 3;

    private final Screen parent;
    private final CursorConfig original;
    private final CursorConfig draft;

    private @Nullable SetList list;
    private @Nullable EditBox search;
    private @Nullable Component statusMessage;
    private boolean statusProblem;
    private long statusUntil;
    private @Nullable CursorSet selected;
    private boolean systemSelected;
    private int seenGeneration;

    public CursorKitScreen(@Nullable Screen parent) {
        super(CursorTranslations.get("cursorkit.screen.title", "Cursor Kit"));
        this.parent = parent;
        CursorManager manager = CursorManager.get();
        this.original = manager.config().copy();
        this.draft = manager.config().copy();
        this.selected = manager.findSelected().orElse(null);
        this.systemSelected = manager.isSystemSelected();
    }

    @Override
    protected void init() {
        CursorManager manager = CursorManager.get();
        List<CursorSet> sets = manager.sets();
        this.seenGeneration = manager.generation();
        // The editor screen writes the hand-made set's paths and its effect straight to disk, and
        // this screen's draft is a copy from before that: without picking the values up again, the
        // next save from here would quietly put the old ones back.
        CursorConfig saved = manager.config();
        for (CursorState state : CursorState.values()) {
            this.draft.setCustomState(state.id(), saved.customState(state.id()));
        }
        this.draft.setCustomEffect(saved.customEffect());
        this.draft.setClickEffect(saved.clickEffect());
        this.draft.setAnimate(saved.animate());
        // Another screen (the click point editor) may have replaced the selected set since this
        // screen captured it, and the generation counter alone would not notice.
        if (this.selected != null) {
            for (CursorSet set : sets) {
                if (set.id().equals(this.selected.id())) {
                    this.selected = set;
                    break;
                }
            }
        }

        int listWidth = listWidth();
        int listHeight = listHeight();
        // AbstractSelectionList's constructor is (minecraft, width, height, y, itemHeight) and
        // updateSizeAndPosition takes (width, height, x, y) - not (x, y, width, height).
        this.list = new SetList(this.minecraft, listWidth, listHeight, HEADER_HEIGHT, 22);
        this.list.updateSizeAndPosition(listWidth, listHeight, MARGIN, HEADER_HEIGHT);
        addRenderableWidget(this.list);
        this.list.populate(sets, "");

        // The search field sits under the list (like a shader pack screen) so the header stays free
        // for the title.
        this.search = new EditBox(this.font, MARGIN, searchBoxY(), listWidth, SEARCH_HEIGHT,
                CursorTranslations.get("cursorkit.search", "Search"));
        this.search.setHint(CursorTranslations.get("cursorkit.search", "Search"));
        this.search.setResponder(value -> {
            if (this.list != null) {
                this.list.populate(CursorManager.get().sets(), value);
            }
        });
        addRenderableWidget(this.search);

        int switchY = switchRowY();
        int x = MARGIN;
        x = addSwitch(x, switchY, "cursorkit.switch.animate", "Animate: %s",
                onOff(this.draft.animate()),
                () -> this.draft.setAnimate(!this.draft.animate()));
        x = addSwitch(x, switchY, "cursorkit.switch.click", "Click: %s",
                onOff(this.draft.clickEffect()),
                () -> this.draft.setClickEffect(!this.draft.clickEffect()));
        x = addSwitch(x, switchY, "cursorkit.switch.scale", "Scale: %s",
                Component.literal(this.draft.scale() + "x"),
                () -> this.draft.setScale(this.draft.scale() >= MAX_SCALE ? 1 : this.draft.scale() + 1));
        addSwitch(x, switchY, "cursorkit.switch.edge", "Edge: %s",
                Component.literal(this.draft.edgeMargin() + "px"),
                () -> this.draft.setEdgeMargin(nextMargin(this.draft.edgeMargin())));

        addStateEditorButton();

        // Tools sit next to the title in the top right corner, so the bottom row only carries the
        // two dialogs' buttons.
        // Two small square buttons: a glyph reads faster than a label here, and it cannot be cut off
        // by a translation that happens to be long. The words live in the tooltips.
        ToolButton hotspots = new ToolButton(this.width - MARGIN - TOOL_SIZE, TOOL_Y, TOOL_SIZE,
                ToolButton.Glyph.HOTSPOT,
                CursorTranslations.get("cursorkit.button.hotspots", "Hotspots…"),
                button -> openHotspotEditor());
        hotspots.active = !this.systemSelected && this.selected != null;
        addRenderableWidget(hotspots);

        addRenderableWidget(new ToolButton(this.width - MARGIN - TOOL_SIZE * 2 - FOOTER_GAP, TOOL_Y,
                TOOL_SIZE, ToolButton.Glyph.FOLDER,
                CursorTranslations.get("cursorkit.button.folder", "Folder"),
                button -> openConfigFolder()));

        addRenderableWidget(new ToolButton(this.width - MARGIN - TOOL_SIZE * 3 - FOOTER_GAP * 2, TOOL_Y,
                TOOL_SIZE, ToolButton.Glyph.ABOUT,
                CursorTranslations.get("cursorkit.button.about", "About"),
                button -> Minecraft.getInstance().setScreenAndShow(new AboutScreen(this))));

        // Done and Cancel share the bottom row.
        int footerWidth = ScreenLayout.buttonWidth(this.width - MARGIN * 2, 2, FOOTER_GAP);
        addRenderableWidget(new FlatButton(MARGIN, this.height - 26, footerWidth, 20,
                CursorTranslations.get("cursorkit.button.done", "Done"),
                button -> {
                    applyCustomStatesToDisk();
                    onClose();
                }));
        addRenderableWidget(new FlatButton(
                ScreenLayout.buttonX(MARGIN, 1, footerWidth, FOOTER_GAP), this.height - 26,
                footerWidth, 20,
                CursorTranslations.get("cursorkit.button.cancel", "Cancel"),
                button -> cancel()));
    }

    /**
     * Follows the set list while the screen is open: {@link CursorWatcher} re-reads
     * {@code config/cursorkit/} whenever it changes on disk, so a dropped in cursor set or pack shows
     * up here within a second - without losing the search text or the current selection.
     */
    @Override
    public void tick() {
        super.tick();
        if (this.list == null) {
            return;
        }
        CursorManager manager = CursorManager.get();
        if (manager.generation() == this.seenGeneration) {
            return;
        }
        this.seenGeneration = manager.generation();

        // Point the details panel at the freshly read instance of the same set, if it is still there.
        if (this.selected != null) {
            CursorSet replacement = null;
            for (CursorSet set : manager.sets()) {
                if (set.id().equals(this.selected.id())) {
                    replacement = set;
                    break;
                }
            }
            if (replacement != null) {
                this.selected = replacement;
            } else if (!this.selected.id().equals(com.fragmentedchaos.cursorkit.cursor.load.CursorSets.CUSTOM_ID)) {
                // The set was removed while the screen was open. Keeping the stale instance would show
                // the details of something that no longer exists - and its textures have just been
                // released, so the previews would be missing-texture blocks. The mod already handed
                // the cursor back to the system, so the panel has to follow it there.
                this.selected = null;
                this.systemSelected = true;
            }
        }
        this.list.populate(manager.sets(), this.search == null ? "" : this.search.getValue());
    }

    /** GUI space centre of the "open config folder" button; the debug aid parks the pointer there. */
    public static int debugFolderButtonCenterX() {
        return MARGIN + FOOTER_BUTTON_WIDTH / 2;
    }

    /** @see #debugFolderButtonCenterX() */
    public static int debugFolderButtonCenterY(int guiHeight) {
        return guiHeight - 26 + 10;
    }

    private int listWidth() {
        return Math.max(120, (int) ((this.width - MARGIN * 3) * 0.45F));
    }

    private int switchRowY() {
        return this.height - FOOTER_HEIGHT - 22;
    }

    private int searchBoxY() {
        return switchRowY() - 6 - SEARCH_HEIGHT;
    }

    private int listHeight() {
        return Math.max(40, searchBoxY() - 6 - HEADER_HEIGHT);
    }

    /** Every switch is the same width so the row lines up with the screen margins. */
    /** Width of the two tool buttons in the top right corner; they shrink in narrow windows. */

    private int switchWidth() {
        // Four switches: animation, click feedback, scale and the edge margin.
        return Math.min(160, ScreenLayout.buttonWidth(this.width - MARGIN * 2, 4, SWITCH_GAP));
    }

    private int addSwitch(int x, int y, String key, String fallback, Component value,
                          Runnable action) {
        Component label = CursorTranslations.get(key, fallback, value);
        // No tooltips here: a tooltip next to a control at the window edge covers the row above it,
        // which looked like a stray black box. The labels are self explanatory.
        addRenderableWidget(new FlatButton(x, y, switchWidth(), 20, label, ignored -> {
            action.run();
            CursorManager.get().applyConfig(this.draft);
            rebuildWidgets();
        }));
        return x + switchWidth() + SWITCH_GAP;
    }

    /** Translated name of a cursor set's origin, for the list and the details panel. */
    private static Component originLabel(CursorSetOrigin origin) {
        return switch (origin) {
            case RESOURCE_PACK -> CursorTranslations.get("cursorkit.origin.pack",
                    "resource pack");
            case CONFIG_PACK -> CursorTranslations.get("cursorkit.origin.config_pack",
                    "config pack");
            case CONFIG -> CursorTranslations.get("cursorkit.origin.config", "config");
            case CUSTOM_STATES -> CursorTranslations.get("cursorkit.origin.custom", "custom");
        };
    }

    /** The name to show for a set: the hand-made one has a translated name, everything else its own. */
    private static Component displayName(CursorSet set) {
        if (set.id().equals(CursorSets.CUSTOM_ID)) {
            return CursorTranslations.get("cursorkit.custom.name", "Custom (per state)");
        }
        return Component.literal(set.name());
    }

    /**
     * Shortens a component until it fits {@code maxWidth}, appending an ellipsis.
     * <p>
     * Long entries are the norm here - a set's id, its source path and its click effect - and in
     * English they are longer still, so every line that has a box to live in goes through this. That
     * also keeps them from running under the effect preview in the corner.
     */
    private Component fit(Component text, int maxWidth) {
        String plain = text.getString();
        if (maxWidth <= 0) {
            return Component.literal("");
        }
        if (this.font.width(plain) <= maxWidth) {
            return text;
        }
        String cut = this.font.plainSubstrByWidth(plain, Math.max(0, maxWidth - this.font.width("...")));
        return Component.literal(cut + "...");
    }

    /** Translated name of a click effect, for the list and the details panel. */
    private static Component effectLabel(ClickEffect effect) {
        return CursorTranslations.get("cursorkit.effect." + effect.type(),
                effect.type());
    }

    private static Component onOff(boolean value) {
        return value
                ? CursorTranslations.get("cursorkit.value.on", "ON")
                : CursorTranslations.get("cursorkit.value.off", "OFF");
    }

    private static int nextMargin(int current) {
        for (int candidate : EDGE_MARGINS) {
            if (candidate > current) {
                return candidate;
            }
        }
        return EDGE_MARGINS[0];
    }

    void chooseSystemCursor() {
        this.systemSelected = true;
        this.selected = null;
        this.draft.setSelectedSet(CursorManager.SYSTEM_SELECTION);
        CursorManager.get().selectSystem();
        CursorManager.get().applyConfig(this.draft);
        rebuildWidgets();
    }

    void choose(CursorSet set) {
        this.systemSelected = false;
        this.selected = set;
        this.draft.setSelectedSet(set.id());
        CursorManager.get().select(set.id());
        CursorManager.get().applyConfig(this.draft);
        rebuildWidgets();
    }

    private void cancel() {
        // Click points were edited in their own screen and confirmed with "Done" there, so they are
        // not part of what this screen's "Cancel" takes back.
        CursorManager.get().applyConfig(CursorHotspots.mergedInto(this.original, this.draft));
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        if (directory != null) {
            CursorManager.get().rescan(directory);
            CursorReloadListener.rescanTextures(this.minecraft, directory);
        }
        onClose();
    }

    /**
     * The one control the hand-made set needs inside the picker: a button into its editor.
     * <p>
     * The six path fields live on that screen instead of here - at GUI scale 3 in a small window
     * this details panel is barely a hundred pixels tall, which cannot hold six rows.
     */
    private void addStateEditorButton() {
        int panelLeft = MARGIN * 2 + listWidth();
        int panelWidth = Math.max(80, this.width - MARGIN - panelLeft - 12);
        int broken = brokenPathCount();
        Component label = CursorTranslations.get("cursorkit.custom.edit", "Edit paths...");
        if (broken > 0) {
            // The details panel has no room for another line, so the warning rides on the button.
            label = label.copy().append(CursorTranslations.get("cursorkit.custom.edit_broken",
                    "  (%s not found)", broken));
        } else if (this.selected != null && this.selected.images().isEmpty()) {
            // Nothing picked yet: while the set is selected the mod deliberately leaves the system
            // cursor in place, so say why nothing changed instead of leaving the player guessing.
            label = label.copy().append(CursorTranslations.get("cursorkit.custom.edit_empty",
                    "  (no image picked yet)"));
        }
        FlatButton edit = new FlatButton(panelLeft + 6, HEADER_HEIGHT + 30, panelWidth, 18,
                label, button -> openStateEditor());
        edit.visible = isCustomSelected();
        addRenderableWidget(edit);
    }

    /** @return true while the set the player assembles by hand is the selected one */
    private boolean isCustomSelected() {
        return this.selected != null && this.selected.id().equals(CursorSets.CUSTOM_ID);
    }

    /** Opens the per-state path editor and points this screen at the configuration it returns with. */
    private void openStateEditor() {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.setScreenAndShow(new CursorStatesScreen(this, this.draft, this.draft.copy()));
    }

    /** Writes the paths to disk; used by the Done button. */
    private void applyCustomStatesToDisk() {
        CursorManager manager = CursorManager.get();
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        manager.applyConfig(this.draft);
        if (directory != null) {
            manager.rescan(directory);
            CursorReloadListener.rescanTextures(this.minecraft, directory);
        }
        rebuildWidgets();
    }

    /**
     * Installs whatever the player drops onto the window.
     * <p>
     * A {@code .zip} or a pack folder goes into {@code config/cursorkit/packs/}, loose cursor files
     * next to the configuration - the same two places the Folder button opens, so a player who
     * prefers dragging never has to leave the game. Existing names are never overwritten.
     */
    @Override
    public void onFilesDrop(List<Path> paths) {
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        if (directory == null || paths == null || paths.isEmpty()) {
            return;
        }
        List<String> installed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Path path : paths) {
            try {
                String what = CursorPackInstaller.install(path, directory);
                if (what.isEmpty()) {
                    failed.add(path.getFileName().toString());
                } else {
                    installed.add(what);
                    Constants.LOG.debug("Installed {} from {}", what, path);
                }
            } catch (Exception e) {
                Constants.LOG.warn("Could not install {}: {}", path, e.toString());
                failed.add(path.getFileName().toString());
            }
        }
        CursorManager manager = CursorManager.get();
        manager.rescan(directory);
        CursorReloadListener.rescanTextures(this.minecraft, directory);
        if (!installed.isEmpty()) {
            status(CursorTranslations.get("cursorkit.picker.installed", "Installed: %s",
                    String.join(", ", installed)), false);
        } else if (!failed.isEmpty()) {
            status(CursorTranslations.get("cursorkit.picker.not_installed",
                    "Not a cursor pack: %s", String.join(", ", failed)), true);
        }
        rebuildWidgets();
    }

    /** Shows a line of feedback under the list for a few seconds. */
    private void status(Component message, boolean problem) {
        this.statusMessage = message;
        this.statusProblem = problem;
        this.statusUntil = System.nanoTime() / 1_000_000L + STATUS_MS;
    }

    /** Opens the dedicated click point editor for the selected set. */
    private void openHotspotEditor() {
        if (this.selected == null || this.minecraft == null) {
            return;
        }
        // The editor takes its own snapshot: its "Cancel" must only undo the click points it
        // changed, not everything done in the picker before it was opened.
        this.minecraft.setScreenAndShow(new CursorHotspotScreen(this, this.selected, this.draft,
                this.draft.copy()));
    }

    private void openConfigFolder() {
        Path directory = CursorReloadListener.configDirectory(this.minecraft);
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
            // Created up front so it is obvious where a cursor pack (folder or .zip) belongs.
            Files.createDirectories(CursorPackLoader.packsDirectory(directory));
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(directory.toFile());
            } else {
                new ProcessBuilder("xdg-open", directory.toString()).start();
            }
        } catch (Exception e) {
            Constants.LOG.warn("Could not open {}: {}", directory, e.toString());
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        // Left aligned next to the tool buttons: a centred title would sit on top of them.
        extractor.text(this.font, this.title, MARGIN, TOOL_Y + 6, 0xFFFFFFFF);
        drawDetails(extractor);
    }

    /** Translated label of the entry that hands the cursor back to the system. */
    private static Component systemLabel() {
        return CursorTranslations.get("cursorkit.entry.system",
                "Default (system cursor)");
    }

    /** Right hand panel while the system cursor is selected: there is no set to preview. */
    private void drawSystemDetails(GuiGraphicsExtractor extractor) {
        int left = MARGIN * 2 + listWidth();
        int top = HEADER_HEIGHT;
        int right = this.width - MARGIN;
        int bottom = searchBoxY() + SEARCH_HEIGHT;
        extractor.fill(left, top, right, bottom, 0x66000000);

        extractor.text(this.font, systemLabel(), left + 6, top + 6, 0xFFFFFFFF);
        int textWidth = right - left - 12;
        int y = top + 24;
        y += wrapped(extractor,
                CursorTranslations.get("cursorkit.entry.system.line1",
                        "Minecraft's own cursor is used, Cursor Kit draws nothing."),
                left + 6, y, textWidth, 0xFF8899AA) + 4;
        y += wrapped(extractor,
                CursorTranslations.get("cursorkit.entry.system.line2",
                        "Pick any cursor set on the left to take over the cursor."),
                left + 6, y, textWidth, 0xFF8899AA);
        if (CursorManager.get().sets().isEmpty()) {
            wrapped(extractor,
                    CursorTranslations.get("cursorkit.entry.empty",
                            "No cursor sets yet: drop a cursor pack into config/cursorkit/packs/, or use the Folder button."),
                    left + 6, y + 8, textWidth, 0xFFFFD479);
        }
    }

    /**
     * Shows the selected set's click effect in the corner of the details panel.
     * <p>
     * Loops on its own rather than waiting for a click: with three packs bringing three different
     * effects, "what happens when I click" is part of what the player is choosing.
     */
    private void drawEffectPreview(GuiGraphicsExtractor extractor, int left, int top, int right) {
        ClickEffect effect = this.selected == null ? ClickEffect.DEFAULT : this.selected.clickEffect();
        if (effect.disabled()) {
            return;
        }
        int box = EFFECT_BOX;
        int boxX = right - 6 - box;
        int boxY = top + 4;
        extractor.fill(boxX, boxY, boxX + box, boxY + box, 0x44000000);
        // A loop of its own, so the preview keeps playing whether or not anything is clicked.
        long now = System.nanoTime() / 1_000_000L;
        long duration = Math.max(1L, effect.durationMs());
        float progress = (float) (now % duration) / duration;
        ClickEffect preview = effect;
        if (preview.isImage() && preview.size() > box - 4) {
            preview = new ClickEffect(preview.type(), preview.color(), preview.radius(),
                    preview.durationMs(), preview.particles(), preview.texture(), preview.frames(),
                    preview.frameMs(), box - 4);
        } else if (!preview.isImage() && preview.radius() > box / 2.0F - 2) {
            preview = new ClickEffect(preview.type(), preview.color(), box / 2.0F - 2,
                    preview.durationMs(), preview.particles(), preview.texture(), preview.frames(),
                    preview.frameMs(), preview.size());
        }
        ClickEffectPainter.paint(extractor, this.selected, preview, boxX + box / 2.0,
                boxY + box / 2.0, progress, this.minecraft == null ? 1 : this.minecraft.getWindow().getGuiScale());
    }

    /**
     * Draws wrapped text and reports how tall it turned out.
     * <p>
     * {@code textWithWordWrap} returns the y it was given rather than the height, so adding its
     * result to y doubles it - which is what pushed the lines apart.
     *
     * @return the height of the drawn text, in GUI units
     */
    private int wrapped(GuiGraphicsExtractor extractor, Component text, int x, int y, int width,
                        int colour) {
        extractor.textWithWordWrap(this.font, text, x, y, width, colour);
        return Math.max(1, this.font.split(text, width).size()) * (this.font.lineHeight + 1);
    }

    private void drawDetails(GuiGraphicsExtractor extractor) {
        if (this.selected == null || this.systemSelected) {
            // Nothing chosen: either the Default row is selected or no cursor set was found at all.
            // Both cases explain themselves in the right hand panel instead of leaving it blank.
            drawSystemDetails(extractor);
            return;
        }
        int left = MARGIN * 2 + listWidth();
        int top = HEADER_HEIGHT;
        int right = this.width - MARGIN;
        int bottom = searchBoxY() + SEARCH_HEIGHT;
        extractor.fill(left, top, right, bottom, 0x66000000);

        // With the animation switch off the cursor never leaves the arrow, so listing the other
        // states would describe something that is not drawn: show the arrow and say so instead.
        drawEffectPreview(extractor, left, top, right);

        List<PreviewEntry> previewEntries = this.draft.animate()
                ? previewEntries()
                : previewEntries().stream()
                        .filter(entry -> entry.state() == CursorState.DEFAULT).toList();

        Component title = displayName(this.selected).copy().append(Component.literal("  ("))
                .append(originLabel(this.selected.origin()))
                .append(Component.literal(")"));
        int textWidth = Math.max(40, right - left - 12 - EFFECT_BOX - 8);
        extractor.text(this.font, fit(title, textWidth), left + 6, top + 6, 0xFFFFFFFF);
        extractor.text(this.font,
                fit(Component.literal(this.selected.id() + " · " + this.selected.source() + " · ")
                                .append(CursorTranslations.get("cursorkit.effect.label", "Effect: %s",
                                        effectLabel(this.selected.clickEffect()))), textWidth),
                left + 6, top + 18, 0xFF8899AA);

        long now = System.nanoTime() / 1_000_000L;
        int rowHeight = previewRowHeight(previewEntries.size());
        int iconSize = Math.max(10, Math.min(PREVIEW_ICON_SIZE, rowHeight - 4));
        // No room for the file/hotspot line on a short window; the state name is what matters.
        boolean showDetail = rowHeight >= PREVIEW_ROW_HEIGHT - 2;
        for (int index = 0; index < previewEntries.size(); index++) {
            PreviewEntry entry = previewEntries.get(index);
            CursorImage image = entry.image();
            int iconX = previewIconX();
            int rowY = previewRowsTop() + index * rowHeight;
            // The icon may have shrunk, so the hotspot marker has to be scaled the same way.
            float previewScale = (float) iconSize / Math.max(1, entry.frameSize());

            if (entry.texture() != null) {
                // The previews follow the animation switch, otherwise flipping it looks like it does
                // nothing: with the cursor stuck on the arrow every state stands still on frame 0.
                int frame = this.draft.animate()
                        ? Math.min(image.frameAt(now), entry.frames() - 1)
                        : 0;
                extractor.blit(RenderPipelines.GUI_TEXTURED, entry.texture(),
                        iconX, rowY,
                        (float) (frame * entry.frameSize()), 0.0F,
                        iconSize, iconSize,
                        entry.frameSize(), entry.frameSize(),
                        entry.frameSize() * entry.frames(), entry.frameSize());
            }
            // The preview is drawn bigger than the frame, so the hotspot has to grow with it,
            // otherwise the marker sits closer to the top left corner than it really is.
            int hotX = iconX + Math.round(entry.hotspotX() * previewScale);
            int hotY = rowY + Math.round(entry.hotspotY() * previewScale);
            boolean edited = CursorManager.get()
                    .hasHotspotOverride(this.selected.id(), entry.target());
            extractor.fill(hotX - 1, hotY - 1, hotX + 2, hotY + 2,
                    edited ? 0xFFFFD479 : 0xFFFF5555);

            Component stateLabel = CursorTranslations.state(entry.state()).copy()
                    .append(image.animated() ? "  " + image.frames() + "f" : "");
            if (!this.selected.has(entry.state())) {
                stateLabel = stateLabel.copy().append(Component.literal("  "))
                        .append(CursorTranslations.get("cursorkit.state.fallback",
                                "(default)"));
            }
            int labelWidth = Math.max(20, this.width - MARGIN - (iconX + iconSize + 6));
            extractor.text(this.font, fit(stateLabel, labelWidth),
                    iconX + iconSize + 6, rowY + 1, 0xFFDDDDDD);
            if (showDetail) {
                extractor.text(this.font,
                        fit(Component.literal(shortName(image) + "  @" + entry.hotspotX() + ","
                                + entry.hotspotY()), labelWidth),
                        iconX + iconSize + 6, rowY + 11,
                        edited ? 0xFFFFD479 : 0xFF8899AA);
            }
        }

        if (!this.draft.animate()) {
            extractor.text(this.font,
                    CursorTranslations.get("cursorkit.details.static",
                            "Animate off: the cursor stays the arrow"),
                    previewIconX(), previewRowsTop() + rowHeight, 0xFFFFD479);
        }
    }

    /**
     * How many state paths are set but lead nowhere.
     * <p>
     * Those states are previewed with the default image - which is what they fall back to - so
     * without the count on the button the reason would be invisible.
     */
    private int brokenPathCount() {
        if (!isCustomSelected()) {
            return 0;
        }
        Path base = CursorManager.get().gameDirectory();
        int broken = 0;
        for (CursorState state : CursorState.values()) {
            String configured = this.draft.customState(state.id());
            if (configured.isBlank()) {
                continue;
            }
            Path file = CursorSets.resolve(configured, base);
            if (file == null || !Files.isRegularFile(file)) {
                broken++;
            }
        }
        return broken;
    }

    /** Where the preview rows start, and the icon column, shared by drawing and hit testing. */
    private int previewRowsTop() {
        // The hand-made set carries its "edit paths" button above the previews, so they start one
        // line lower; drawing and hit testing both go through here, so they stay in step.
        return HEADER_HEIGHT + 34 + (isCustomSelected() ? 20 : 0);
    }

    private int previewIconX() {
        return MARGIN * 2 + listWidth() + 6;
    }

    /**
     * Height of one preview row for the current window.
     * <p>
     * The six states have to stay visible together - comparing them is the whole point of the panel -
     * so a short window gets shorter rows instead of rows that run into the footer. Drawing and
     * anything that measures the rows go through here, so they stay in step.
     *
     * @param rows how many rows are about to be drawn
     */
    private int previewRowHeight(int rows) {
        int space = Math.max(0, this.height - previewRowsTop() - FOOTER_RESERVED);
        return Math.max(MIN_PREVIEW_ROW_HEIGHT,
                Math.min(PREVIEW_ROW_HEIGHT, space / Math.max(1, rows)));
    }

    /**
     * One row of the details panel. {@code target} is the state an edit changes: a row that only
     * shows the fallback really edits {@code default}.
     */
    private record PreviewEntry(CursorState state, CursorState target, CursorImage image,
                                @Nullable Identifier texture, int frameSize, int frames,
                                float previewScale, int hotspotX, int hotspotY) {
    }

    private List<PreviewEntry> previewEntries() {
        List<PreviewEntry> entries = new ArrayList<>();
        if (this.selected == null || this.systemSelected) {
            return entries;
        }
        for (CursorState state : CursorState.values()) {
            CursorImage image = this.selected.image(state).orElse(null);
            if (image == null) {
                continue;
            }
            Identifier texture = CursorTextures.resolve(this.selected, image);
            // Same geometry the renderer uses, so the marker shows the real click point.
            CursorGeometry geometry = texture == null ? null : CursorGeometry.of(texture, image);
            int frameSize = geometry == null ? CursorGeometry.DEFAULT_FRAME_SIZE
                    : geometry.frameSize();
            int frames = geometry == null ? image.frames() : geometry.frames();
            int hotspotX = geometry == null ? Math.max(0, image.hotspotX())
                    : geometry.hotspotX(image);
            int hotspotY = geometry == null ? Math.max(0, image.hotspotY())
                    : geometry.hotspotY(image);
            entries.add(new PreviewEntry(state, this.selected.has(state) ? state : CursorState.DEFAULT,
                    image, texture, frameSize, frames,
                    (float) PREVIEW_ICON_SIZE / frameSize, hotspotX, hotspotY));
        }
        return entries;
    }

    private static String shortName(CursorImage image) {
        String texture = image.texture();
        int slash = texture.lastIndexOf('/');
        return slash >= 0 ? texture.substring(slash + 1) : texture;
    }

    /** Scrollable list of available cursor sets. */
    private final class SetList extends ObjectSelectionList<SetList.Row> {

        SetList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            // A picker list reads better top aligned than vertically centred.
            this.centerListVertically = false;
        }

        void populate(List<CursorSet> sets, String filter) {
            clearEntries();
            String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
            Row toSelect = null;

            // The two fixed rows come first and never move: the way back to vanilla, and the set
            // the player assembles by hand. Searching filters the sets below them, not them.
            Row systemRow = new Row(null);
            addEntry(systemRow);
            if (CursorKitScreen.this.systemSelected) {
                toSelect = systemRow;
            }
            CursorSet handMade = null;
            for (CursorSet set : sets) {
                if (set.id().equals(CursorSets.CUSTOM_ID)) {
                    handMade = set;
                    break;
                }
            }
            if (handMade != null) {
                Row row = new Row(handMade);
                addEntry(row);
                if (!CursorKitScreen.this.systemSelected && CursorKitScreen.this.selected != null
                        && CursorKitScreen.this.selected.id().equals(handMade.id())) {
                    toSelect = row;
                }
            }
            for (CursorSet set : sets) {
                if (set.id().equals(CursorSets.CUSTOM_ID)) {
                    // Already pinned as the second row.
                    continue;
                }
                if (!needle.isEmpty() && !matches(set, needle)) {
                    continue;
                }
                Row row = new Row(set);
                addEntry(row);
                if (!CursorKitScreen.this.systemSelected && CursorKitScreen.this.selected != null
                        && CursorKitScreen.this.selected.id().equals(set.id())) {
                    toSelect = row;
                }
            }
            if (toSelect != null) {
                setSelected(toSelect);
                if (getSelected() == children().get(0)) {
                    // Vanilla scrolls a selected entry to the middle of the list; for the very
                    // first row that hides the row the player just selected.
                    setScrollAmount(0.0D);
                }
            }
        }

        private static boolean matches(CursorSet set, String needle) {
            return set.id().toLowerCase(Locale.ROOT).contains(needle)
                    || set.name().toLowerCase(Locale.ROOT).contains(needle)
                    || set.source().toLowerCase(Locale.ROOT).contains(needle);
        }

        private static boolean matchesSystem(String needle) {
            return systemLabel().getString().toLowerCase(Locale.ROOT).contains(needle)
                    || CursorTranslations.get("cursorkit.origin.system", "system cursor")
                            .getString().toLowerCase(Locale.ROOT).contains(needle);
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 12;
        }

        final class Row extends ObjectSelectionList.Entry<Row> {

            /** {@code null} marks the "Default" row, which hands the cursor back to the system. */
            private final @Nullable CursorSet set;

            Row(@Nullable CursorSet set) {
                this.set = set;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                if (this.set == null) {
                    boolean active = CursorKitScreen.this.systemSelected;
                    String prefix = active ? "> " : "  ";
                    int colour = active ? 0xFFFFD479 : (hovered ? 0xFFFFFFFF : 0xFFBFBFBF);
                    extractor.text(CursorKitScreen.this.font,
                            CursorKitScreen.this.fit(Component.literal(prefix)
                                    .append(systemLabel()), SetList.this.getRowWidth() - 6),
                            this.getContentX() + 2, this.getContentY() + 2, colour);
                    extractor.text(CursorKitScreen.this.font,
                            CursorKitScreen.this.fit(
                                    CursorTranslations.get("cursorkit.origin.system",
                                            "system cursor"),
                                    SetList.this.getRowWidth() - 6),
                            this.getContentX() + 2, this.getContentY() + 11, 0xFF8899AA);
                    return;
                }
                boolean active = !CursorKitScreen.this.systemSelected
                        && CursorKitScreen.this.selected != null
                        && CursorKitScreen.this.selected.id().equals(this.set.id());
                String prefix = active ? "> " : "  ";
                int colour = active ? 0xFFFFD479 : (hovered ? 0xFFFFFFFF : 0xFFBFBFBF);
                extractor.text(CursorKitScreen.this.font,
                        Component.literal(prefix).append(displayName(this.set)),
                        this.getContentX() + 2, this.getContentY() + 2, colour);
                int stateCount = this.set.images().size();
                Component subtitle = originLabel(this.set.origin())
                        .copy()
                        .append(Component.literal(" · "))
                        .append(stateCount == 1
                                ? CursorTranslations.get("cursorkit.state_one", "1 state")
                                : CursorTranslations.get("cursorkit.states", "%s states",
                                        stateCount))
                        // Which click effect a set brings is part of choosing it, so it belongs in
                        // the list next to the states, not only in the details panel.
                        .append(Component.literal(" · "))
                        .append(CursorTranslations.get("cursorkit.effect.label", "Effect: %s",
                                effectLabel(this.set.clickEffect())));
                extractor.text(CursorKitScreen.this.font,
                        CursorKitScreen.this.fit(subtitle, SetList.this.getRowWidth() - 6),
                        this.getContentX() + 2, this.getContentY() + 11, 0xFF8899AA);
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,
                                        boolean doubleClick) {
                if (this.set == null) {
                    CursorKitScreen.this.chooseSystemCursor();
                } else {
                    CursorKitScreen.this.choose(this.set);
                }
                return true;
            }

            @Override
            public Component getNarration() {
                return this.set == null ? systemLabel() : displayName(this.set);
            }
        }
    }
}
