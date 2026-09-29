package com.fragmentedchaos.cursorkit.cursor.config;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetFormatException;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.render.CursorVisibilityPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The mod's configuration file ({@code config/cursorkit.json}).
 * <p>
 * Minecraft-free so loading, saving and validation are unit tested without a game instance. A
 * missing file is written with the defaults, a broken file falls back to the defaults instead of
 * breaking the mod.
 */
public final class CursorConfig {

    /** File name inside the {@code config} directory. Cursor sets live in {@code config/cursorkit/}. */
    public static final String FILE_NAME = Constants.MOD_ID + ".json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String selectedSet = "";
    private int scale = 1;
    private boolean animate = true;
    /** whether a click leaves a ripple at the cursor's position */
    private boolean clickEffect = true;
    /** state id -> image file the player picked for it; empty means the hand-made set is unused */
    private final Map<String, String> customStates = new LinkedHashMap<>();
    /** the click effect the hand-made set uses, on top of the per-state paths */
    private ClickEffect customEffect = ClickEffect.DEFAULT;
    private int edgeMargin = CursorVisibilityPolicy.DEFAULT_EDGE_MARGIN;
    /** set id -> state id -> click point in image pixels, for hotspots the player moved in the UI */
    private final Map<String, Map<String, int[]>> hotspots = new LinkedHashMap<>();

    public CursorConfig() {
    }

    public boolean enabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the selected cursor set name, empty when the user never picked one */
    public String selectedSet() {
        return this.selectedSet;
    }

    public void setSelectedSet(String selectedSet) {
        this.selectedSet = selectedSet == null ? "" : selectedSet;
    }

    /** @return an extra integer scale applied on top of the GUI scale, at least 1 */
    public int scale() {
        return this.scale;
    }

    public void setScale(int scale) {
        this.scale = Math.max(1, scale);
    }

    /** @return whether cursor images with several frames are played; when off they stay on frame 0 */
    public boolean animate() {
        return this.animate;
    }

    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    /** @return whether a click leaves a ripple at the cursor's position */
    public boolean clickEffect() {
        return this.clickEffect;
    }

    public void setClickEffect(boolean clickEffect) {
        this.clickEffect = clickEffect;
    }

    /** @return the click effect the hand-made set plays when clicked */
    public ClickEffect customEffect() {
        return this.customEffect;
    }

    public void setCustomEffect(ClickEffect customEffect) {
        this.customEffect = customEffect == null ? ClickEffect.DEFAULT : customEffect;
    }

    /** @return the per-state image paths of the hand-made cursor set, in state order */
    public Map<String, String> customStates() {
        return Map.copyOf(this.customStates);
    }

    /**
     * @param stateId one of {@link CursorState}'s ids
     * @return the image file the player set for that state, or an empty string
     */
    public String customState(String stateId) {
        return this.customStates.getOrDefault(stateId, "");
    }

    /** Sets or clears one state's image path; a blank value removes the entry. */
    public void setCustomState(String stateId, String path) {
        String trimmed = path == null ? "" : path.trim();
        if (trimmed.isEmpty()) {
            this.customStates.remove(stateId);
        } else {
            this.customStates.put(stateId, trimmed);
        }
    }

    /** @return distance from the window border where the system cursor is kept, at least 0 */
    public int edgeMargin() {
        return this.edgeMargin;
    }

    public void setEdgeMargin(int edgeMargin) {
        this.edgeMargin = Math.max(0, edgeMargin);
    }

    /**
     * @param setId the cursor set id
     * @param state the state whose click point the player edited
     * @return the stored click point, or {@code null} when the set's own value is used
     */
    public @Nullable int[] hotspot(String setId, String state) {
        Map<String, int[]> states = this.hotspots.get(setId);
        int[] found = states == null ? null : states.get(state);
        return found == null ? null : new int[] {found[0], found[1]};
    }

    /**
     * Stores a click point the player picked in the picker. Values are clamped to the frame when the
     * set is used, the frame size is only known once the image was read.
     */
    public void setHotspot(String setId, String state, int x, int y) {
        this.hotspots.computeIfAbsent(setId, key -> new LinkedHashMap<>())
                .put(state, new int[] {Math.max(0, x), Math.max(0, y)});
    }

    /** Drops an edited click point, so the value from the cursor set is used again. */
    public void clearHotspot(String setId, String state) {
        Map<String, int[]> states = this.hotspots.get(setId);
        if (states == null) {
            return;
        }
        states.remove(state);
        if (states.isEmpty()) {
            this.hotspots.remove(setId);
        }
    }

    /** Replaces every edited click point, used when one screen takes over another's edits. */
    public void replaceHotspots(Map<String, Map<String, int[]>> replacement) {
        this.hotspots.clear();
        if (replacement == null) {
            return;
        }
        replacement.forEach((setId, states) -> states.forEach(
                (state, point) -> setHotspot(setId, state, point[0], point[1])));
    }

    /** @return a deep copy of every edited click point */
    public Map<String, Map<String, int[]>> hotspots() {
        Map<String, Map<String, int[]>> copy = new LinkedHashMap<>();
        this.hotspots.forEach((setId, states) -> {
            Map<String, int[]> statesCopy = new LinkedHashMap<>();
            states.forEach((state, point) -> statesCopy.put(state, new int[] {point[0], point[1]}));
            copy.put(setId, statesCopy);
        });
        return copy;
    }

    /** @return a deep copy, handy for a settings screen that needs a "cancel" button */
    public CursorConfig copy() {
        CursorConfig copy = new CursorConfig();
        copy.setEnabled(this.enabled);
        copy.setSelectedSet(this.selectedSet);
        copy.setScale(this.scale);
        copy.setAnimate(this.animate);
        copy.setClickEffect(this.clickEffect);
        copy.customStates.putAll(this.customStates);
        copy.setCustomEffect(this.customEffect);
        copy.setEdgeMargin(this.edgeMargin);
        copy.hotspots.putAll(hotspots());
        return copy;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", this.enabled);
        root.addProperty("selected_set", this.selectedSet);
        root.addProperty("scale", this.scale);
        root.addProperty("animate", this.animate);
        root.addProperty("click_effect", this.clickEffect);
        if (!this.customEffect.equals(ClickEffect.DEFAULT)) {
            root.add("custom_effect", this.customEffect.toJson());
        }
        if (!this.customStates.isEmpty()) {
            JsonObject states = new JsonObject();
            this.customStates.forEach(states::addProperty);
            root.add("custom_states", states);
        }
        root.addProperty("edge_margin", this.edgeMargin);
        if (!this.hotspots.isEmpty()) {
            JsonObject edited = new JsonObject();
            this.hotspots.forEach((setId, states) -> {
                JsonObject statesJson = new JsonObject();
                states.forEach((state, point) -> {
                    JsonArray array = new JsonArray();
                    array.add(point[0]);
                    array.add(point[1]);
                    statesJson.add(state, array);
                });
                edited.add(setId, statesJson);
            });
            root.add("hotspots", edited);
        }
        return root;
    }

    public static CursorConfig fromJson(JsonObject root) {
        CursorConfig config = new CursorConfig();
        if (root == null) {
            return config;
        }
        if (root.has("enabled")) {
            config.setEnabled(root.get("enabled").getAsBoolean());
        }
        if (root.has("selected_set")) {
            config.setSelectedSet(root.get("selected_set").getAsString());
        }
        if (root.has("scale")) {
            config.setScale(root.get("scale").getAsInt());
        }
        // "animate_busy" is what the key was called while only the busy state animated.
        if (root.has("animate")) {
            config.setAnimate(root.get("animate").getAsBoolean());
        } else if (root.has("animate_busy")) {
            config.setAnimate(root.get("animate_busy").getAsBoolean());
        }
        if (root.has("click_effect")) {
            config.setClickEffect(root.get("click_effect").getAsBoolean());
        }
        if (root.has("custom_effect")) {
            try {
                config.setCustomEffect(ClickEffect.parse(root.get("custom_effect"), "custom_effect"));
            } catch (CursorSetFormatException e) {
                Constants.LOG.warn("Ignoring the custom click effect: {}", e.getMessage());
            }
        }
        if (root.has("custom_states") && root.get("custom_states").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry
                    : root.getAsJsonObject("custom_states").entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    config.setCustomState(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        if (root.has("edge_margin")) {
            config.setEdgeMargin(root.get("edge_margin").getAsInt());
        }
        if (root.has("hotspots") && root.get("hotspots").isJsonObject()) {
            for (Map.Entry<String, JsonElement> set : root.getAsJsonObject("hotspots").entrySet()) {
                if (!set.getValue().isJsonObject()) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> state : set.getValue().getAsJsonObject().entrySet()) {
                    JsonElement point = state.getValue();
                    if (!point.isJsonArray() || point.getAsJsonArray().size() != 2) {
                        Constants.LOG.warn("Ignoring hotspot '{}' of cursor set '{}': not a pair",
                                state.getKey(), set.getKey());
                        continue;
                    }
                    JsonArray array = point.getAsJsonArray();
                    config.setHotspot(set.getKey(), state.getKey(),
                            array.get(0).getAsInt(), array.get(1).getAsInt());
                }
            }
        }
        return config;
    }

    /**
     * Reads the configuration, writing the defaults when the file does not exist yet.
     *
     * @param file the config file, may be {@code null}
     * @return the loaded configuration, never {@code null}
     */
    public static CursorConfig load(Path file) {
        if (file == null) {
            return new CursorConfig();
        }
        if (!Files.isRegularFile(file)) {
            CursorConfig defaults = new CursorConfig();
            defaults.save(file);
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return fromJson(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            Constants.LOG.warn("Could not read {} ({}), falling back to the defaults",
                    file, e.toString());
            return new CursorConfig();
        }
    }

    /** Writes the configuration, creating the parent directory when needed. */
    public void save(Path file) {
        if (file == null) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(toJson(), writer);
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            Constants.LOG.warn("Could not write {}: {}", file, e.toString());
        }
    }
}
