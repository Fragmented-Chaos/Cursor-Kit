package com.fragmentedchaos.cursorkit.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader facts shown in the about popup, and who is allowed to publish them.
 * <p>
 * The point of this class is the fallback: when no loader module registered a platform the popup
 * must say so rather than name a loader nobody checked for.
 */
class CursorPlatformsTest {

    @AfterEach
    void forgetTheRegisteredPlatform() {
        CursorPlatforms.resetForTests();
    }

    @Test
    void aLoaderWithNoPlatformIsReportedAsUnknown() {
        assertEquals("Unknown", CursorPlatforms.loaderName());
        assertEquals("", CursorPlatforms.loaderVersion());
        assertEquals(Optional.empty(), CursorPlatforms.get());
    }

    @Test
    void theRegisteredPlatformSuppliesTheNameAndVersion() {
        CursorPlatforms.register(platform("NeoForge", "26.3.0.12-beta"));

        assertEquals("NeoForge", CursorPlatforms.loaderName());
        assertEquals("26.3.0.12-beta", CursorPlatforms.loaderVersion());
    }

    @Test
    void theFirstRegistrationWins() {
        // A compatibility layer can forward to another loader's entry point, so a second loader must
        // not be able to rename the one that actually started the game.
        CursorPlatforms.register(platform("Quilt Loader", "0.30.0-beta.7"));
        CursorPlatforms.register(platform("Fabric Loader", "0.19.5"));

        assertEquals("Quilt Loader", CursorPlatforms.loaderName());
        assertEquals("0.30.0-beta.7", CursorPlatforms.loaderVersion());
    }

    @Test
    void nothingIsPublishedFromNothing() {
        CursorPlatforms.register(null);

        assertTrue(CursorPlatforms.get().isEmpty());
    }

    @Test
    void theLoaderNameIsEmptyRatherThanMissingWhenAModuleCouldNotReadOne() {
        CursorPlatforms.register(platform("", ""));

        assertEquals("", CursorPlatforms.loaderName());
        assertEquals("", CursorPlatforms.loaderVersion());
    }

    @Test
    void fabricIsNotNeoForge() {
        // The mixin has to know this before any platform is registered, so it is answered from a
        // marker class. On this JVM there is no NeoForge on the class path.
        assertFalse(CursorPlatforms.isNeoForge());
    }

    @Test
    void theAboutPopupShowsTheVersionAfterTheName() {
        CursorPlatforms.register(platform("Fabric Loader", "0.19.5"));

        assertEquals("Fabric Loader 0.19.5", CursorPlatforms.loaderDisplayName());
    }

    @Test
    void aLoaderWithoutAVersionIsNamedAlone() {
        CursorPlatforms.register(platform("Quilt Loader", ""));

        assertEquals("Quilt Loader", CursorPlatforms.loaderDisplayName());
    }

    @Test
    void anUnknownLoaderDoesNotGetATrailingGap() {
        // A version could still be registered by something, so the pair is made explicit here.
        CursorPlatforms.register(platform(CursorPlatforms.UNKNOWN_LOADER, "1.2.3"));

        assertEquals(CursorPlatforms.UNKNOWN_LOADER, CursorPlatforms.loaderDisplayName());
    }

    @Test
    void noPlatformAtAllNamesNoLoaderInThePopup() {
        assertEquals(CursorPlatforms.UNKNOWN_LOADER, CursorPlatforms.loaderDisplayName());
    }

    private static CursorPlatform platform(String name, String version) {
        return new CursorPlatform() {
            @Override
            public String loaderName() {
                return name;
            }

            @Override
            public String loaderVersion() {
                return version;
            }
        };
    }
}
