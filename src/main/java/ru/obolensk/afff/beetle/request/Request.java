package ru.obolensk.afff.beetle.request;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ru.obolensk.afff.beetle.log.LoggablePrintWriter;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.log.Writer;
import ru.obolensk.afff.beetle.protocol.HttpHeader;
import ru.obolensk.afff.beetle.protocol.HttpHeaderValue;
import ru.obolensk.afff.beetle.protocol.HttpHeaderValueAttribute;
import ru.obolensk.afff.beetle.protocol.HttpMethod;
import ru.obolensk.afff.beetle.protocol.HttpVersion;
import ru.obolensk.afff.beetle.protocol.MimeType;
import ru.obolensk.afff.beetle.stream.LimitedBufferedReader;
import ru.obolensk.afff.beetle.util.RequestUtil;
import ru.obolensk.afff.beetle.util.UriUtil;

import static lombok.AccessLevel.PROTECTED;
import static ru.obolensk.afff.beetle.core.Storage.ROOT;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.GET;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.util.UriUtil.decode;

/**
 * Created by Afff on 10.04.2017.
 */
public class Request {

    private static final Logger logger = new Logger(Request.class);

    @Nonnull @Getter
    private final LimitedBufferedReader reader;

    @Nonnull @Getter
    private final OutputStream outputStream;

    @Nonnull @Getter
    private final Writer writer;

    @Nonnull @Getter @Setter(AccessLevel.MODULE)
    private String rawData;

    @Nonnull @Getter
    private final InetAddress ip;

    @Nonnull @Getter
    private final HttpMethod method;

    @Nullable @Getter
    private final URI uri;

    @Nonnull @Getter
    private final HttpVersion version;

    @Nonnull @Getter
    private final Map<String, String> parameters = new HashMap<>();

    @Nonnull @Getter
    private final Multimap<HttpHeader, AttributedValue> headers = LinkedListMultimap.create();

    @Nonnull @Getter
    private final List<MultipartData> multipartData = new ArrayList<>();

    @Getter
    private final boolean invalid;

    @Nullable @Getter @Setter(PROTECTED)
    private int entitySize;

    private Request(@Nonnull final LimitedBufferedReader reader,
                    @Nonnull final OutputStream outputStream,
                    @Nonnull final InetAddress ip,
                    @Nonnull final String method,
                    @Nonnull final String uri,
                    @Nonnull final String version) {
        this(reader, outputStream, ip, HttpMethod.decode(method), uri, HttpVersion.decode(version));
    }

    private Request(@Nonnull final LimitedBufferedReader reader,
                    @Nonnull final OutputStream outputStream,
                    @Nonnull final InetAddress ip,
                    @Nonnull final HttpMethod method,
                    @Nullable final String uri,
                    @Nonnull final HttpVersion version) {
        this.reader = reader;
        this.outputStream = outputStream;
        this.writer = new LoggablePrintWriter(outputStream, logger);
        this.ip = ip;
        this.method = method;
        this.uri = readUriAndParams(uri);
        this.version = version;
        this.invalid = method == HttpMethod.UNKNOWN || uri == null || version == HttpVersion.UNKNOWN;
    }

    static Request makeNew(@Nonnull final LimitedBufferedReader reader, @Nonnull final OutputStream outputStream, @Nonnull final InetAddress ip, @Nonnull final String method, @Nonnull final String uri, @Nonnull final String version) {
        return new Request(reader, outputStream, ip, method, uri, version);
    }

    boolean addHeader(@Nonnull final String line) {
        return RequestUtil.addHeader(headers, line);
    }

    @Nullable
    String getHeaderValue(@Nonnull final HttpHeader header) {
        return RequestUtil.getHeaderValue(headers, header);
    }

    @Nullable
    public String getHeaderAttribute(@Nonnull final HttpHeader header,
                                     @Nonnull final HttpHeaderValueAttribute attr) {
        return RequestUtil.getHeaderAttribute(headers, header, attr);
    }

    public boolean hasHeaderValue(@Nonnull final HttpHeader header,
                                  @Nonnull final HttpHeaderValue value) {
        return RequestUtil.hasHeaderValue(headers, header, value);
    }

    @Nullable
    private URI readUriAndParams(@Nullable String uri) {
        if (uri == null) {
            return null;
        }
        if (method == GET || method == HEAD) {
            final String[] params = uri.split("[?;]");
            uri = params[0];
            if (params.length > 1) {
                parseParams(params[1]);
            }
        }
        return UriUtil.toURI(uri);
    }

    public void parseParams(@Nonnull final String params) {
        final String[] paramsArr = params.split("&");
        for (final String param : paramsArr) {
            final String[] parts = param.split("=");
            addParameter(parts[0].trim(), parts.length > 1 ? decode(parts[1]).trim() : null);
        }
    }

    void addParameter(@Nonnull final String name, @Nullable final String value) {
        this.parameters.put(name, value);
    }

    public boolean shouldWriteBody() {
        return method != HttpMethod.HEAD;
    }

    public boolean hasEntity() {
        return entitySize != 0;
    }

    public void skipEntityQuietly() {
        if (hasEntity()) {
            try {
                long actualSkipped = reader.skip(entitySize);
                if (actualSkipped != entitySize) {
                    logger.warn("Malformed HTTP request: entity should have "
                            + entitySize + " but it has only " + actualSkipped + " bytes.");
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    @Nonnull
    public String getLocalPath() {
        final String path = uri != null ? uri.getPath() : "";
        return path.isEmpty() ? ROOT.toString() : path;
    }

    @Nonnull
    public String getPathForFile(@Nonnull final String fileName) {
        final String path = uri != null ? uri.resolve(fileName).getPath() : "";
        return path.isEmpty() ? ROOT.resolve(fileName).toString() : path;
    }

    @Nonnull
    public MimeType getContentType() {
        return MimeType.getByName(getHeaderValue(HttpHeader.CONTENT_TYPE));
    }

}
