package ru.obolensk.afff.beetle.protocol;

import javax.annotation.Nonnull;

import lombok.Getter;

/**
 * Created by Afff on 25.05.2017.
 */
public enum HttpHeaderValueAttribute {
    BOUNDARY("boundary"),
    NAME("name"),
    FILENAME("filename")
    ;

    @Nonnull
    @Getter
    private final String name;

    HttpHeaderValueAttribute(final String name) {
        this.name = name.toLowerCase();
    }
}
