package ru.obolensk.afff.beetle.protocol;

import javax.annotation.Nonnull;

import lombok.Getter;

/**
 * Created by Afff on 11.04.2017.
 */
public enum HttpHeaderValue {
    CONNECTION_KEEP_ALIVE("keep-alive"),
    CONNECTION_CLOSE("close"),
    CONTENT_DISPOSITION_FORM_DATA("form-data"),
    CONTENT_DISPOSITION_FILE("file")
    ;

    @Nonnull @Getter
    private final String name;

    HttpHeaderValue(final String name) {
        this.name = name.toLowerCase();
    }
}
