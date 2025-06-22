package dev.iseal.sealUtils.systems.sealLogger;

import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.SealUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A java.util.logging-based implementation of SealLogger.
 * This class is used as a fallback when SLF4J is not available.
 */
public class JavaUtilLogger implements SealLogger {
    private final Logger logger;

    public JavaUtilLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warning(msg);
    }

    @Override
    public void error(String msg) {
        logger.severe(msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    @Override
    public void debug(String msg) {
        if (SealUtils.isDebug()) {
            logger.fine(msg);
        }
    }

    @Override
    public void log(Level level, String msg) {
        logger.log(level, msg);
    }
}