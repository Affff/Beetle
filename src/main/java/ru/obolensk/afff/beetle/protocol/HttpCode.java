package ru.obolensk.afff.beetle.protocol;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;

/**
 * Created by Afff on 08.06.2016.
 */
public enum HttpCode {

    HTTP_100(100, "Continue"),
    HTTP_101(101, "Switching Protocols"),
    HTTP_200(200, "OK"),
    HTTP_201(201, "Created"),
    HTTP_202(202, "Accepted"),
    HTTP_203(203, "Non-Authoritative Information"),
    HTTP_204(204, "No Content"),
    HTTP_205(205, "Reset Content"),
    HTTP_206(206, "Partial Content"),
    HTTP_300(300, "Multiple Choices"),
    HTTP_301(301, "Moved Permanently"),
    HTTP_302(302, "Found"),
    HTTP_303(303, "See Other"),
    HTTP_304(304, "Not Modified"),
    HTTP_305(305, "Use Proxy"),
    HTTP_307(307, "Temporary Redirect"),
    HTTP_400(400, "Bad Request"),
    HTTP_401(401, "Unauthorized"),
    HTTP_402(402, "Payment Required"),
    HTTP_403(403, "Forbidden"),
    HTTP_404(404, "Not Found"),
    HTTP_405(405, "Method Not Allowed"),
    HTTP_406(406, "Not Acceptable"),
    HTTP_407(407, "Proxy Authentication Required"),
    HTTP_408(408, "Request Time-out"),
    HTTP_409(409, "Conflict"),
    HTTP_410(410, "Gone"),
    HTTP_411(411, "Length Required"),
    HTTP_412(412, "Precondition Failed"),
    HTTP_413(413, "Request Entity Too Large"),
    HTTP_414(414, "Request-URI Too Large"),
    HTTP_415(415, "Unsupported Media Type"),
    HTTP_416(416, "Requested range not satisfiable"),
    HTTP_417(417, "Expectation Failed"),
    HTTP_500(500, "Internal Server Error"),
    HTTP_501(501, "Not Implemented"),
    HTTP_502(502, "Bad Gateway"),
    HTTP_503(503, "Service Unavailable"),
    HTTP_504(504, "Gateway Time-out"),
    HTTP_505(505, "HTTP Version not supported")
    ;

    @Getter
    private final int code;

    @Nonnull @Getter
    private final String descr;

    HttpCode(int code, String name) {
        this.code = code;
        this.descr = name;
    }

    @Nonnull
    public static HttpCode getByStatusCode(@Nullable final Number status) {
        if (status == null) {
            return HTTP_500;
        }
        try {
            return HttpCode.valueOf("HTTP_" + status.intValue());
        } catch (IllegalArgumentException e) {
            return HTTP_500;
        }
    }
}
