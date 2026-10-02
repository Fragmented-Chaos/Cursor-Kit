package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Where the loader modules publish their {@link CursorPlatform}, and where the shared code asks for
 * the one that is running.
 * <p>
 * There is no probing here. An implementation is registered by the loader module it describes, so a
 * loader with no implementation is reported as {@code Unknown} instead of being guessed at.
 */
public final class CursorPlatforms {

    /** What the about popup shows when the running loader published no platform. */
    public static final String UNKNOWN_LOADER = "Unknown";

    /**
     * The marker class NeoForge is recognised by.
     * <p>
     * This one fact cannot come from a registered {@link CursorPlatform}: it is needed while mixins
     * are being applied, which is before any entry point runs and therefore before anything can be
     * registered (see {@link #isNeoForge()}).
     */
    private static final String NEOFORGE_MARKER = "net.neoforged.fml.ModList";

    private static volatile Optional<CursorPlatform> platform = Optional.empty();
    private static volatile Boolean neoForge;

    private CursorPlatforms() {
        throw new UnsupportedOperationException("CursorPlatforms cannot be instantiated");
    }

    /**
     * Publishes the platform for the loader this game is running on.
     * <p>
     * Called from the loader's own client entry point. The first registration wins, so a second call
     * - the same entry point running twice, or a compatibility layer forwarding to another loader's
     * entry point - cannot overwrite the loader that actually started the game.
     */
    public static synchronized void register(@Nullable CursorPlatform implementation) {
        if (implementation == null || platform.isPresent()) {
            return;
        }
        platform = Optional.of(implementation);
        Constants.LOG.debug("Cursor Kit running on {} {}", implementation.loaderName(),
                implementation.loaderVersion());
    }

    /** @return the registered platform, or empty when the running loader has none */
    public static Optional<CursorPlatform> get() {
        return platform;
    }

    /**
     * The loader's name for the about popup.
     * <p>
     * {@code Unknown} is the honest answer when nothing registered: the alternative is naming a
     * loader that was never checked for, which is what this class exists to stop doing.
     */
    public static String loaderName() {
        return platform.map(CursorPlatform::loaderName).orElse(UNKNOWN_LOADER);
    }

    /** @return the loader's version, or an empty string when no platform registered one */
    public static String loaderVersion() {
        return platform.map(CursorPlatform::loaderVersion).orElse("");
    }

    /**
     * What the about popup shows for the loader: its name, and behind it the version its own
     * metadata reported.
     * <p>
     * The version is left off when there is none, and when nothing is known the name stands alone -
     * printing {@code Unknown } with a trailing gap would look like a bug.
     */
    public static String loaderDisplayName() {
        String name = loaderName();
        String version = loaderVersion();
        return name.equals(UNKNOWN_LOADER) || version.isEmpty() ? name : name + " " + version;
    }

    /**
     * @return true when this game is running on NeoForge, so a mod reload listener added to the
     *         vanilla resource manager by a mixin is refused and has to come from the loader's own
     *         event.
     *         <p>
     *         Answered from a marker class rather than from the registered platform, and cached,
     *         because {@code ReloadableResourceManager}'s constructor runs before the entry points
     *         that register a platform. The marker is the only loader fact that is knowable that
     *         early; a platform registered later cannot change this answer, and none would: a loader
     *         that refuses mixin listeners does so from the moment it starts.
     */
    public static boolean isNeoForge() {
        Boolean cached = neoForge;
        if (cached == null) {
            cached = hasClass(NEOFORGE_MARKER);
            neoForge = cached;
        }
        return cached;
    }

    /**
     * Forgets the registered platform, so one test does not inherit another's registration.
     * <p>
     * Only the tests call this; the game registers once and never looks back.
     */
    static void resetForTests() {
        platform = Optional.empty();
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, CursorPlatforms.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
