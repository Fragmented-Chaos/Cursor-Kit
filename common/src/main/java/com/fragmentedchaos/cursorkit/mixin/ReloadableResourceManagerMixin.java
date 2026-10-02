package com.fragmentedchaos.cursorkit.mixin;

import com.fragmentedchaos.cursorkit.client.CursorPlatforms;
import com.fragmentedchaos.cursorkit.client.CursorReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the cursor set reload listener as soon as the client's resource manager exists.
 * <p>
 * Registering later (for example at the end of {@code Minecraft}'s constructor) is too late: the
 * initial reload has already collected its listener list by then, so our first load would be
 * skipped.
 * <p>
 * Only {@link PackType#CLIENT_RESOURCES} is hooked; the integrated server's data manager does not
 * need cursor sets.
 * <p>
 * On NeoForge this is skipped entirely: NeoForge refuses mod listeners that reach the resource
 * manager outside its own {@code AddClientReloadListenersEvent}, so that side registers through the
 * event instead (see the NeoForge entry point). Fabric and Quilt have no such event, and going
 * through vanilla's resource manager keeps this free of Fabric API - which Quilt would otherwise
 * demand QFAPI for.
 * <p>
 * The loader is asked through {@link CursorPlatforms#isNeoForge()} rather than through a registered
 * platform on purpose: this constructor runs before the entry points that register one, so there is
 * no platform to ask yet. A loader that would refuse this registration refuses it from the moment it
 * starts, which is what makes a marker class a sound answer this early.
 */
@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cursorkit$registerCursorReloadListener(PackType type, CallbackInfo ci) {
        if (type == PackType.CLIENT_RESOURCES && !CursorPlatforms.isNeoForge()) {
            ((ReloadableResourceManager) (Object) this)
                    .registerReloadListener(CursorReloadListener.INSTANCE);
        }
    }
}
