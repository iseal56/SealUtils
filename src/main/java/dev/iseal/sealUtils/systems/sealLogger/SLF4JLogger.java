package dev.iseal.sealUtils.systems.sealLogger;

import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.SealUtils;
import org.slf4j.Logger;

/**
 * An SLF4J-based implementation of SealLogger.
 * This class is loaded only if SLF4J is on the classpath.
 */
public class SLF4JLogger implements SealLogger {
    private final Logger logger;

    public SLF4JLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    @Override
    public void debug(String msg) {
        if (SealUtils.isDebug()) {
            logger.debug(msg);
        }
    }

    @Override
    public void log(java.util.logging.Level level, String msg) {
        switch (level.getName()) {
            case "SEVERE":
                logger.error(msg);
                break;
            case "WARNING":
                logger.warn(msg);
                break;
            case "INFO":
                logger.info(msg);
                break;
            case "FINE":
            case "FINER":
            case "FINEST":
                if (SealUtils.isDebug()) {
                    logger.debug(msg);
                }
                break;
            default:
                logger.info(msg); // Default to info for unknown levels
        }
    }
}