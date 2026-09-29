package com.fragmentedchaos.cursorkit.mixin;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.render.CursorClickAnimation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds mouse presses to {@link CursorClickAnimation}, which is the only way the renderer can know
 * that the player just clicked.
 * <p>
 * Minecraft reports the action as an int: 1 is a press, 0 a release (see
 * {@code MouseHandler.onButton}). Only presses matter here; the animation decays on its own.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void cursorkit$clickAnimation(long handle, MouseButtonInfo info, int action,
                                          CallbackInfo ci) {
        if (action == 1) {
            CursorClickAnimation.press();
            Constants.LOG.debug("Mouse button {} pressed at {}", info.button(),
                    System.nanoTime() / 1_000_000L);
        }
    }
}
