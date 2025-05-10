package dev.iseal.sealUtils;

public class SealUtils {

    private static boolean debugMode = false;

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
}
