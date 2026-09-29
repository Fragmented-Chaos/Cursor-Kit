package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Translations for this mod's own texts, read from the mod's language files.
 * <p>
 * Minecraft only loads a mod's {@code assets/<id>/lang/} files when something puts that mod into the
 * resource system, which Fabric and Quilt only do with Fabric API installed. Without it every string
 * fell back to the English text compiled into the code. The picker therefore asks here instead:
 * the game's own translation wins when it exists (NeoForge, or a resource pack providing the keys),
 * otherwise the language file shipped in this jar is used, and only then the English fallback.
 * <p>
 * The file is cached per selected language, so switching language in the options takes effect on the
 * next lookup without any reload hook.
 */
public final class CursorTranslations {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

    private static String cachedCode;
    private static Map<String, String> cached = Map.of();

    private CursorTranslations() {
        throw new UnsupportedOperationException("CursorTranslations cannot be instantiated");
    }

    /**
     * @param key      translation key, as used in the language files
     * @param fallback English text used when neither the game nor this mod has a translation
     * @param args     values for the {@code %s} placeholders; {@link Component}s are resolved first
     * @return the translated, formatted text
     */
    public static Component get(String key, String fallback, Object... args) {
        String text = resolve(key, fallback);
        return Component.literal(args.length == 0 ? text : format(text, args));
    }

    private static String resolve(String key, String fallback) {
        Language language = Language.getInstance();
        if (language != null && language.has(key)) {
            return language.getOrDefault(key);
        }
        return translations().getOrDefault(key, fallback);
    }

    static String format(String text, Object[] args) {
        Object[] values = new Object[args.length];
        for (int index = 0; index < args.length; index++) {
            values[index] = args[index] instanceof Component component
                    ? component.getString() : args[index];
        }
        try {
            return String.format(text, values);
        } catch (RuntimeException e) {
            // A language file with a broken placeholder must not take the screen down.
            return text;
        }
    }

    /** @return the keys and texts of one of this mod's language files, empty when it is missing */
    static Map<String, String> load(String code) {
        String path = "/assets/" + Constants.MOD_ID + "/lang/" + code + ".json";
        try (InputStream stream = CursorTranslations.class.getResourceAsStream(path)) {
            if (stream == null) {
                return Map.of();
            }
            Map<String, String> parsed = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), MAP_TYPE);
            return parsed == null ? Map.of() : Map.copyOf(parsed);
        } catch (Exception e) {
            Constants.LOG.warn("Could not read {}: {}", path, e.toString());
            return Map.of();
        }
    }

    private static Map<String, String> translations() {
        String code = currentCode();
        if (!code.equals(cachedCode)) {
            cachedCode = code;
            cached = load(code);
        }
        return cached;
    }

    private static String currentCode() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) {
            return "en_us";
        }
        String selected = minecraft.getLanguageManager().getSelected();
        return selected == null || selected.isBlank() ? "en_us" : selected;
    }
}
