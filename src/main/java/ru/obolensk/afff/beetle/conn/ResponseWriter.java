package ru.obolensk.afff.beetle.conn;

import com.google.common.io.Files;
import ru.obolensk.afff.beetle.Version;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.HttpHeader;
import ru.obolensk.afff.beetle.request.HttpMethod;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.util.DateUtil;
import ru.obolensk.afff.beetle.util.Writer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

import static ru.obolensk.afff.beetle.conn.MimeType.TEXT_HTML;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_LENGTH;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.request.HttpHeader.DATE;
import static ru.obolensk.afff.beetle.request.HttpHeader.SERVER;

/**
 * Created by Afff on 11.04.2017.
 */
public class ResponseWriter {

    private static final Logger logger = new Logger(ResponseWriter.class);

    public static void sendAnswer(@Nonnull final Request req, @Nonnull final HttpCode code, @Nonnull final MimeType mimeType, @Nonnull final String content) {
        final Writer writer = writeHeader(req, code);
        final int length = content.getBytes(StandardCharsets.UTF_8).length;
        if (length != 0) {
            writer.println(CONTENT_TYPE.getName() + ":" + mimeType.getName() + charsetAttr(mimeType));
        }
        writer.println(CONTENT_LENGTH.getName() + ": " + length);
        writer.println();
        if (req.shouldWriteBody()) {
            writer.println(content);
        }
        writer.flush();
    }

    @Nonnull
    private static String charsetAttr(@Nonnull final MimeType mimeType) {
        if (mimeType.getCharset() == null) {
            return null;
        }
        return "; charset=" + mimeType.getCharset().name();
    }

    public static void sendOptions(Request req, List<HttpMethod> options) {
        final Writer writer = writeHeader(req, HttpCode.HTTP_200);
        final StringJoiner joiner = new StringJoiner("");
        options.stream().forEach(option -> joiner.add(option.name()));
        writer.println(HttpHeader.ALLOW.getName() + ":" + joiner.toString());
        writer.flush();
    }

    public static void sendConnected(@Nonnull final Request req) {
        final Writer writer = req.getWriter();
        writer.println(req.getVersion().getName() + " " + HttpCode.HTTP_200 + " Connection established");
        writer.println(DATE.getName() + ": " + DateUtil.getHttpTime());
        writer.println(SERVER.getName() + ": " + Version.nameAndVersion());
        writer.flush();
    }

    public static void sendEmptyAnswer(@Nonnull final Request req, @Nonnull final HttpCode code) {
        sendAnswer(req, code, TEXT_HTML, "");
    }

    public static void sendFile(@Nonnull final Request req, @Nonnull final Path path) {
        final HttpCode code = HttpCode.HTTP_200;
        String answer;
        try {
            answer = Files.toString(path.toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e);
            answer = "";
        }
        sendAnswer(req, code, TEXT_HTML, answer);
    }

    public static void send404(@Nonnull final Request req) {
        final HttpCode code = HttpCode.HTTP_404;
        String answer = "<html><header><meta charset=\"UTF-8\"><title>" + Version.nameAndVersion() + ": " + + code.getCode() + " " + code.getDescr() + "</title></header>"
            + "<body>Requested URI '" + req.getUri() + "' wasn't found.</body></html>";
        sendAnswer(req, code, TEXT_HTML, answer);
    }

    private static Writer writeHeader(@Nonnull final Request req, @Nonnull final HttpCode code) {
        final Writer writer = req.getWriter();
        writer.println(req.getVersion().getName() + " " + code.getCode() + " " + code.getDescr());
        writer.println(DATE.getName() + ": " + DateUtil.getHttpTime());
        writer.println(SERVER.getName() + ": " + Version.nameAndVersion());
        return writer;
    }
}
