package com.fragmentedchaos.cursorkit.client.gui;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.client.CursorPlatforms;
import com.fragmentedchaos.cursorkit.client.CursorTranslations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The about popup: what this mod is, which build it is, and where its files live.
 * <p>
 * A screen of its own rather than a panel inside the picker, because the picker's details area is
 * for the selected set - covering it with text made the screen look broken instead of informative.
 * Closing returns to the picker it was opened from.
 */
public class AboutScreen extends Screen {

    private static final int PANEL_WIDTH = 276;
    private static final int PANEL_HEIGHT = 152;
    private static final int LINE_HEIGHT = 14;
    // Deliberately translucent: the popup should read as a layer over the game, not a solid box.
    private static final int PANEL_FILL = 0xB01B1B1F;
    private static final int PANEL_BORDER = 0x38FFFFFF;
    private static final int TEXT = 0xFFDDDDDD;
    private static final int TEXT_DIM = 0xFF8899AA;

    /** The three places a player may want to go from here. */
    private static final String[] LINK_URLS = {
            "https://github.com/Fragmented-Chaos/Cursor-Kit",
            "https://fragmented-chaos.github.io/mods/cursorkit/",
            "https://github.com/Fragmented-Chaos/Cursor-Kit/issues",
    };
    /** Language keys and fallbacks of those buttons, in the same order. */
    private static final String[] LINK_KEYS = {
            "cursorkit.about.source", "cursorkit.about.home", "cursorkit.about.issues",
    };
    private static final String[] LINK_FALLBACKS = {"Source", "Website", "Issues"};

    @Nullable
    private final Screen parent;

    public AboutScreen(@Nullable Screen parent) {
        super(CursorTranslations.get("cursorkit.about.title", "About Cursor Kit"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        // Three links in a row, then the close button under them. They open in the desktop browser,
        // the same way the picker's folder button opens the config directory.
        int linkWidth = (PANEL_WIDTH - 28 - 12) / 3;
        for (int index = 0; index < LINK_URLS.length; index++) {
            String url = LINK_URLS[index];
            addRenderableWidget(new FlatButton(left + 14 + index * (linkWidth + 6),
                    top + PANEL_HEIGHT - 50, linkWidth, 18,
                    CursorTranslations.get(LINK_KEYS[index], LINK_FALLBACKS[index]),
                    button -> openLink(url)));
        }
        addRenderableWidget(new FlatButton(left + 14, top + PANEL_HEIGHT - 26, PANEL_WIDTH - 28, 18,
                CursorTranslations.get("cursorkit.about.close", "Close"), button -> onClose()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                   float partialTick) {
        // Order matters: the panel and its text go down first, the widgets last, otherwise the panel
        // paints over the buttons and they end up hidden behind it.
        // The popup floats over whatever was behind it: dimming first is what makes it read as a
        // dialog instead of text someone dropped on the world.
        extractor.fill(0, 0, this.width, this.height, 0xC0000000);

        int left = panelLeft();
        int top = panelTop();
        FlatButton.roundedRect(extractor, left - 1, top - 1, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, 7,
                PANEL_BORDER);
        FlatButton.roundedRect(extractor, left, top, PANEL_WIDTH, PANEL_HEIGHT, 6, PANEL_FILL);

        String version = version();

        // A solid title bar, like the picker's own header, so the popup reads as part of this mod
        // rather than as a bare text box someone dropped on the game.
        FlatButton.roundedRect(extractor, left, top, PANEL_WIDTH, 24, 6, 0xFF2A2A31);
        extractor.fill(left, top + 18, left + PANEL_WIDTH, top + 24, 0xFF2A2A31);
        extractor.fill(left, top + 24, left + PANEL_WIDTH, top + 25, PANEL_BORDER);
        extractor.text(this.font, getTitle(), left + 14, top + 8, 0xFFFFFFFF);

        int y = top + 34;
        for (Component[] row : rows(version)) {
            y = field(extractor, left, y, row[0], row[1]);
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    /**
     * The rows of the popup, in display order, as {label, value}.
     * <p>
     * Two of the values are facts read at runtime - the loader and this build - so they are carried
     * as text rather than as translation keys of their own; the other two are translated.
     */
    private static Component[][] rows(String version) {
        return new Component[][] {
                label("cursorkit.about.version_label", "Version", version),
                label("cursorkit.about.loaders_label", "Loader", CursorPlatforms.loaderDisplayName()),
                row("cursorkit.about.author_label", "Author", "cursorkit.about.author",
                        "Fragmented_Chaos"),
                row("cursorkit.about.licence_label", "Licence", "cursorkit.about.licence",
                        "LGPL-3.0"),
        };
    }

    /** A row whose value is text the game cannot translate, such as a version number. */
    private static Component[] label(String labelKey, String labelFallback, String value) {
        return new Component[] {
                CursorTranslations.get(labelKey, labelFallback),
                Component.literal(value),
        };
    }

    /** A row whose value is itself a translation key. */
    private static Component[] row(String labelKey, String labelFallback, String valueKey,
                                   String valueFallback) {
        return new Component[] {
                CursorTranslations.get(labelKey, labelFallback),
                CursorTranslations.get(valueKey, valueFallback),
        };
    }

    /**
     * One "label: value" row: the label in a fixed column so the values line up, which is what makes
     * a list of short facts readable at a glance.
     */
    private int field(GuiGraphicsExtractor extractor, int left, int y, Component label,
                      Component value) {
        extractor.text(this.font, label, left + 14, y, TEXT_DIM);
        int valueX = left + 74;
        String fitted = this.font.plainSubstrByWidth(value.getString(),
                left + PANEL_WIDTH - 14 - valueX);
        extractor.text(this.font, Component.literal(fitted), valueX, y, TEXT);
        return y + LINE_HEIGHT;
    }

    /**
     * The version this build was made from.
     * <p>
     * Read from the version file the build stamps into the jar rather than from the manifest, because
     * a development run has no manifest attributes and showed "dev" instead of a version.
     */
    private static String version() {
        try (java.io.InputStream in = AboutScreen.class.getResourceAsStream("/cursorkit-version.txt")) {
            if (in != null) {
                String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!text.isEmpty() && !text.startsWith("${")) {
                    return text;
                }
            }
        } catch (java.io.IOException ignored) {
            // Fall through to the manifest, then to a placeholder.
        }
        Package mod = AboutScreen.class.getPackage();
        return mod != null && mod.getImplementationVersion() != null
                ? mod.getImplementationVersion() : "dev";
    }

    /**
     * Hands a link to the desktop browser.
     * <p>
     * Two routes on purpose: {@code Desktop} is unavailable in some launchers and sandboxes (it just
     * reports "not supported"), and the OS command always exists on a desktop install. The first one
     * that works wins; if neither does, the log says so instead of failing silently.
     */
    private void openLink(String url) {
        if (browseWithDesktop(url) || browseWithCommand(url)) {
            return;
        }
        Constants.LOG.warn("Could not open {}: no desktop handler available", url);
    }

    private boolean browseWithDesktop(String url) {
        try {
            if (!java.awt.Desktop.isDesktopSupported()) {
                return false;
            }
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(java.net.URI.create(url));
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            Constants.LOG.debug("Desktop could not open {}: {}", url, e.toString());
            return false;
        }
    }

    private boolean browseWithCommand(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        String[] command;
        if (os.contains("win")) {
            command = new String[] {"rundll32", "url.dll,FileProtocolHandler", url};
        } else if (os.contains("mac")) {
            command = new String[] {"open", url};
        } else {
            command = new String[] {"xdg-open", url};
        }
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            Constants.LOG.debug("{} could not open {}: {}", command[0], url, e.toString());
            return false;
        }
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void onClose() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreenAndShow(this.parent);
        }
    }
}
