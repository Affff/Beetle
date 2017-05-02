package ru.obolensk.afff.beetle.conn;

import com.google.common.io.Files;
import ru.obolensk.afff.beetle.Version;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.log.Writer;
import ru.obolensk.afff.beetle.request.*;
import ru.obolensk.afff.beetle.util.DateUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

import static ru.obolensk.afff.beetle.conn.MimeType.TEXT_HTML;
import static ru.obolensk.afff.beetle.request.HttpHeader.*;

/**
 * Created by Afff on 11.04.2017.
 */
public class ResponseWriter {

    private static final Logger logger = new Logger(ResponseWriter.class);

    private final Request request;

    public ResponseWriter(@Nonnull final Request request) {
        this.request = request;
    }

    public void sendAnswer(@Nonnull final HttpCode code, @Nonnull final MimeType mimeType, @Nonnull final String content) {
        final Writer writer = writeHeader(code);
        final int length = content.getBytes(StandardCharsets.UTF_8).length;
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

    @Nonnull
    private String charsetAttr(@Nonnull final MimeType mimeType) {
        if (mimeType.getCharset() == null) {
            return "";
        }
        return "; charset=" + mimeType.getCharset().name();
    }

    public void sendOptions(List<HttpMethod> options) {
        final Writer writer = writeHeader(HttpCode.HTTP_200);
        final StringJoiner joiner = new StringJoiner("");
        options.forEach(option -> joiner.add(option.name()));
        writer.println(HttpHeader.ALLOW.getName() + ":" + joiner.toString());
        writer.flush();
    }

    public void sendConnected() {
        final Writer writer = request.getWriter();
        writer.println(request.getVersion().getName() + " " + HttpCode.HTTP_200 + " Connection established");
        writer.println(DATE.getName() + ": " + DateUtil.getHttpTime());
        writer.println(SERVER.getName() + ": " + Version.nameAndVersion());
        writer.flush();
    }

    public void sendEmptyAnswer(@Nonnull final HttpCode code) {
        sendAnswer(code, TEXT_HTML, "");
    }

    public static void sendUnparseableRequestAnswer(@Nonnull final OutputStream out, @Nonnull final HttpCode code) {
        final ResponseWriter writer = new ResponseWriter(new RequestBuilder(new StringReader(""), out,"UNKNOWN / HTTP/1.1").build());
        writer.sendEmptyAnswer(code);
    }

    public void sendFile(@Nonnull final Path path) {
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

    public void send404() {
        final HttpCode code = HttpCode.HTTP_404;
        String answer = "<html><header><meta charset=\"UTF-8\"><title>" + Version.nameAndVersion() + ": " + + code.getCode() + " " + code.getDescr() + "</title></header>"
            + "<body>Requested URI '" + request.getUri() + "' wasn't found.</body></html>";
        sendAnswer(code, TEXT_HTML, answer);
    }

    private Writer writeHeader(@Nonnull final HttpCode code) {
        final Writer writer = request.getWriter();
        writer.println(request.getVersion().getName() + " " + code.getCode() + " " + code.getDescr());
        writer.println(DATE.getName() + ": " + DateUtil.getHttpTime());
        writer.println(SERVER.getName() + ": " + Version.nameAndVersion());
        return writer;
    }
}
