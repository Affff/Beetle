package ru.obolensk.afff.beetle.protocol;

import javax.annotation.Nonnull;

/**
 * Created by Afff on 10.04.2017.
 */
public enum HttpMethod {
    OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT, UNKNOWN;

    @Nonnull
    public static HttpMethod decode(@Nonnull final String method) {
        try {
            return HttpMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}