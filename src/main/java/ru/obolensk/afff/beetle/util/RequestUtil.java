package ru.obolensk.afff.beetle.util;

import com.google.common.collect.Multimap;
import ru.obolensk.afff.beetle.request.AttributedValue;
import ru.obolensk.afff.beetle.request.HttpHeader;
import ru.obolensk.afff.beetle.request.HttpHeaderValue;
import ru.obolensk.afff.beetle.request.HttpHeaderValueAttribute;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Created by Afff on 01.06.2017.
 */
public class RequestUtil {

    //TODO create here methods to compare header values and mime types, refactor the code to use them

    /**
     * Method parses line to request parameter. If parameter has no value, it will be ignored.
     * True - line is some parameter value, false - line is empty string (the end of parameters list detected).
     */
    public static boolean addHeader(@Nonnull final Multimap<HttpHeader, AttributedValue> headers, @Nonnull final String headerStr) {
        if (headerStr.isEmpty()) {
            return false;
        }
        final String[] headerParts = headerStr.split(":");
        if (headerParts.length >= 2) {
            final String name = headerParts[0];
            final String[] values = headerParts[1].split(",");
            final HttpHeader header = HttpHeader.getByName(name.trim().toLowerCase());
            if (header != null) {
                for (final String value : values) {
                    headers.put(header, new AttributedValue(value.trim()));
                }
            }
        }
        return true;
    }

    @Nullable
    public static String getHeaderValue(@Nonnull final Multimap<HttpHeader, AttributedValue> headers,
                                        @Nonnull final HttpHeader header) {
        final Collection<AttributedValue> values = headers.get(header);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.iterator().next().getValue();
    }

    @Nullable
    public static String getHeaderAttribute(@Nonnull final Multimap<HttpHeader, AttributedValue> headers,
                                            @Nonnull final HttpHeader header,
                                            @Nonnull final HttpHeaderValueAttribute attr) {
        final Collection<AttributedValue> values = headers.get(header);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.iterator().next().getAttributes().get(attr.getName());
    }

    public static boolean hasHeaderValue(@Nonnull final Multimap<HttpHeader, AttributedValue> headers,
                                         @Nonnull final HttpHeader header,
                                         @Nonnull final HttpHeaderValue value) {
        final Collection<AttributedValue> values = headers.get(header);
        for (final AttributedValue item : values) {
            if (value.getName().equals(item.getValue().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
