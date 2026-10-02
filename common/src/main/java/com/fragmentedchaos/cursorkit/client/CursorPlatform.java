package com.fragmentedchaos.cursorkit.client;

/**
 * What the shared code needs to know about the mod loader it is running on.
 * <p>
 * Each loader module implements this and registers its implementation in {@link CursorPlatforms};
 * the shared code never names a loader itself. Both values come from the loader's own metadata
 * rather than from a constant here, so a loader that renames itself, or a compatibility layer that
 * runs one loader on top of another, reports what is actually installed.
 * <p>
 * Deliberately small: only facts a loader can report about itself belong here. Anything a mixin has
 * to know <em>before</em> any mod code runs cannot, because mixins are applied before the entry
 * points that register an implementation. See {@link CursorPlatforms#isNeoForge()}.
 */
public interface CursorPlatform {

    /** @return the loader's own name, as its metadata spells it, e.g. {@code Fabric Loader} */
    String loaderName();

    /** @return the loader's own version, or an empty string when its metadata has none */
    String loaderVersion();
}
