package ru.obolensk.afff.beetle.conn;

import lombok.Getter;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by Afff on 11.04.2017.
 */
public enum MimeType {
    TEXT_HTML("text/html"),
    MESSAGE_HTTP("message/http", null),
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded", null),
    MULTIPART_FORM_DATA("multipart/form-data", null),
    TEXT_PLAIN("text-plain"),
    UNKNOWN(null)
    ;

    @Getter
    private final String name;

    @Getter
    private final Charset charset;

    MimeType(@Nullable final String name) {
        this(name, StandardCharsets.UTF_8);
    }

    MimeType(@Nullable final String name, @Nullable final Charset charset) {
        this.name = name;
        this.charset = charset;
    }

    @Nullable
    public static MimeType getByName(@Nullable final String name) {
        for (MimeType type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
