package com.fragmentedchaos.cursorkit.mixin;

import com.fragmentedchaos.cursorkit.client.render.CursorRenderer;
import com.fragmentedchaos.cursorkit.client.CursorStateTracker;
import com.fragmentedchaos.cursorkit.client.CursorWatcher;
import com.fragmentedchaos.cursorkit.client.VanillaCursorMapper;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the cursor vanilla decided on and draws this mod's cursor at the right layer.
 * <p>
 * Every widget requests its cursor through {@code requestCursor}; {@code pendingCursor} holds the
 * final answer, so reading it gives us the state - including for screens added by other mods.
 * <p>
 * The icon is drawn in {@code extractDeferredElements}, which the screen calls right after its
 * widgets and <em>before</em> it opens the strata for the hover tooltip: the cursor therefore sits
 * above the HUD and the screen but underneath the tooltip. Drawing at the very end of the frame
 * instead ({@code applyCursor}) put the cursor into the tooltip's own stratum, where it covered item
 * tooltips.
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @Shadow
    private CursorType pendingCursor;

    @Inject(method = "extractDeferredElements", at = @At("HEAD"))
    private void cursorkit$drawCursor(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        CursorStateTracker.setRequested(VanillaCursorMapper.map(this.pendingCursor));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            CursorRenderer.render((GuiGraphicsExtractor) (Object) this, minecraft.getWindow());
        }
    }

    @Inject(method = "applyCursor", at = @At("HEAD"))
    private void cursorkit$captureRequestedCursor(Window window, CallbackInfo ci) {
        CursorStateTracker.setRequested(VanillaCursorMapper.map(this.pendingCursor));
    }

    /**
     * Runs once per frame whether or not a screen is open: it keeps the config watcher alive and
     * hands the system cursor back as soon as nothing of ours is drawn.
     */
    @Inject(method = "applyCursor", at = @At("TAIL"))
    private void cursorkit$tick(Window window, CallbackInfo ci) {
        CursorWatcher.tick();
        CursorRenderer.updateVisibility(window);
    }
}
