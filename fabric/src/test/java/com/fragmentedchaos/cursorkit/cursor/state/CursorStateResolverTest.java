package com.fragmentedchaos.cursorkit.cursor.state;

import com.fragmentedchaos.cursorkit.cursor.model.CursorContext;
import com.fragmentedchaos.cursorkit.cursor.model.CursorState;
import com.fragmentedchaos.cursorkit.cursor.model.VanillaCursor;
import com.fragmentedchaos.cursorkit.cursor.state.CursorStateResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CursorStateResolverTest {

    private static CursorContext ctx(boolean dragging, boolean busy, VanillaCursor vanilla) {
        return new CursorContext(dragging, busy, vanilla);
    }

    @Test
    void plainBackgroundIsDefault() {
        assertEquals(CursorState.DEFAULT, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.DEFAULT)));
    }

    @Test
    void vanillaRequestsMapToOurStates() {
        assertEquals(CursorState.TEXT, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.IBEAM)));
        assertEquals(CursorState.CLICKABLE, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.POINTING_HAND)));
        assertEquals(CursorState.DISABLED, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.NOT_ALLOWED)));
    }

    @Test
    void resizeCursorsCountAsClickable() {
        for (VanillaCursor cursor : new VanillaCursor[]{
                VanillaCursor.RESIZE_NS, VanillaCursor.RESIZE_EW, VanillaCursor.RESIZE_ALL}) {
            assertEquals(CursorState.CLICKABLE, CursorStateResolver.resolve(
                    ctx(false, false, cursor)), cursor.name());
        }
    }

    @Test
    void crosshairStaysDefault() {
        assertEquals(CursorState.DEFAULT, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.CROSSHAIR)));
    }

    @Test
    void busyLoadingWinsOverDisabledAndClickable() {
        assertEquals(CursorState.BUSY, CursorStateResolver.resolve(
                ctx(false, true, VanillaCursor.NOT_ALLOWED)));
        assertEquals(CursorState.BUSY, CursorStateResolver.resolve(
                ctx(false, true, VanillaCursor.POINTING_HAND)));
    }

    @Test
    void dragWinsOverEverything() {
        assertEquals(CursorState.DRAG, CursorStateResolver.resolve(
                ctx(true, true, VanillaCursor.IBEAM)));
        assertEquals(CursorState.DRAG, CursorStateResolver.resolve(
                ctx(true, false, VanillaCursor.NOT_ALLOWED)));
    }

    @Test
    void textWinsOverBusyAndBelow() {
        assertEquals(CursorState.TEXT, CursorStateResolver.resolve(
                ctx(false, true, VanillaCursor.IBEAM)));
        assertEquals(CursorState.TEXT, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.IBEAM)));
    }

    @Test
    void disabledWinsOverClickable() {
        assertEquals(CursorState.DISABLED, CursorStateResolver.resolve(
                ctx(false, false, VanillaCursor.NOT_ALLOWED)));
    }

    @Test
    void nullContextAndNullVanillaAreSafe() {
        assertEquals(CursorState.DEFAULT, CursorStateResolver.resolve(null));
        assertEquals(VanillaCursor.DEFAULT, new CursorContext(false, false, null).vanilla());
    }
}
