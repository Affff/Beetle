package ru.obolensk.afff.beetle.settings;

import javax.annotation.Nonnull;

/**
 * Created by Afff on 21.04.2017.
 */
public interface Config {
    <T> T get(@Nonnull final Options options);
}
