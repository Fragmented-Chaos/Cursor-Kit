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

    private CursorWatcher() {
        throw new UnsupportedOperationException("CursorWatcher cannot be instantiated");
    }

    /** Called once per frame; the actual check is throttled internally. */
    public static void tick() {
        long now = System.nanoTime() / 1_000_000L;
        if (now < nextCheckAt) {
            return;
        }
        nextCheckAt = now + INTERVAL_MS;

        Minecraft minecraft = Minecraft.getInstance();
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
        lastSignature = signature;
        refresh(minecraft, directory);
    }

    /** Called after a resource reload so the next check does not rescan the same files again. */
    static void remember(Path configDirectory) {
        lastSignature = CursorSetLoader.signature(configDirectory);
    }

    private static void refresh(Minecraft minecraft, Path directory) {
        CursorManager manager = CursorManager.get();
        if (!manager.rescan(directory)) {
            return;
        }
        CursorReloadListener.rescanTextures(minecraft, directory);
        Constants.LOG.info("Cursor sets changed on disk, now {} set(s): {}", manager.sets().size(),
                manager.sets().stream().map(set -> set.id()).toList());
    }
}
