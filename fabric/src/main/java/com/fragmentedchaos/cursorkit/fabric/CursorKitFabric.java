package com.fragmentedchaos.cursorkit.fabric;

import com.fragmentedchaos.cursorkit.CursorKitCommon;
import com.fragmentedchaos.cursorkit.client.CursorPlatforms;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entry point.
 * <p>
 * Named {@code ...Fabric} so it can share one jar with the NeoForge {@code @Mod} class.
 * <p>
 * Quilt runs this same entry point through its Fabric compatibility layer, so this is where the two
 * are told apart. Doing it here, before registering, keeps Quilt's classes out of the class path of
 * a plain Fabric game and keeps the shared code free of loader names.
 */
public class CursorKitFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (startedByQuilt()) {
            QuiltPlatform.register();
        } else {
            FabricPlatform.register();
        }
        CursorKitCommon.initClient();
    }

    /**
     * @return true when Quilt started this game, in which case its compatibility layer is what read
     *         {@code fabric.mod.json} and called this method.
     */
    private static boolean startedByQuilt() {
        try {
            Class.forName("org.quiltmc.loader.api.QuiltLoader", false,
                    CursorKitFabric.class.getClassLoader());
            return true;
        } catch (Throwable absent) {
            return false;
        }
    }
}
