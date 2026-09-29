package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.client.sodium.CursorSodiumConfig;

/**
 * Decides which screens get the entry button to the cursor picker.
 * <p>
 * The button belongs in the video settings, and that screen is often replaced by a mod: Sodium and
 * Embeddium ship their own. They cannot be referenced here - a mod that is not installed would break
 * the build and a mixin targeting a missing class would fail - so the decision is made from the
 * class name. Anything that looks like a video or graphics options screen gets the button, and the
 * picker itself never does.
 * <p>
 * Sodium's own screens are the exception once Sodium has taken the entry into its option list; the
 * flag for that lives here rather than in {@link CursorSodiumConfig} so that the screens every game has
 * to open never touch the class that talks to Sodium.
 */
public final class CursorEntryPoint {

    /** Parts of a class name that mark a screen as the place for the video settings entry. */
    private static final String[] VIDEO_SCREEN_HINTS = {
            "videosettings", "videooptions", "sodium", "embeddium", "optifine",
    };

    /** Package Sodium keeps its own screens in. */
    private static final String SODIUM_SCREEN_PACKAGE = "net.caffeinemc.mods.sodium.client.gui.";

    private static boolean sodiumLinked;

    private CursorEntryPoint() {
        throw new UnsupportedOperationException("CursorEntryPoint cannot be instantiated");
    }

    /** Called by {@link CursorSodiumConfig} once Sodium listed the cursor page in its video settings. */
    public static void markSodiumLinked() {
        sodiumLinked = true;
    }

    /** @return true when Sodium shows the cursor entry itself, so its screens need no button */
    public static boolean isSodiumLinked() {
        return sodiumLinked;
    }

    /**
     * @param className fully qualified name of the screen, usually {@code getClass().getName()}
     * @return true when the cursor entry button belongs on that screen
     */
    public static boolean isVideoSettingsScreen(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        String lower = className.toLowerCase(java.util.Locale.ROOT);
        // Never on our own screens: the picker has its own buttons.
        if (lower.startsWith("com.fragmentedchaos.cursorkit.")) {
            return false;
        }
        for (String hint : VIDEO_SCREEN_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sodium gives the entry a page of its own in the option list, so its screens must not also get
     * the floating button. Only checked when Sodium actually took that page - see
     * {@link #isSodiumLinked()}.
     *
     * @param className fully qualified name of the screen, usually {@code getClass().getName()}
     * @return true when the screen is one of Sodium's own
     */
    public static boolean isSodiumScreen(String className) {
        return className != null && className.startsWith(SODIUM_SCREEN_PACKAGE);
    }
}
