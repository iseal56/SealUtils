package dev.iseal.sealUtils;

import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.systems.sealLogger.JavaUtilLogger;
import dev.iseal.sealUtils.systems.sealLogger.SLF4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SealUtils {

    private static boolean debugMode = false;
    private static SealLogger log;

    static {
        try {
            // Check if SLF4J is available
            Class.forName("org.slf4j.Logger");
            log = new SLF4JLogger(LoggerFactory.getLogger(SealUtils.class));
        } catch (ClassNotFoundException e) {
            // Fallback to java.util.logging
            log = new JavaUtilLogger(java.util.logging.Logger.getLogger(SealUtils.class.getName()));
        }
    }

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
    public static void setLogger(java.util.logging.Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        log = new JavaUtilLogger(logger);
    }

    /**
     * Sets the logger for the SealUtils library.
     * @param logger the logger to set.
     */
    public static void setLogger(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        log = new SLF4JLogger(logger);
    }

    /**
     * Gets the logger for the SealUtils library.
     *
     * @return the logger.
     */
    public static SealLogger getLogger() {
        return log;
    }
}
