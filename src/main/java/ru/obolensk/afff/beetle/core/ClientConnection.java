package ru.obolensk.afff.beetle.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.protocol.HttpMethod;
import ru.obolensk.afff.beetle.request.MultipartDataProcessor;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.request.RequestBuilder;
import ru.obolensk.afff.beetle.stream.LimitedBufferedReader;

import static java.util.Arrays.asList;
import static ru.obolensk.afff.beetle.core.ResponseWriter.sendUnparseableRequestAnswer;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_200;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_400;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_414;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_415;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_500;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_501;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_505;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONNECTION;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValue.CONNECTION_CLOSE;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValueAttribute.BOUNDARY;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.CONNECT;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.DELETE;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.GET;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.OPTIONS;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.POST;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.PUT;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.TRACE;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.UNKNOWN;
import static ru.obolensk.afff.beetle.protocol.HttpVersion.HTTP_1_1;
import static ru.obolensk.afff.beetle.protocol.MimeType.MESSAGE_HTTP;
import static ru.obolensk.afff.beetle.settings.Options.REQUEST_MAX_LINE_LENGTH;
import static ru.obolensk.afff.beetle.util.FileUtils.exists;
import static ru.obolensk.afff.beetle.util.StreamUtil.readAsString;


/**
 * Created by Afff on 10.04.2017.
 */
public class ClientConnection {

    private static final Logger logger = new Logger(ClientConnection.class);

    //TODO support HTTP 2.0
    //TODO support WebSockets

    public ClientConnection(@Nonnull final BeetleServer server, @Nonnull final Socket socket) throws IOException {
        final InetAddress ip = socket.getInetAddress();
        logger.debug("Open client connection from {}:{}", ip, socket.getPort());
        final int maxLineLimit = server.getConfig().get(REQUEST_MAX_LINE_LENGTH);
        final InputStream inputStream = socket.getInputStream();
        final OutputStream outputStream = socket.getOutputStream();
        try (final LimitedBufferedReader reader
                     = new LimitedBufferedReader(new InputStreamReader(inputStream), maxLineLimit)) {
            String nextReq;
            while ((nextReq = reader.readLine()) != null) {
                if (reader.isLineOverflow()) {
                    sendUnparseableRequestAnswer(ip, outputStream, HTTP_414);
                    continue;
                }
                final RequestBuilder builder = new RequestBuilder(ip, reader, outputStream, nextReq);
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
        } else if (req.getVersion() != HTTP_1_1) {
            req.skipEntityQuietly();
            writer.sendEmptyAnswer(HTTP_505);
        } else if (req.getMethod() == GET
                || req.getMethod() == HEAD
                || req.getMethod() == POST) {
            if (req.getMethod() == POST) {
             sw: switch(req.getContentType()) {
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
                        final MultipartDataProcessor processor = new MultipartDataProcessor(req, boundary, storage);
                        while (counter > 0) {
                            if (!processor.step(req.getReader().readLine())) {
                                break sw;
                            }
                            counter -= req.getReader().getLastLineSize();
                        }
                        // if multipart sequence ends unexpectedly, send error
                        writer.sendEmptyAnswer(HTTP_400);
                        return;
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
            if (!storage.execute(req, file, writer)) {
                if (!req.getMultipartData().isEmpty()) {
                    req.getMultipartData().forEach(storage::putMultipartFile);
                }
                if (!exists(file)) {
                    writer.send404();
                } else {
                    writer.sendFile(file);
                }
            }
        } else if (req.getMethod() == PUT) {
            final Path file = storage.getFilePath(req.getLocalPath());
            if (!storage.execute(req, file, writer)) {
                writer.sendEmptyAnswer(storage.putFile(req, file));
            }
        } else if (req.getMethod() == DELETE) {
            req.skipEntityQuietly();
            final Path file = storage.getFilePath(req.getLocalPath());
            if (!storage.execute(req, file, writer)) {
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
