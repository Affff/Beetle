package ru.obolensk.afff.beetle.settings;

import java.nio.file.Paths;

import javax.annotation.Nonnull;

import lombok.Getter;
import org.apache.log4j.Level;

/**
 * Created by Afff on 20.04.2017.
 * WARNING! Currently null value isn't supported to use in config.
 */
public enum Options {

    LOG_TO_CONSOLE(true),
    LOG_LEVEL(Level.INFO),
    ROOT_DIR(Paths.get("c:/beetle")),
    WWW_DIR(Paths.get("www")),
    TEMP_DIR(Paths.get("temp")),
    SERVLETS_ENABLED(false),
    SERVLET_DIR(Paths.get("srv")),
    SERVLET_REFRESH_FILES_SERVICE_ENABLED(false),
    SERVLET_REFRESH_FILES_SERVICE_INTERVAL(60000),
    WELCOME_FILE_NAME("index.html"),
    SERVER_THREAD_COUNT(5),
    REQUEST_MAX_LINE_LENGTH(8192)
    ;

    Options(@Nonnull final Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Getter
    @Nonnull
    private final Object defaultValue;
}
