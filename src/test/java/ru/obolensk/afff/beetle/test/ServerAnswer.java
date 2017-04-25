package ru.obolensk.afff.beetle.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.obolensk.afff.beetle.request.HttpCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by Afff on 21.04.2017.
 */
@AllArgsConstructor
public class ServerAnswer {

    @Getter
    @Nonnull
    private final HttpCode code;

    @Getter
    @Nullable
    private final String receivedContent;
}
