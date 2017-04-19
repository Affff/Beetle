package ru.obolensk.afff.beetle.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by Afff on 19.04.2017.
 */
public class UriUtil {

    @Nullable
    public static URI toURI(@Nonnull final String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Nonnull
    public static String decode(@Nonnull final String uriParams) {
        try {
            return URLDecoder.decode(uriParams, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage()); // shouldn't be thrown
        }
    }
}
