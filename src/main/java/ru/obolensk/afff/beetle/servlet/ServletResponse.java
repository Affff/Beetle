package ru.obolensk.afff.beetle.servlet;

import lombok.Getter;
import ru.obolensk.afff.beetle.protocol.HttpCode;

import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_200;

/**
 * Created by Afff on 30.08.2017.
 */
public class ServletResponse {

    @Getter
    private final HttpCode code;

    @Getter
    private final String errorMessage;

    @Getter
    private final String data;

    public ServletResponse(HttpCode code, String data) {
        this.code = code;
		if (code == HTTP_200) {
			this.errorMessage = null;
			this.data = data;
		} else {
			this.errorMessage = data;
			this.data = null;
		}
    }
}
