package ru.obolensk.afff.beetle.log;

import javax.annotation.Nonnull;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for sl4j logger
 * Created by Afff on 05.06.2016.
 */
public class Logger {

    private final org.slf4j.Logger sl4jLogger;

    public Logger(Class<?> clazz) {
        sl4jLogger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Method enables logging to system.out
     */
    public static void addConsoleAppender(@Nonnull final Level level) {
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d [%p] {%c{1}} %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(level);
        console.activateOptions();
        org.apache.log4j.Logger.getRootLogger().addAppender(console);
        org.apache.log4j.Logger.getRootLogger().setLevel(level);
    }

    public void error(String message, Object... params) {
        sl4jLogger.error(message, params);
    }

    public void error(String message, Throwable throwable) {
        sl4jLogger.error(message, throwable);
    }

    public void error(Throwable err) {
        sl4jLogger.error(err.getMessage(), err);
    }

    public void info(String message, Object... params) {
        sl4jLogger.info(message, params);
    }

    public void info(String message, Throwable throwable) {
        sl4jLogger.info(message, throwable);
    }

    public void warn(String message, Object... params) {
        sl4jLogger.warn(message, params);
    }

    public void warn(String message, Throwable throwable) {
        sl4jLogger.warn(message, throwable);
    }

    public void debug(String message, Object... params) {
        sl4jLogger.debug(message, params);
    }

    public void debug(String message, Throwable throwable) {
        sl4jLogger.debug(message, throwable);
    }

    public void debug(Throwable err) {
        sl4jLogger.debug(err.getMessage(), err);
    }

    public void trace(String message, Object... params) {
        sl4jLogger.trace(message, params);
    }

    public void trace(String message, Throwable throwable) {
        sl4jLogger.trace(message, throwable);
    }
}
