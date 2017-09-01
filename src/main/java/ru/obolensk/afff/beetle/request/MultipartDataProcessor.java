package ru.obolensk.afff.beetle.request;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.obolensk.afff.beetle.core.Storage;
import ru.obolensk.afff.beetle.protocol.HttpHeader;
import ru.obolensk.afff.beetle.protocol.HttpHeaderValueAttribute;
import ru.obolensk.afff.beetle.util.RequestUtil;

import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_DISPOSITION;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValue.CONTENT_DISPOSITION_FILE;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValue.CONTENT_DISPOSITION_FORM_DATA;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValueAttribute.FILENAME;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValueAttribute.NAME;
import static ru.obolensk.afff.beetle.protocol.MimeType.MULTIPART_MIXED;
import static ru.obolensk.afff.beetle.request.MultipartDataProcessor.State.BOUNDARY;
import static ru.obolensk.afff.beetle.request.MultipartDataProcessor.State.DATA;
import static ru.obolensk.afff.beetle.request.MultipartDataProcessor.State.HEAD;
import static ru.obolensk.afff.beetle.util.RequestUtil.contentTypeEquals;
import static ru.obolensk.afff.beetle.util.RequestUtil.headerEquals;

/**
 * Created by Afff on 29.05.2017.
 */
public class MultipartDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MultipartDataProcessor.class);

    @Nonnull
    private final Request request;

    @Nonnull
    private final String boundary;

    @Nonnull
    private final Storage storage;

    protected enum State {
        BOUNDARY,
        HEAD,
        DATA
    }

    @Nonnull
    private State waitFor = BOUNDARY;

    @Nonnull
    private final Multimap<HttpHeader, AttributedValue> headers = LinkedListMultimap.create();

    @Nonnull
    private final StringBuilder buffer = new StringBuilder();

    public MultipartDataProcessor(@Nonnull final Request request, @Nonnull final String boundary, @Nonnull final Storage storage) {
        this.request = request;
        this.boundary = "--" + boundary;
        this.storage = storage;
    }

    public boolean step(@Nullable final String line) throws IOException {
        if (line == null) {
            return false;
        }
        if (line.startsWith(boundary)) {
            if (buffer.length() != 0) {
                if (headerEquals(headers, CONTENT_DISPOSITION, CONTENT_DISPOSITION_FORM_DATA)) { // otherwise it is corrupted header, skip all data in this section
                    if (contentTypeEquals(headers, MULTIPART_MIXED)) {
                        final String boundary = RequestUtil.getHeaderAttribute(headers, CONTENT_TYPE, HttpHeaderValueAttribute.BOUNDARY);
                        if (boundary != null) {
                            final MultipartDataProcessor nested = new MultipartDataProcessor(request, boundary, storage);
                            while (nested.step(request.getReader().readLine())) ;
                        }
                    } else {
                        //consider other content types means nothing, simple ignore them
                        final String name = RequestUtil.getHeaderAttribute(headers, CONTENT_DISPOSITION, NAME);
                        if (name != null && !name.trim().isEmpty()) {
                            request.addParameter(name, buffer.toString());
                        }
                    }
                } else if (headerEquals(headers, CONTENT_DISPOSITION, CONTENT_DISPOSITION_FILE)) {
                    final String filename = RequestUtil.getHeaderAttribute(headers, CONTENT_DISPOSITION, FILENAME);
                    //TODO buffer -> support different file transfer encodings
                    if (!storage.readMultipartFileToRequest(request, filename, buffer.toString())) {
                        logger.warn("File {} can't be stored for request with URI {}", filename, request.getUri());
                    };
                }
                buffer.setLength(0);
            }
            if (line.length() == boundary.length()) {
                waitFor = HEAD;
                return true;
            } else {
                return false; // end of data
            }
        }
        switch (waitFor) {
            case BOUNDARY: {
                return true; // skip current lines while boundary won't be detected
            }
            case HEAD: {
                if (!line.isEmpty()) {
                    RequestUtil.addHeader(headers, line);
                } else {
                    waitFor = DATA;
                }
                break;
            }
            default: {
                buffer.append(line);
            }
        }
        return true;
    }
}
