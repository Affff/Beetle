package ru.obolensk.afff.beetle.log;

/**
 * Created by Afff on 11.04.2017.
 */
public interface Writer {
    void println(String string);

    void println();

    void flush();
}
