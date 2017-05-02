package ru.obolensk.afff.beetle.request;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ru.obolensk.afff.beetle.conn.MimeType;
import ru.obolensk.afff.beetle.log.LoggablePrintWriter;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.log.Writer;
import ru.obolensk.afff.beetle.util.UriUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.PROTECTED;
import static ru.obolensk.afff.beetle.Storage.ROOT;
import static ru.obolensk.afff.beetle.request.HttpMethod.GET;
import static ru.obolensk.afff.beetle.request.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.util.UriUtil.decode;

/**
 * Created by Afff on 10.04.2017.
 */
public class Request {

    private static final Logger logger = new Logger(Request.class);

    @Nonnull @Getter
    private final Reader reader;

    @Nonnull @Getter
    private final OutputStream outputStream;

    @Nonnull @Getter
    private final Writer writer;

    @Nonnull @Getter @Setter(AccessLevel.MODULE)
    private String rawData;

    @Nonnull @Getter
    private final HttpMethod method;

    @Nullable @Getter
    private final URI uri;

    @Nonnull @Getter
    private final HttpVersion version;

    @Nonnull @Getter
    private final Map<String, String> parameters = new HashMap<>();

    @Nonnull @Getter
    private final Multimap<HttpHeader, String> headers = LinkedListMultimap.create();

    @Getter
    private final boolean invalid;

    @Nullable @Getter @Setter(PROTECTED)
    private Integer entitySize;

    private Request(@Nonnull final Reader reader, @Nonnull final OutputStream outputStream, @Nonnull final String method, @Nonnull final String uri, @Nonnull final String version) {
        this(reader, outputStream, HttpMethod.decode(method), uri, HttpVersion.decode(version));
    }

    private Request(@Nonnull final Reader reader, @Nonnull final OutputStream outputStream) {
        this(reader, outputStream, HttpMethod.UNKNOWN, null, HttpVersion.UNKNOWN);
    }

    private Request(@Nonnull final Reader reader,
                    @Nonnull final OutputStream outputStream,
                    @Nonnull final HttpMethod method,
                    @Nullable final String uri,
                    @Nonnull final HttpVersion version) {
        this.reader = reader;
        this.outputStream = outputStream;
        this.writer = new LoggablePrintWriter(outputStream, logger);
        this.method = method;
        this.uri = readUriAndParams(uri);
        this.version = version;
        this.invalid = method == HttpMethod.UNKNOWN || uri == null || version == HttpVersion.UNKNOWN;
    }

    static Request makeNew(@Nonnull final Reader reader, @Nonnull final OutputStream outputStream, @Nonnull final String method, @Nonnull final String uri, @Nonnull final String version) {
        return new Request(reader, outputStream, method, uri, version);
    }

    void addHeader(@Nonnull final String name, @Nullable final String... values) {
        final HttpHeader header = HttpHeader.getByName(name.trim().toLowerCase());
        if (header != null && values != null) {
            for (final String value : values) {
                headers.put(header, value.trim());
            }
        }
    }

    @Nullable
    public String getHeaderValue(@Nonnull final HttpHeader header) {
        Collection<String> values = headers.get(header);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
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
            this.parameters.put(parts[0].trim(), parts.length > 1 ? decode(parts[1]).trim() : null);
        }
    }

    public boolean shouldWriteBody() {
        return method != HttpMethod.HEAD;
    }

    public boolean hasEntity() {
        return entitySize != null;
    }

    public void skipEntityQuietly() {
        if (entitySize != null) {
            try {
                reader.skip(entitySize);
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
    public MimeType getContentType() {
        return MimeType.getByName(getHeaderValue(HttpHeader.CONTENT_TYPE));
    }
}
