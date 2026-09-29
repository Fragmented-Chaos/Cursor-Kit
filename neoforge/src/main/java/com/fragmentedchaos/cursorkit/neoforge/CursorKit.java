package com.fragmentedchaos.cursorkit.neoforge;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.CursorKitCommon;
import com.fragmentedchaos.cursorkit.client.CursorReloadListener;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/**
 * NeoForge client entry point. This is a client-only mod.
 */
@Mod(value = "cursorkit", dist = Dist.CLIENT)
public class CursorKit {

    public CursorKit(IEventBus eventBus) {
        CursorKitCommon.initClient();
        // NeoForge only accepts mod reload listeners through this event, so the shared mixin path is
        // disabled on this loader (see CursorLoaderPlatform).
        eventBus.addListener(CursorKit::onAddClientReloadListeners);
    }

    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "cursor_sets"),
                CursorReloadListener.INSTANCE);
    }
}
