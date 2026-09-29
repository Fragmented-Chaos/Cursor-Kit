package com.fragmentedchaos.cursorkit;

/**
 * Client bootstrap shared by both loader entry points.
 * <p>
 * Cursor state detection, the resource-pack driven cursor sets and the cursor renderer will live
 * here; the loader modules only wire their own events and hooks into this class.
 */
public final class CursorKitCommon {

    private CursorKitCommon() {
        throw new UnsupportedOperationException("CursorKitCommon cannot be instantiated");
    }

    /** Called once from the Fabric and NeoForge client entry points. */
    public static void initClient() {
        Constants.LOG.info("{} initialised on the client", Constants.MOD_NAME);
    }
}
