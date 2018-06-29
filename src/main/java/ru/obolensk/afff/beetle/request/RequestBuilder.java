package ru.obolensk.afff.beetle.request;

import java.io.OutputStream;
import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.protocol.HttpHeader;
import ru.obolensk.afff.beetle.stream.LimitedBufferedReader;

/**
 * Created by Afff on 10.04.2017.
 */
public class RequestBuilder {

    private static final Logger logger = new Logger(RequestBuilder.class);

    @Nonnull
    private final Request request;

    @Nonnull
    private final StringBuilder rawRequest = new StringBuilder();

    public RequestBuilder(@Nonnull final InetAddress ip, @Nonnull final LimitedBufferedReader reader, @Nonnull final OutputStream outputStream, @Nonnull final String requestStr) {
        final String[] components = requestStr.split("\\s");
        request = Request.makeNew(reader, outputStream, ip, components[0], components[1], components[2]);
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

    private int entitySize() {
        final String bodySizeStr = request.getHeaderValue(HttpHeader.CONTENT_LENGTH);
        if (bodySizeStr != null) {
            try {
                return Integer.valueOf(bodySizeStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

}
