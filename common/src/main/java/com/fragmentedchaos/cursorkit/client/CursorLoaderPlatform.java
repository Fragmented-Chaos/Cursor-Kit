package com.fragmentedchaos.cursorkit.client;

/**
 * Tells the shared code which loader it is running on, for the couple of places where the loaders
 * genuinely differ.
 * <p>
 * Right now that is the reload listener registration: NeoForge rejects a mod listener that was
 * added to the resource manager outside its {@code AddClientReloadListenersEvent} ("A non-vanilla
 * reload listener ... was added via mixin before the AddClientReloadListenerEvent!"), while Fabric
 * and Quilt have no such event and are happy with the vanilla resource manager.
 */
public final class CursorLoaderPlatform {

    private static final String NEOFORGE_MARKER = "net.neoforged.fml.ModList";

    private static volatile Boolean neoForge;

    private CursorLoaderPlatform() {
        throw new UnsupportedOperationException("CursorLoaderPlatform cannot be instantiated");
    }

    public static boolean isNeoForge() {
        Boolean cached = neoForge;
        if (cached == null) {
            cached = hasClass(NEOFORGE_MARKER);
            neoForge = cached;
        }
        return cached;
    }

    /** @return true when this side should register the reload listener itself */
    public static boolean selfRegisterReloadListener() {
        return !isNeoForge();
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, CursorLoaderPlatform.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
