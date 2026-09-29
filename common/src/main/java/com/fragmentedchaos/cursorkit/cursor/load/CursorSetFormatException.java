package com.fragmentedchaos.cursorkit.cursor.load;

/**
 * Thrown when a cursor set file cannot be understood. The loader catches it per file, logs the
 * message and skips that single set, so one broken cursor set never breaks the mod or other sets.
 */
public class CursorSetFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public CursorSetFormatException(String message) {
        super(message);
    }

    public CursorSetFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
