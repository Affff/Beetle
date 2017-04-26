package ru.obolensk.afff.beetle.log;

import javax.annotation.Nonnull;

/**
 * Created by Afff on 11.04.2017.
 */
public interface Writer {

    void println(@Nonnull String string);

    void println();

    void write(@Nonnull String string);

    void flush();
}
