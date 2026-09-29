package com.fragmentedchaos.cursorkit.mixin;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import com.fragmentedchaos.cursorkit.client.gui.CursorKitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the cursor entry as a normal option row in the vanilla video settings screen.
 * <p>
 * The screen fills its list with the sections display, quality and preferences in that order, so a
 * row appended at the end of {@code addOptions} lands in the preferences section where a display
 * setting belongs - rather than floating over the screen.
 * <p>
 * Screens that replace the vanilla one (Sodium and friends build their own list) are handled by
 * {@link ScreenEntryMixin} instead, which draws a plain button because their internals cannot be
 * extended generically.
 * <p>
 * The mixin extends {@link OptionsSubScreen} because {@code list} lives there: a shadow of an
 * inherited field is not resolved by Mixin.
 */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {

    /** Only there to satisfy the compiler: mixins never call the superclass constructor. */
    private VideoSettingsScreenMixin() {
        super(null, null, null);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void cursorkit$addEntryRow(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        Constants.LOG.info("Adding the cursor entry row to {}", self.getClass().getName());
        // A vanilla button on purpose: this row sits among the screen's own options, so it should
        // look like them rather than like the picker's flat style.
        this.list.addBig(Button
                .builder(CursorTranslations.get("cursorkit.button.open", "Cursor"),
                        button -> Minecraft.getInstance().setScreenAndShow(new CursorKitScreen(self)))
                .bounds(0, 0, 200, 20)
                .build());
    }
}
