package ru.obolensk.afff.beetle.log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * LoggablePrintWriter decorator class, writes all commands to the Log
 * Created by Afff on 14.06.2016.
 */
public class LoggablePrintWriter implements Writer {
    private final PrintWriter pw;
    private final Logger logger;

    public LoggablePrintWriter(@Nonnull final OutputStream outStream, @Nonnull final Logger logger) {
        this.pw = new PrintWriter(outStream);
        this.logger = logger;
    }

    @Override
    public void println(@Nullable final String string) {
        logger.trace("OUT<<<{}", string);
        pw.println(string);
    }

    @Override
    public void write(@Nullable final String string) {
        logger.trace("OUT<<<{}", string);
        pw.print(string);
    }

    @Override
    public void println() {
        pw.println();
    }

    @Override
    public void flush() {
        pw.flush();
    }
}
