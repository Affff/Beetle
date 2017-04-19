package ru.obolensk.afff.beetle.request;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by Afff on 10.04.2017.
 */
public enum HttpHeader {
    DATE("Date"),
    SERVER("Server"),
    CONTENT_TYPE("Content-Type"),
    CONTENT_LENGTH("Content-Length"),
    CONNECTION("Connection"),
    ALLOW("Allow");

    @Nonnull @Getter
    private final String name;

    @Nonnull @Getter
    private final String lowStrName;

    HttpHeader(final String name) {
        this.name = name;
        this.lowStrName = name.toLowerCase();
    }

    @Nullable
    public static HttpHeader getByName(@Nonnull final String name) {
        for (HttpHeader header : values()) {
            if (header.getLowStrName().equals(name)) {
                return header;
            }
        }
        return null;
    }
}