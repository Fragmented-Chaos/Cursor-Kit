package com.fragmentedchaos.cursorkit.fabric;

import com.fragmentedchaos.cursorkit.CursorKitCommon;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entry point.
 * <p>
 * Named {@code ...Fabric} so it can share one jar with the NeoForge {@code @Mod} class.
 */
public class CursorKitFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CursorKitCommon.initClient();
    }
}
