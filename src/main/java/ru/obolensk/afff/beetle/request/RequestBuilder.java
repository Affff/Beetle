package ru.obolensk.afff.beetle.request;

import com.google.common.primitives.Ints;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.stream.LimitedBufferedReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.OutputStream;

/**
 * Created by Afff on 10.04.2017.
 */
public class RequestBuilder {

    private static final Logger logger = new Logger(RequestBuilder.class);

    @Nonnull
    private final Request request;

    @Nonnull
    private final StringBuilder rawRequest = new StringBuilder();

    public RequestBuilder(@Nonnull final LimitedBufferedReader reader, @Nonnull final OutputStream outputStream, @Nonnull final String requestStr) {
        final String[] components = requestStr.split("\\s");
        request = Request.makeNew(reader, outputStream, components[0], components[1], components[2]);
        rawRequest.append(requestStr).append("\r\n");
    }

    public boolean addHeader(@Nullable final String headerStr) {
        if (headerStr == null) {
            return false;
        }
        rawRequest.append(headerStr).append("\r\n");
        return !headerStr.isEmpty() && request.addHeader(headerStr);
    }
    @Nonnull
    public Request build() {
        request.setEntitySize(entitySize());
        request.setRawData(rawRequest.toString());
        logger.trace("IN>>>" + request.getRawData());
        return request;
    }

    @Nullable
    private Integer entitySize() {
        final String bodySizeStr = request.getHeaderValue(HttpHeader.CONTENT_LENGTH);
        if (bodySizeStr != null) {
            return Ints.tryParse(bodySizeStr);
        }
        return null;
    }

}
