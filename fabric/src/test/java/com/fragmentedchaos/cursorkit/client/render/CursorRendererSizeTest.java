package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.client.render.CursorClickAnimation;
import com.fragmentedchaos.cursorkit.client.render.CursorRenderer;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.fragmentedchaos.cursorkit.cursor.model.ClickEffect;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cursor is drawn at a whole multiple of its own pixels, the way the system pointer is drawn: any
 * other ratio means the GPU resamples the image and the cursor stops looking sharp.
 */
class CursorRendererSizeTest {

    @Test
    void aCursorKeepsItsPhysicalSizeWhateverTheWindowIs() {
        // One image pixel is one screen pixel, so the drawn size must not grow with the GUI scale:
        // the same cursor has to stay the same size when the window is enlarged.
        for (int frameSize : new int[] {16, 24, 32, 48, 64, 128}) {
            for (int guiScale : new int[] {1, 2, 3, 4}) {
                int size = CursorRenderer.scaledSize(frameSize, 1, guiScale);
                int physical = size * guiScale;

                assertTrue(Math.abs(physical - frameSize) <= 6 * guiScale,
                        "frameSize=" + frameSize + " guiScale=" + guiScale + " drew " + physical
                                + " physical pixels instead of about " + frameSize);
            }
        }
    }

    @Test
    void aHighResolutionImageIsDrawnPixelForPixelWhenItFits() {
        // The image's own pixels are the screen's pixels, whatever the GUI scale is.
        assertEquals(32, CursorRenderer.scaledSize(32, 1, 1));   // 32*1 = 32 physical, 1:1
        assertEquals(16, CursorRenderer.scaledSize(32, 1, 2));   // 16*2 = 32 physical, 1:1
        assertEquals(16, CursorRenderer.scaledSize(64, 1, 4));   // 16*4 = 64 physical, 1:1
    }

    @Test
    void extremeSizesStillLandOnAnExactMultiple() {
        // Small art, huge art and a big multiplier: the drawn size must stay pixel exact, because a
        // fractional ratio is what makes an HD cursor look blurry.
        for (int frameSize : new int[] {1, 8, 1024}) {
            for (int wanted : new int[] {1, 4}) {
                for (int guiScale : new int[] {1, 4, 8}) {
                    int size = CursorRenderer.scaledSize(frameSize, wanted, guiScale);
                    int physical = size * guiScale;
                    assertTrue(physical % frameSize == 0 || frameSize % physical == 0,
                            "frameSize=" + frameSize + " wanted=" + wanted + " guiScale=" + guiScale
                                    + " drew " + physical + " physical pixels, which is not an exact"
                                    + " multiple of the image");
                }
            }
        }
    }

    @Test
    void theMultiplierStillScales() {
        assertEquals(CursorRenderer.scaledSize(32, 1, 2) * 2, CursorRenderer.scaledSize(32, 2, 2));
    }

    @Test
    void theDrawnSizeIsAlwaysUsable() {
        for (int frameSize : new int[] {1, 8, 16, 24, 32, 48, 64, 128, 256}) {
            for (int guiScale : new int[] {1, 2, 3, 4}) {
                for (int wanted : new int[] {1, 2, 3}) {
                    int size = CursorRenderer.scaledSize(frameSize, wanted, guiScale);
                    assertTrue(size >= 1, "frameSize=" + frameSize + " guiScale=" + guiScale);
                    assertTrue(size <= frameSize * wanted + 6,
                            "frameSize=" + frameSize + " size=" + size);
                }
            }
        }
    }

    @Test
    void withAnimationOffTheCursorIsAlwaysTheArrow() {
        for (CursorState state : CursorState.values()) {
            assertEquals(CursorState.DEFAULT, CursorRenderer.drawnState(state, false),
                    "animation off has to leave the plain arrow, whatever the game asks for");
            assertEquals(state, CursorRenderer.drawnState(state, true));
        }
    }

    @Test
    void animationOffFreezesAnAnimatedImageOnItsFirstFrame() {
        CursorImage strip = new CursorImage("frame0.png", 0, 0, 4, 100);

        assertEquals(0, CursorRenderer.frameFor(strip, false, 0));
        assertEquals(0, CursorRenderer.frameFor(strip, false, 250));
        assertEquals(2, CursorRenderer.frameFor(strip, true, 250),
                "with animation on the strip keeps stepping through its frames");
    }

    @Test
    void theClickRippleExpandsAndFades() {
        ClickEffect spec = ClickEffect.DEFAULT;
        CursorClickAnimation.reset();
        CursorClickAnimation.pressAt(1_000L);
        CursorClickAnimation.tick(10.0D, 20.0D, 1_000L, spec);

        List<CursorClickAnimation.Effect> fresh = CursorClickAnimation.effects(1_000L);
        assertEquals(1, fresh.size(), "a press leaves exactly one ripple");
        assertEquals(spec, fresh.get(0).spec(), "and it remembers which set spawned it");

        float early = CursorClickAnimation.radius(spec, CursorClickAnimation.progress(fresh.get(0), 1_050L));
        float later = CursorClickAnimation.radius(spec, CursorClickAnimation.progress(fresh.get(0), 1_300L));
        assertTrue(later > early, "the ring keeps expanding: " + early + " -> " + later);

        assertTrue(CursorClickAnimation.alpha(spec, 0.0F) > CursorClickAnimation.alpha(spec, 0.8F));
        assertEquals(0, CursorClickAnimation.alpha(spec, 1.0F), 0);
        assertTrue(CursorClickAnimation.effects(1_000L + spec.durationMs()).isEmpty(),
                "and it is gone once its time is up");
    }

    @Test
    void onePressLeavesOneRipple() {
        CursorClickAnimation.reset();
        CursorClickAnimation.pressAt(2_000L);

        // The renderer calls this every frame; a press must not spawn a ripple per frame.
        CursorClickAnimation.tick(1.0D, 1.0D, 2_000L, ClickEffect.DEFAULT);
        CursorClickAnimation.tick(2.0D, 2.0D, 2_010L, ClickEffect.DEFAULT);
        CursorClickAnimation.tick(3.0D, 3.0D, 2_020L, ClickEffect.DEFAULT);

        assertEquals(1, CursorClickAnimation.effects(2_020L).size());
        CursorClickAnimation.reset();
    }

    @Test
    void aSetCanAskForNoEffect() {
        CursorClickAnimation.reset();
        CursorClickAnimation.pressAt(3_000L);
        CursorClickAnimation.tick(1.0D, 1.0D, 3_000L, ClickEffect.NONE);

        assertTrue(CursorClickAnimation.effects(3_000L).isEmpty(),
                "\"type\": \"none\" means exactly that");
        CursorClickAnimation.reset();
    }

    @Test
    void theEffectFollowsTheSetsOwnSize() {
        ClickEffect big = new ClickEffect("ripple", 0xFFFFFF, 30.0F, 450L, 6, "", 1, 60, 32);
        ClickEffect small = new ClickEffect("ripple", 0xFFFFFF, 15.0F, 450L, 6, "", 1, 60, 32);

        assertEquals(2.0F, CursorClickAnimation.radius(big, 0.5F)
                / CursorClickAnimation.radius(small, 0.5F), 0.001F);
    }
}
