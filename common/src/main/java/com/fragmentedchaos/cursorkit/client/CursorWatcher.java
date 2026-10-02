package com.fragmentedchaos.cursorkit.client;

import com.fragmentedchaos.cursorkit.cursor.load.CursorSetLoader;
import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.CursorManager;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

/**
 * Picks up cursor sets that appear in {@code config/cursorkit/} while the game is running.
 * <p>
 * The directory is fingerprinted at most once per second; when the fingerprint moves, the config
 * sources are read again and the textures this mod serves are registered anew. That is what makes
 * dropping a JSON, a PNG or a whole cursor pack into the folder work without restarting the game or
 * pressing F3+T - including while the picker is open, since the list follows
 * {@link CursorManager#generation()}.
 * <p>
 * Resource packs are deliberately not covered: adding or removing one is Minecraft's own business
 * and still needs a resource reload.
 */
public final class CursorWatcher {

    /** How long to wait between two fingerprint checks. */
    private static final long INTERVAL_MS = 1000L;

    private static long nextCheckAt;
    private static long lastSignature = Long.MIN_VALUE;

    /** Single background thread: reading packs is disk work and must not happen inside a frame. */
    private static final java.util.concurrent.ExecutorService SCANNER =
            java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "cursorkit-scan");
                thread.setDaemon(true);
                return thread;
            });

    /** True while the background thread is reading, so one change is only scanned once. */
    private static final java.util.concurrent.atomic.AtomicBoolean SCANNING =
            new java.util.concurrent.atomic.AtomicBoolean();

    /** What the background thread read, handed to the render thread on a later tick. */
    private static volatile java.util.List<com.fragmentedchaos.cursorkit.cursor.model.CursorSet> pending;

    private CursorWatcher() {
        throw new UnsupportedOperationException("CursorWatcher cannot be instantiated");
    }

    /** Called once per frame; the actual check is throttled internally. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        CursorManager manager = CursorManager.get();

        // First the hand-over: whatever the background scan read becomes visible here, on the render
        // thread, so the renderer and the picker never see a half-applied list.
        java.util.List<com.fragmentedchaos.cursorkit.cursor.model.CursorSet> ready = pending;
        if (ready != null) {
            pending = null;
            if (manager.applyLoaded(ready)) {
                Path scanned = CursorReloadListener.configDirectory(minecraft);
                if (scanned != null) {
                    CursorReloadListener.rescanTextures(minecraft, scanned);
                }
                Constants.LOG.debug("Cursor sets changed on disk, now {} set(s): {}",
                        manager.sets().size(),
                        manager.sets().stream().map(set -> set.id()).toList());
            }
        }

        long now = System.nanoTime() / 1_000_000L;
        if (now < nextCheckAt) {
            return;
        }
        nextCheckAt = now + INTERVAL_MS;

        Path directory = CursorReloadListener.configDirectory(minecraft);
        if (directory == null) {
            return;
        }
        long signature = CursorSetLoader.signature(directory);
        if (lastSignature == Long.MIN_VALUE) {
            // First look after startup: the resource reload already read everything.
            lastSignature = signature;
            return;
        }
        if (signature == lastSignature) {
            return;
        }
        // Remember the signature before scanning, so one change is submitted once - a scan that fails
        // is reported and simply picked up by the next change.
        lastSignature = signature;
        if (SCANNING.compareAndSet(false, true)) {
            SCANNER.execute(() -> {
                try {
                    pending = manager.loadFrom(directory);
                } catch (RuntimeException e) {
                    Constants.LOG.warn("Could not rescan {}: {}", directory, e.toString());
                } finally {
                    SCANNING.set(false);
                }
            });
        }
    }

    /** Called after a resource reload so the next check does not rescan the same files again. */
    static void remember(Path configDirectory) {
        lastSignature = CursorSetLoader.signature(configDirectory);
    }
}
