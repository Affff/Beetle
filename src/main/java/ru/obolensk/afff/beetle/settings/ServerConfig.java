package ru.obolensk.afff.beetle.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Afff on 20.04.2017.
 */
public class ServerConfig implements Config {

    private final Map<Options, Object> settings = new ConcurrentHashMap<>();

    public ServerConfig() {
        for (Options option : Options.values()) {
            settings.put(option, option.getDefaultValue());
        }
    }

    public void set(@Nonnull final Options option, @Nullable final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Option can't has a null value.");
        }
        if (option.getDefaultValue().getClass() != value.getClass()) {
            throw new IllegalArgumentException("Option value has wrong type: " + value.getClass()
                    + " instead of " + option.getDefaultValue().getClass());
        }
        settings.put(option, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@Nonnull final Options options) {
        return (T) settings.get(options);
    }
}
