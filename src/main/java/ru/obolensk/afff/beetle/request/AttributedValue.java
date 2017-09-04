package ru.obolensk.afff.beetle.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.Getter;

/**
 * Created by Afff on 25.05.2017.
 */
public class AttributedValue {

    @Getter
    @Nonnull
    private final String value;

    @Getter
    @Nonnull
    private final Map<String, String> attributes;

    public AttributedValue(@Nonnull final String value) {
        final Map<String, String> attributes = new HashMap<>();
        final String[] parts = value.split(";");
        this.value = parts[0].trim();
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                final String[] keyValuePair = parts[i].split("=");
                final String attrKey = keyValuePair[0].trim();
                final String attrValue = keyValuePair.length != 1 ? trimAndRemoveQuotes(keyValuePair[1]) : null;
                attributes.put(attrKey, attrValue);
            }
        }
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    private static String trimAndRemoveQuotes(@Nonnull String str) {
        str = str.trim();
        if (str != null) {
            char startChar = str.charAt(0);
            if (startChar == '"' || startChar == '\'') {
                str = str.substring(1, str.length() - 1);
            }
        }
        return str;
    }
}
