package ru.obolensk.afff.beetle.request;

import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Created by Afff on 11.04.2017.
 */
public enum HttpHeaderValue {
    CONNECTION_KEEP_ALIVE("keep-alive"),
    CONNECTION_CLOSE("close");

    @Nonnull @Getter
    private final String name;

    HttpHeaderValue(final String name) {
        this.name = name;
    }
}
