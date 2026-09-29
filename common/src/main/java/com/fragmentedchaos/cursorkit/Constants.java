package com.fragmentedchaos.cursorkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants shared by every module.
 */
public final class Constants {

    public static final String MOD_ID = "cursorkit";
    public static final String MOD_NAME = "Cursor Kit";

    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Constants() {
        throw new UnsupportedOperationException("Constants cannot be instantiated");
    }
}
