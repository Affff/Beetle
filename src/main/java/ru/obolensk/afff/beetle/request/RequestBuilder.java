package ru.obolensk.afff.beetle.request;

import com.google.common.primitives.Ints;
import ru.obolensk.afff.beetle.log.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
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

    public RequestBuilder(@Nonnull final OutputStream outputStream, @Nonnull final String requestStr) {
        final String[] components = requestStr.split("\\s");
        request = Request.makeNew(outputStream, components[0], components[1], components[2]);
        rawRequest.append(requestStr).append("\r\n");
    }

    public boolean addHeader(@Nullable final String headerStr) {
        if (headerStr == null) {
            return false;
        }
        rawRequest.append(headerStr).append("\r\n");
        if (headerStr.isEmpty()) {
            return false;
        }
        final String[] headers = headerStr.split(":");
        if (headers.length >= 1) {
            request.addHeader(headers[0], headers[1].split(","));
            return true;
        }
        return false;
    }

    public void appendEnitityIfExists(@Nonnull final InputStream inputStream) {
        final Integer entitySize = entitySize();
        if (entitySize != null) {
            request.setEntitySize(entitySize);
            request.setEntityStream(inputStream);
        }
    }

    @Nullable
    private Integer entitySize() {
        final String bodySizeStr = request.getHeaderValue(HttpHeader.CONTENT_LENGTH);
        if (bodySizeStr != null) {
            return Ints.tryParse(bodySizeStr);
        }
        return null;
    }

    @Nonnull
    public Request build() {
        request.setRawData(rawRequest.toString());
        logger.trace("IN>>>" + request.getRawData());
        return request;
    }
}
