package ru.obolensk.afff.beetle.request;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
                attributes.put(keyValuePair[0].trim(),
                        keyValuePair.length != 1 ? keyValuePair[1].trim() : null);
            }
        }
        this.attributes = Collections.unmodifiableMap(attributes);
    }
}
