package ru.obolensk.afff.beetle.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.lang.Math.min;

/**
 * Created by Afff on 19.04.2017.
 */
public class StreamUtil {

    private static final int BUFFER_SIZE = 8192;

    public static int copy(@Nonnull final InputStream source, @Nonnull final OutputStream target, final int bytesCount) throws IOException {
        if (bytesCount <= 0) {
            throw new IllegalArgumentException("Bytes count must be positive!");
        }
        final byte[] buf = new byte[BUFFER_SIZE];
        int nread = 0;
        int n;
        while ((n = source.read(buf, 0, min(BUFFER_SIZE, bytesCount - nread))) > 0 && nread < bytesCount) {
            target.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }
}
