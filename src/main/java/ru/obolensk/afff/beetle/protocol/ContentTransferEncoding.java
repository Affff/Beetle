package ru.obolensk.afff.beetle.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;

/**
 * Created by Afff on 04.09.2017.
 */
public enum ContentTransferEncoding {

    BASE64("base64"),
    QUOTED_PRINTABLE("quoted-printable"),
    _8BIT("8bit"),
    _7BIT("7bit"),
    BINARY("binary")
    ;

    @Nonnull @Getter
    private final String name;

    ContentTransferEncoding(@Nonnull final String name) {
        this.name = name;
    }

    @Nullable
    public static ContentTransferEncoding getByName(@Nullable final String name) {
        if (name == null) {
            return null;
        }
        for (ContentTransferEncoding enc : values()) {
            if (enc.getName().equalsIgnoreCase(name)) {
                return enc;
            }
        }
        return _7BIT; // default encoding
    }
}
