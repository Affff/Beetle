package ru.obolensk.afff.beetle.request;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import ru.obolensk.afff.beetle.util.RequestUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static ru.obolensk.afff.beetle.conn.MimeType.MULTIPART_MIXED;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_DISPOSITION;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.request.HttpHeaderValue.CONTENT_DISPOSITION_FORM_DATA;
import static ru.obolensk.afff.beetle.request.HttpHeaderValueAttribute.NAME;
import static ru.obolensk.afff.beetle.request.MultipartDataProcessor.State.*;

/**
 * Created by Afff on 29.05.2017.
 */
//TODO test multipart form data params
public class MultipartDataProcessor {

    @Nonnull
    private final Request request;

    @Nonnull
    private final String boundary;

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

    public MultipartDataProcessor(@Nonnull final Request request, @Nonnull final String boundary) {
        this.request = request;
        this.boundary = "--" + boundary;
    }

    public boolean step(@Nullable final String line) {
        if (line == null) {
            return false;
        }
        if (line.startsWith(boundary)) {
            if (buffer.length() != 0) {
                final String contentDisposition = RequestUtil.getHeaderValue(headers, CONTENT_DISPOSITION);
                final String contentType = RequestUtil.getHeaderValue(headers, CONTENT_TYPE);
                if (contentDisposition != null) { // otherwise it is corrupted header, skip all data in this section
                    if (CONTENT_DISPOSITION_FORM_DATA.getName().equals(contentDisposition.toLowerCase())) {
                        if (contentType != null && MULTIPART_MIXED.getName().equals(contentType.toLowerCase())) {
                            //TODO support form-data, Content-Type: multipart/mixed - nested data
                        } else {
                            //consider other content types means nothing, simple ignore them
                            final String name = RequestUtil.getHeaderAttribute(headers, CONTENT_DISPOSITION, NAME);
                            if (name != null && !name.trim().isEmpty()) {
                                request.addParameter(name, buffer.toString());
                            }
                        }
                    }
                }
                //TODO support file content type, attribute filename
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
