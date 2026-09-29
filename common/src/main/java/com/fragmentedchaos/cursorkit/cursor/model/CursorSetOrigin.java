package com.fragmentedchaos.cursorkit.cursor.model;

/**
 * Where a cursor set came from. The declaration order is also the precedence used when two sets
 * share a name: a loose file in {@code config/cursorkit/} beats a cursor pack in
 * {@code config/cursorkit/packs/}, which beats the player's own per-state files, which beat a
 * resource pack.
 */
public enum CursorSetOrigin {

    /** A PNG (and optionally a JSON) dropped into {@code config/cursorkit/}. */
    CONFIG("config"),
    /**
     * A cursor pack in {@code config/cursorkit/packs/}: a folder or a {@code .zip} using the same
     * {@code assets/<namespace>/cursor/} layout a resource pack uses. It needs no resource pack
     * screen, everything in it is always active.
     */
    CONFIG_PACK("config pack"),
    /**
     * The set the player assembles by hand in the picker: one image path per state, each pointing at
     * a file anywhere on disk. It exists only while at least one of those paths is set.
     */
    CUSTOM_STATES("custom"),
    /** A resource pack. */
    RESOURCE_PACK("resource pack");

    private final String label;

    CursorSetOrigin(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    /** @return true when this origin should win over {@code other} */
    public boolean beats(CursorSetOrigin other) {
        return ordinal() < other.ordinal();
    }
}
