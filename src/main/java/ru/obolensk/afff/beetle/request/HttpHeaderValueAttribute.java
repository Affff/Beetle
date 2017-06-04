package ru.obolensk.afff.beetle.request;

import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Created by Afff on 25.05.2017.
 */
public enum HttpHeaderValueAttribute {
    BOUNDARY("boundary"),
    NAME("name"),
    ;

    @Nonnull
    @Getter
    private final String name;

    HttpHeaderValueAttribute(final String name) {
        this.name = name.toLowerCase();
    }
}
