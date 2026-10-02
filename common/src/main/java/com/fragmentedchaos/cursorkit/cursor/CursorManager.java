package com.fragmentedchaos.cursorkit.cursor;

import com.fragmentedchaos.cursorkit.cursor.config.CursorConfig;
import com.fragmentedchaos.cursorkit.cursor.config.CursorHotspots;
import com.fragmentedchaos.cursorkit.cursor.load.CursorPackLoader;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSetLoader;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSetRegistry;
import com.fragmentedchaos.cursorkit.cursor.load.CursorSets;
import com.fragmentedchaos.cursorkit.cursor.model.CursorSet;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.Constants;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Holds the loaded cursor sets and the current selection.
 * <p>
 * Populated on every client resource reload; the selection is kept in memory for now and will be
 * persisted to {@code config/cursorkit.json} in the configuration milestone.
 */
public final class CursorManager {

    /**
     * Value of {@code selected_set} that hands the cursor back to the system: nothing is drawn and
     * the window's own cursor stays visible. This is what the picker's "Default" row selects and
     * what a fresh configuration starts with.
     */
    public static final String SYSTEM_SELECTION = "";

    private static final CursorManager INSTANCE = new CursorManager();

    private volatile List<CursorSet> sets = List.of();
    /** the sets exactly as they were loaded, without the player's edited click points */
    private volatile List<CursorSet> baseSets = List.of();
    private volatile List<CursorSet> resourceSets = List.of();
    private volatile String selectedId;
    private volatile CursorConfig config = new CursorConfig();
    private volatile Path configFile;
    private volatile Path gameDirectory;
    private volatile int generation;

    private CursorManager() {
    }

    public static CursorManager get() {
        return INSTANCE;
    }

    /**
     * Re-reads the configuration and every cursor set; called on client resource reload.
     *
     * @param configDirectory the {@code config/cursorkit} directory holding user cursor sets
     * @param configFile      the {@code config/cursorkit.json} configuration file
     */
    public void reload(ResourceManager resourceManager, Path configDirectory, Path configFile) {
        this.configFile = configFile;
        this.config = CursorConfig.load(configFile);
        this.selectedId = this.config.selectedSet();

        // Everything that can only change through a resource reload is read once and kept: a live
        // rescan of the config directory reuses it instead of asking the resource manager again.
        // Every set comes from a pack now: resource packs, cursor packs or the config directory.
        this.resourceSets = List.copyOf(CursorSetLoader.loadFromResourcePacks(resourceManager));

        List<CursorSet> loaded = mergeConfig(configDirectory);
        boolean changed = apply(loaded);
        // One line per start, so a "why did it pick that cursor" report can be answered from the log.
        Constants.LOG.debug("Cursor selection after loading: stored='{}' resolved='{}' available={}",
                this.config.selectedSet(),
                findSelected().map(CursorSet::id).orElse("<system cursor>"),
                this.sets.size());
        Constants.LOG.debug("Loaded {} cursor set(s): {}", this.sets.size(),
                this.sets.stream().map(CursorSet::describe).toList());
        Constants.LOG.debug("Configured click effect: {} / hand-made set present: {} / its effect: {}",
                this.config.customEffect().type(),
                this.sets.stream().anyMatch(set -> set.id().equals(CursorSets.CUSTOM_ID)),
                this.sets.stream().filter(set -> set.id().equals(CursorSets.CUSTOM_ID)).findFirst()
                        .map(set -> set.clickEffect().type()).orElse("-"));
        if (!changed) {
            Constants.LOG.debug("Cursor set list is unchanged");
        }

        if (!isSystemSelected() && findSelected().isEmpty()) {
            // The set the player picked is gone (deleted, or its resource pack was switched off).
            // Hand the cursor back to the system - and write that down, otherwise the next start
            // would silently land on some other set, which looks like the mod picked one itself.
            Constants.LOG.warn("Selected cursor set '{}' no longer exists, using the system cursor",
                    this.selectedId);
            selectSystem();
        }
    }

    /**
     * Re-reads only what lives in {@code config/cursorkit/} (loose files and cursor packs) and keeps
     * the current selection. Used to pick up files a user dropped in while the game is running.
     * <p>
     * Unlike {@link #reload}, this never writes the configuration: a set that is briefly unreadable
     * (a file still being copied, for example) must not make the player lose their choice.
     *
     * @param configDirectory the {@code config/cursorkit} directory
     * @return true when the resulting set list differs from the one that was active
     */
    public boolean rescan(Path configDirectory) {
        return apply(mergeConfig(configDirectory));
    }

    /**
     * Reads the config directory <b>without touching this manager's state</b>, so the caller may run
     * it on a background thread: it opens zips, parses JSON and reads PNG headers, which is far too
     * much work to do inside a frame.
     *
     * @param configDirectory the {@code config/cursorkit} directory
     * @return the merged set list, ready for {@link #applyLoaded(List)}
     */
    public List<CursorSet> loadFrom(Path configDirectory) {
        return mergeConfig(configDirectory);
    }

    /**
     * Makes the sets {@link #loadFrom(Path)} read visible.
     * <p>
     * Render thread only: it swaps the list the renderer and the picker look at, bumps the generation
     * that tells them to refresh, and has to be seen by them in the same frame.
     *
     * @param loaded the list returned by {@link #loadFrom(Path)}
     * @return true when the visible set list actually changed
     */
    public boolean applyLoaded(List<CursorSet> loaded) {
        return apply(loaded);
    }

    private List<CursorSet> mergeConfig(Path configDirectory) {
        List<CursorSet> found = new java.util.ArrayList<>(this.resourceSets);
        found.addAll(CursorPackLoader.loadAll(configDirectory));
        found.addAll(CursorSetLoader.loadFromConfigDirectory(configDirectory));
        // Last, so an edited path is picked up by the same rescan as everything else.
        found.add(CursorSets.custom(this.config, this.gameDirectory,
                this.configFile == null ? "config" : this.configFile.toString()));
        return CursorSetRegistry.merge(found);
    }

    /**
     * Where relative image paths in the configuration are resolved from.
     *
     * @param gameDirectory the game directory, usually set once at startup
     */
    public void setGameDirectory(@Nullable Path gameDirectory) {
        this.gameDirectory = gameDirectory;
    }

    /** @return true while the set the player assembles by hand is the selected one */
    public boolean isCustomSelected() {
        return CursorSets.CUSTOM_ID.equals(this.selectedId);
    }

    /** @return the game directory relative custom image paths are resolved from, may be {@code null} */
    public @Nullable Path gameDirectory() {
        return this.gameDirectory;
    }

    /** @return true when the set list actually changed */
    private boolean apply(List<CursorSet> loaded) {
        this.baseSets = List.copyOf(loaded);
        return publish(CursorHotspots.apply(this.baseSets, this.config));
    }

    /** @return true when the visible set list actually changed */
    private boolean publish(List<CursorSet> visible) {
        boolean changed = !visible.equals(this.sets);
        this.sets = visible;
        if (changed) {
            this.generation++;
        }
        return changed;
    }

    /**
     * Moves one state's click point for a set. The value is kept in memory right away so the picker
     * and the cursor follow the mouse, {@link #applyConfig} writes it to disk when the drag ends.
     */
    public void setHotspotOverride(String setId, CursorState state, int x, int y) {
        this.config.setHotspot(setId, state.id(), x, y);
        publish(CursorHotspots.apply(this.baseSets, this.config));
    }

    /** Drops an edited click point, falling back to the value from the cursor set. */
    public void clearHotspotOverride(String setId, CursorState state) {
        this.config.clearHotspot(setId, state.id());
        publish(CursorHotspots.apply(this.baseSets, this.config));
    }

    /** @return true when the player moved this state's click point away from the set's own value */
    public boolean hasHotspotOverride(String setId, CursorState state) {
        return this.config.hotspot(setId, state.id()) != null;
    }

    /** @return the active configuration, never {@code null} */
    public CursorConfig config() {
        return this.config;
    }

    /** Replaces the configuration and writes it to disk. */
    public void applyConfig(CursorConfig updated) {
        previewConfig(updated);
        this.config.save(this.configFile);
    }

    /**
     * Replaces the configuration in memory only: the picker uses this while a hotspot is being
     * dragged, so the file is written once when the mouse is released instead of on every step.
     */
    public void previewConfig(CursorConfig updated) {
        this.config = updated.copy();
        this.selectedId = this.config.selectedSet();
        publish(CursorHotspots.apply(this.baseSets, this.config));
    }

    /** @return every available set, sorted by name */
    public List<CursorSet> sets() {
        return this.sets;
    }

    /**
     * @return a counter that changes whenever the available sets change. Screens compare it once in
     *         a while instead of rebuilding their list every frame.
     */
    public int generation() {
        return this.generation;
    }

    /**
     * @return the selected set, or the first available one when the stored id is gone, or empty when
     *         the system cursor is selected (or nothing was loaded)
     */
    public Optional<CursorSet> findSelected() {
        if (isSystemSelected()) {
            return Optional.empty();
        }
        return CursorSetRegistry.find(this.sets, this.selectedId);
    }

    /** @return true when the configuration asks for the system cursor instead of a custom one */
    public boolean isSystemSelected() {
        return this.selectedId == null || this.selectedId.isEmpty();
    }

    /** Hands the cursor back to the system; persisted like any other selection. */
    public void selectSystem() {
        this.selectedId = SYSTEM_SELECTION;
        this.config.setSelectedSet(SYSTEM_SELECTION);
        this.config.save(this.configFile);
    }

    /** @return the id of the active set, if any */
    public Optional<String> selectedId() {
        return findSelected().map(CursorSet::id);
    }

    /**
     * @param id the set to activate
     * @return true when a set with that id exists
     */
    public boolean select(String id) {
        if (id == null || id.isEmpty()) {
            selectSystem();
            return true;
        }
        for (CursorSet set : this.sets) {
            if (set.id().equals(id)) {
                this.selectedId = id;
                this.config.setSelectedSet(id);
                this.config.save(this.configFile);
                return true;
            }
        }
        return false;
    }
}
