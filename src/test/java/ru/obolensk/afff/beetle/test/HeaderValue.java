package ru.obolensk.afff.beetle.test;

import javax.annotation.Nonnull;

import ru.obolensk.afff.beetle.protocol.HttpHeader;
import ru.obolensk.afff.beetle.protocol.HttpHeaderValue;

/**
 * Created by Afff on 03.05.2017.
 */
public class HeaderValue {

    private final HttpHeader header;
    private final String value;

    public HeaderValue(@Nonnull final HttpHeader header, @Nonnull final HttpHeaderValue value) {
        this(header, value.getName());
    }

    private HeaderValue(@Nonnull final HttpHeader header, @Nonnull final String value) {
        this.header = header;
        this.value = value;
    }

    @Override
    public String toString() {
        return header.getName() + ": " + value;
    }
}
