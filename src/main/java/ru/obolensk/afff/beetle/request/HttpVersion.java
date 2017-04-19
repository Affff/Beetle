package ru.obolensk.afff.beetle.request;

import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Created by Afff on 10.04.2017.
 */
public enum HttpVersion {
    HTTP_1_0("HTTP/1.0"), HTTP_1_1("HTTP/1.1"), HTTP_2("HTTP/2"), UNKNOWN(("HTTP/1.1")); // HTTP 0.9 unsupported as deprecated

    @Getter
    private final String name;

    HttpVersion(@Nonnull final String name) {
        this.name = name;
    }

    @Nonnull
    public static HttpVersion decode(@Nonnull final String method) {
        try {
            return HttpVersion.valueOf(method.replace('/', '_').replace('.', '_'));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
