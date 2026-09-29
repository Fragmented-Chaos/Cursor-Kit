package com.fragmentedchaos.cursorkit.mixin;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.CursorEntryPoint;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.client.gui.CursorKitScreen;
import com.fragmentedchaos.cursorkit.client.gui.FlatButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the cursor entry button into the video settings screen, whatever that screen is.
 * <p>
 * The injection sits on {@link Screen#init()} rather than on the vanilla video settings screen, so it
 * also covers the replacements other mods ship (Sodium, Embeddium, ...) without referencing them:
 * {@link CursorEntryPoint} only looks at the class name. It runs for every screen, but the check is a
 * couple of string comparisons.
 * <p>
 * A floating button is the fallback only: when Sodium is around, {@code CursorSodiumConfig} gives the
 * entry a page in Sodium's own option list, which then also removes the need for the button on
 * Sodium's screens.
 */
@Mixin(Screen.class)
public abstract class ScreenEntryMixin {

    @Shadow
    protected abstract GuiEventListener addRenderableWidget(GuiEventListener widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void cursorkit$addEntryButton(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        String className = screen.getClass().getName();
        if (screen instanceof net.minecraft.client.gui.screens.options.VideoSettingsScreen) {
            // Handled by VideoSettingsScreenMixin, which adds a proper option row.
            return;
        }
        if (CursorEntryPoint.isSodiumLinked() && CursorEntryPoint.isSodiumScreen(className)) {
            // Sodium lists the cursor picker as a page of its own.
            return;
        }
        if (!CursorEntryPoint.isVideoSettingsScreen(className)) {
            return;
        }
        Constants.LOG.debug("Adding the cursor entry to {}", className);
        addRenderableWidget(new FlatButton(8, screen.height - 27, 80, 20,
                CursorTranslations.get("cursorkit.button.open", "Cursor"),
                button -> Minecraft.getInstance().setScreenAndShow(new CursorKitScreen(screen))));
    }
}
