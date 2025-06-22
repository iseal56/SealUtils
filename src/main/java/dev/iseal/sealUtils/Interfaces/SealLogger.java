package dev.iseal.sealUtils.Interfaces;

import java.util.logging.Level;

/**
 * A simple logger interface to abstract away the underlying logging framework.
 */
public interface SealLogger {
    void info(String msg);
    void warn(String msg);
    void error(String msg);
    void error(String msg, Throwable t);
    void debug(String msg);
    void log(Level level, String msg);
}
