package dev.iseal.sealUtils;

import java.util.logging.Logger;

public class SealUtils {

    private static boolean debugMode = false;
    private static Logger log = Logger.getLogger("SealUtils");

    /**
     * Initializes the SealUtils library.
     *
     * @param debugM true to enable debug mode, false to disable it.
     */
    public static void init(boolean debugM) {
        debugMode = debugM;
    }

    /**
     * Sets the debug mode.
     * @param debugM true to enable debug mode, false to disable it.
     */
    public static void setDebug(boolean debugM) {
        debugMode = debugM;
    }

    /**
     * Checks if the debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise.
     */
    public static boolean isDebug() {
        return debugMode;
    }

    /**
     * Sets the logger for the SealUtils library.
     * @param logger the logger to set.
     */
    public static void setLogger(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        log = logger;
    }

    /**
     * Gets the logger for the SealUtils library.
     *
     * @return the logger.
     */
    public static Logger getLogger() {
        return log;
    }
}
