package ru.obolensk.afff.beetle.settings;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.nio.file.Paths;

/**
 * Created by Afff on 20.04.2017.
 * WARNING! Currently null value isn't supported to use in config.
 */
public enum Options {

    LOG_TO_CONSOLE(true),
    WWW_ROOT_DIR(Paths.get("D:/Beetle/www")),
    WELCOME_FILE_NAME("index.html"),
    SERVER_THREAD_COUNT(5),
    REQUEST_MAX_LINE_LENGHT(8192)
    ;

    Options(@Nonnull final Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Getter
    @Nonnull
    private final Object defaultValue;
}
