package ru.obolensk.afff.beetle.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.io.Files;
import ru.obolensk.afff.beetle.util.Version;
import ru.obolensk.afff.beetle.protocol.MimeType;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.log.Writer;
import ru.obolensk.afff.beetle.protocol.HttpCode;
import ru.obolensk.afff.beetle.protocol.HttpHeader;
import ru.obolensk.afff.beetle.protocol.HttpMethod;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.request.RequestBuilder;
import ru.obolensk.afff.beetle.servlet.ServletResponse;
import ru.obolensk.afff.beetle.stream.LimitedBufferedReader;
import ru.obolensk.afff.beetle.util.DateUtil;

import static ru.obolensk.afff.beetle.protocol.MimeType.TEXT_HTML;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_LENGTH;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.DATE;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.SERVER;

/**
 * Created by Afff on 11.04.2017.
 */
class ResponseWriter {

    private static final Logger logger = new Logger(ResponseWriter.class);

    private final Request request;

    ResponseWriter(@Nonnull final Request request) {
        this.request = request;
    }

	void sendAnswer(@Nonnull final HttpCode code, @Nonnull final MimeType mimeType, @Nonnull final String content) {
		sendAnswer(code, mimeType, content, null);
	}

    private void sendAnswer(@Nonnull final HttpCode code, @Nonnull final MimeType mimeType,
                           @Nullable final String content, @Nullable String errorDescr) {
        final Writer writer = writeHeader(code, errorDescr);
        final int length = content != null ? content.getBytes(StandardCharsets.UTF_8).length : 0;
        if (length != 0) {
            writer.println(CONTENT_TYPE.getName() + ":" + mimeType.getName() + charsetAttr(mimeType));
        }
        writer.println(CONTENT_LENGTH.getName() + ": " + length);
        writer.println();
        if (length != 0 && request.shouldWriteBody()) { // HEAD method mustn't have body
            writer.write(content);
        }
        writer.flush();
    }

    void sendServletResponse(ServletResponse response) {
        sendAnswer(response.getCode(), MimeType.TEXT_HTML, response.getData(), response.getErrorMessage());
    }

    @Nonnull
    private String charsetAttr(@Nonnull final MimeType mimeType) {
        if (mimeType.getCharset() == null) {
            return "";
        }
        return "; charset=" + mimeType.getCharset().name();
    }

    void sendOptions(List<HttpMethod> options) {
        final Writer writer = writeHeader(HttpCode.HTTP_200, null);
        final StringJoiner joiner = new StringJoiner("");
        options.forEach(option -> joiner.add(option.name()));
        writer.println(HttpHeader.ALLOW.getName() + ":" + joiner.toString());
        writer.flush();
    }

    void sendConnected() {
        final Writer writer = request.getWriter();
        writer.println(request.getVersion().getName() + " " + HttpCode.HTTP_200 + " Connection established");
        writer.println(DATE.getName() + ": " + DateUtil.getHttpTime());
        writer.println(SERVER.getName() + ": " + Version.nameAndVersion());
        writer.flush();
    }

    void sendEmptyAnswer(@Nonnull final HttpCode code) {
        sendAnswer(code, TEXT_HTML, "");
    }

    static void sendUnparseableRequestAnswer(InetAddress ip, @Nonnull final OutputStream out, @Nonnull final HttpCode code) {
        final LimitedBufferedReader emptyReader = new LimitedBufferedReader(new StringReader(""), Integer.MAX_VALUE);
        final ResponseWriter writer = new ResponseWriter(new RequestBuilder(ip, emptyReader, out,"UNKNOWN / HTTP/1.1").build());
        writer.sendEmptyAnswer(code);
    }

    void sendFile(@Nonnull final Path path) {
        HttpCode code = HttpCode.HTTP_200;
        String answer;
        try {
            answer = Files.toString(path.toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e);
            answer = "";
            code = HttpCode.HTTP_500;
        }
        sendAnswer(code, TEXT_HTML, answer);
    }

    void send404() {
        final HttpCode code = HttpCode.HTTP_404;
        String answer = "<html><header><meta charset=\"UTF-8\"><title>" + Version.nameAndVersion() + ": " + + code.getCode() + " " + code.getDescr() + "</title></header>"
            + "<body>Requested URI '" + request.getUri() + "' wasn't found.</body></html>";
        sendAnswer(code, TEXT_HTML, answer);
    }

    private Writer writeHeader(@Nonnull final HttpCode code, @Nullable String errorDescr) {
        final Writer writer = request.getWriter();
        writer.println(request.getVersion().getName() + " " + code.getCode() + " " + (errorDescr == null ? code.getDescr() : errorDescr));
        writer.println(DATE.getName() + ": " + DateUtil.getHttpTime());
        writer.println(SERVER.getName() + ": " + Version.nameAndVersion());
        return writer;
    }
}
