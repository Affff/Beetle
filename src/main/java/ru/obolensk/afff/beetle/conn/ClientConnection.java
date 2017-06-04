package ru.obolensk.afff.beetle.conn;

import ru.obolensk.afff.beetle.BeetleServer;
import ru.obolensk.afff.beetle.Storage;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.request.HttpMethod;
import ru.obolensk.afff.beetle.request.MultipartDataProcessor;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.request.RequestBuilder;
import ru.obolensk.afff.beetle.stream.LimitedBufferedReader;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Arrays.asList;
import static ru.obolensk.afff.beetle.conn.MimeType.MESSAGE_HTTP;
import static ru.obolensk.afff.beetle.conn.ResponseWriter.sendUnparseableRequestAnswer;
import static ru.obolensk.afff.beetle.request.HttpCode.*;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONNECTION;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.request.HttpHeaderValue.CONNECTION_CLOSE;
import static ru.obolensk.afff.beetle.request.HttpHeaderValueAttribute.BOUNDARY;
import static ru.obolensk.afff.beetle.request.HttpMethod.*;
import static ru.obolensk.afff.beetle.request.HttpVersion.HTTP_1_1;
import static ru.obolensk.afff.beetle.settings.Options.REQUEST_MAX_LINE_LENGTH;
import static ru.obolensk.afff.beetle.util.FileUtils.exists;
import static ru.obolensk.afff.beetle.util.StreamUtil.readAsString;


/**
 * Created by Afff on 10.04.2017.
 */
public class ClientConnection {

    private static final Logger logger = new Logger(ClientConnection.class);

    //TODO support https
    //TODO support JS servlets

    public ClientConnection(@Nonnull final BeetleServer server, @Nonnull final Socket socket) throws IOException {
        logger.debug("Open client connection from {}:{}", socket.getInetAddress(), socket.getPort());
        final int maxLineLimit = server.getConfig().get(REQUEST_MAX_LINE_LENGTH);
        final InputStream inputStream = socket.getInputStream();
        final OutputStream outputStream = socket.getOutputStream();
        try (final LimitedBufferedReader reader
                     = new LimitedBufferedReader(new InputStreamReader(inputStream), maxLineLimit)) {
            String nextReq;
            while ((nextReq = reader.readLine()) != null) {
                if (reader.isLineOverflow()) {
                    sendUnparseableRequestAnswer(outputStream, HTTP_414);
                    continue;
                }
                final RequestBuilder builder = new RequestBuilder(reader, outputStream, nextReq);
                while (builder.addHeader(reader.readLine())) ;
                final Request req = builder.build();
                proceedRequest(req, server.getStorage());
                if (closeRequested(req)) {
                    break;
                }
            }
        }
        socket.close();
        logger.debug("Close client connection on {}:{}", socket.getInetAddress(), socket.getPort());
    }

    private void proceedRequest(@Nonnull final Request req,
                                @Nonnull final Storage storage) throws IOException {
        final ResponseWriter writer = new ResponseWriter(req);
        if (req.isInvalid()) {
            req.skipEntityQuietly();
            writer.sendEmptyAnswer(HTTP_400);
        } else if (req.getVersion() != HTTP_1_1) { //TODO support other versions
            req.skipEntityQuietly();
            writer.sendEmptyAnswer(HTTP_505);
        } else if (req.getMethod() == GET
                || req.getMethod() == HEAD
                || req.getMethod() == POST) {
            if (req.getMethod() == POST) {
                switch(req.getContentType()) {
                    case APPLICATION_X_WWW_FORM_URLENCODED: {
                        if (req.hasEntity()) {
                            req.parseParams(readAsString(req.getReader(), req.getEntitySize()));
                        }
                        break;
                    }
                    case MULTIPART_FORM_DATA : {
                        final String boundary = req.getHeaderAttribute(CONTENT_TYPE, BOUNDARY);
                        final Integer entitySize = req.getEntitySize();
                        if (boundary == null || entitySize == null) {
                            req.skipEntityQuietly();
                            writer.sendEmptyAnswer(HTTP_400);
                            return;
                        }
                        int counter = entitySize;
                        final MultipartDataProcessor processor = new MultipartDataProcessor(req, boundary);
                        while (counter > 0) {
                            if (!processor.step(req.getReader().readLine())) {
                                break;
                            }
                            counter -= req.getReader().getLastLineSize();
                        }
                    }
                    default:  {
                        req.skipEntityQuietly();
                        writer.sendEmptyAnswer(HTTP_415);
                        return;
                    }
                }
            } else {
                req.skipEntityQuietly();
            }
            final Path file = storage.getFilePath(req.getLocalPath());
            if (!exists(file)) {
                writer.send404();
            } else {
                writer.sendFile(file);
            }
        } else if (req.getMethod() == PUT) {
            writer.sendEmptyAnswer(storage.putFile(req));
        } else if (req.getMethod() == DELETE) {
            req.skipEntityQuietly();
            final Path file = storage.getFilePath(req.getLocalPath());
            if (!exists(file)) {
                writer.send404();
            } else {
                try {
                    Files.delete(file);
                    writer.sendEmptyAnswer(HTTP_200);
                } catch (IOException e) {
                    writer.sendEmptyAnswer(HTTP_500);
                }
            }
        } else if (req.getMethod() == OPTIONS) {
            req.skipEntityQuietly();
            final List<HttpMethod> supportedMethods = asList(HEAD, GET, POST, PUT, DELETE, OPTIONS, TRACE);
            writer.sendOptions(supportedMethods);
        } else if (req.getMethod() == TRACE) {
            req.skipEntityQuietly();
            writer.sendAnswer(HTTP_200, MESSAGE_HTTP, req.getRawData());
        } else if (req.getMethod() == CONNECT) {
            req.skipEntityQuietly();
            writer.sendConnected();
        } else if (req.getMethod() == UNKNOWN) {
            req.skipEntityQuietly();
            writer.sendEmptyAnswer(HTTP_505);
        } else {
            req.skipEntityQuietly();
            writer.sendEmptyAnswer(HTTP_501);
        }
    }

    private boolean closeRequested(@Nonnull final Request req) {
        return req.hasHeaderValue(CONNECTION, CONNECTION_CLOSE);
    }
}
