package ru.obolensk.afff.beetle.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static java.lang.Math.min;

/**
 * Created by Afff on 19.04.2017.
 */
public class StreamUtil {

    private static final int BUFFER_SIZE = 8192;

    public static String readAsString(@Nonnull final Reader reader, final int length) throws IOException {
        final char[] buffer = new char[length];
        final int count = reader.read(buffer, 0, length);
        return new String(buffer, 0, count);
    }

    public static int copy(@Nonnull final Reader source, @Nonnull final Writer target, final int bytesCount) throws IOException, IllegalArgumentException {
        if (bytesCount <= 0) {
            throw new IllegalArgumentException("Bytes count must be positive!");
        }
        final char[] buf = new char[BUFFER_SIZE];
        int nread = 0;
        int n;
        while ((n = source.read(buf, 0, min(BUFFER_SIZE, bytesCount - nread))) > 0 && nread < bytesCount) {
            target.write(buf, 0, n);
            nread += n;
        }
        target.flush();
        return nread;
    }
}
