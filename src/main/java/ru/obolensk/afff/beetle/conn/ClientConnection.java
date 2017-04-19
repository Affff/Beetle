package ru.obolensk.afff.beetle.conn;

import ru.obolensk.afff.beetle.Storage;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.HttpHeader;
import ru.obolensk.afff.beetle.request.HttpMethod;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.request.RequestBuilder;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static ru.obolensk.afff.beetle.conn.MimeType.MESSAGE_HTTP;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_200;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_201;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_400;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_415;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_500;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_501;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_505;
import static ru.obolensk.afff.beetle.request.HttpHeaderValue.CONNECTION_CLOSE;
import static ru.obolensk.afff.beetle.request.HttpMethod.CONNECT;
import static ru.obolensk.afff.beetle.request.HttpMethod.DELETE;
import static ru.obolensk.afff.beetle.request.HttpMethod.GET;
import static ru.obolensk.afff.beetle.request.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.request.HttpMethod.OPTIONS;
import static ru.obolensk.afff.beetle.request.HttpMethod.POST;
import static ru.obolensk.afff.beetle.request.HttpMethod.PUT;
import static ru.obolensk.afff.beetle.request.HttpMethod.TRACE;
import static ru.obolensk.afff.beetle.request.HttpMethod.UNKNOWN;
import static ru.obolensk.afff.beetle.request.HttpVersion.HTTP_1_1;
import static ru.obolensk.afff.beetle.util.StreamUtil.copy;


/**
 * Created by Afff on 10.04.2017.
 */
public class ClientConnection {

    private static final Logger logger = new Logger(ClientConnection.class);

    public ClientConnection(@Nonnull final Socket socket) throws IOException {
        logger.debug("Open client conection from {}:{}", socket.getInetAddress(), socket.getPort());
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String nextReq;
            while ((nextReq = reader.readLine()) != null) { // TODO replace BufferedReader to own BoundedBufferedReader with maximum string lenght method, if max string len exceeded throw "414 Request-URI Too Long"
                final RequestBuilder builder = new RequestBuilder(socket.getOutputStream(), nextReq);
                while (builder.addHeader(reader.readLine())) ;
                builder.appendEnitityIfExists(socket.getInputStream());
                final Request req = builder.build();
                proceedRequest(req);
                if (closeRequested(req)) {
                    break;
                }
            }
            socket.close();
        }
        logger.debug("Close client connection on {}:{}", socket.getInetAddress(), socket.getPort());
    }

    private void proceedRequest(@Nonnull final Request req) {
        //TODO add access rights
        if (req.isInvalid()) {
            req.skipEnitityQuietly();
            ResponseWriter.sendEmptyAnswer(req, HTTP_400);
        } else if (req.getVersion() != HTTP_1_1) { //TODO support other versions
            req.skipEnitityQuietly();
            ResponseWriter.sendEmptyAnswer(req, HTTP_505);
        } else if (req.getMethod() == GET
                || req.getMethod() == HEAD
                || req.getMethod() == POST) {
            if (req.getMethod() == POST) {
                switch(req.getContentType()) {
                    case APPLICATION_X_WWW_FORM_URLENCODED: { // TODO support www form data
                        req.skipEnitityQuietly();
                        ResponseWriter.sendEmptyAnswer(req, HTTP_415);
                        return;
                    }
                    case MULTIPART_FORM_DATA : { // TODO support multipart form data
                        req.skipEnitityQuietly();
                        ResponseWriter.sendEmptyAnswer(req, HTTP_415);
                        return;
                    }
                    case TEXT_PLAIN :
                    default:  {
                        req.skipEnitityQuietly();
                        ResponseWriter.sendEmptyAnswer(req, HTTP_415);
                        return;
                    }
                }
            } else {
                req.skipEnitityQuietly();
            }
            final Path file = Storage.getFilePath(req.getLocalPath());
            if (!Files.exists(file)) {
                ResponseWriter.send404(req);
            } else {
                ResponseWriter.sendFile(req, file);
            }
        } else if (req.getMethod() == PUT) {
            final Path file = Storage.getFilePath(req.getLocalPath());
            final boolean exists = Files.exists(file);
            HttpCode responseCode = exists ? HTTP_200 : HTTP_201;
                try {
                    if (!exists) {
                        Files.createDirectories(file.getParent());
                    }
                    final int size = req.getEntitySize() != null ? req.getEntitySize() : 0;
                    final OutputStream outputStream = Files.newOutputStream(file);
                    copy(req.getEntityStream(), outputStream, size);
                } catch (IOException e) {
                    responseCode = HTTP_500;
                }
            ResponseWriter.sendEmptyAnswer(req, responseCode);
        } else if (req.getMethod() == DELETE) {
            req.skipEnitityQuietly();
            final Path file = Storage.getFilePath(req.getLocalPath());
            if (!Files.exists(file)) {
                ResponseWriter.send404(req);
            } else {
                try {
                    Files.delete(file);
                    ResponseWriter.sendEmptyAnswer(req, HTTP_200);
                } catch (IOException e) {
                    ResponseWriter.sendEmptyAnswer(req, HTTP_500);
                }
            }
        } else if (req.getMethod() == OPTIONS) {
            //TODO support options for nested resources
            req.skipEnitityQuietly();
            final List<HttpMethod> supportedMethods = Arrays.asList(new HttpMethod[] { HEAD, GET, POST, PUT, DELETE, OPTIONS, TRACE  });
            ResponseWriter.sendOptions(req, supportedMethods);
        } else if (req.getMethod() == TRACE) {
            req.skipEnitityQuietly();
            ResponseWriter.sendAnswer(req, HTTP_200, MESSAGE_HTTP, req.getRawData());
        } else if (req.getMethod() == CONNECT) {
            req.skipEnitityQuietly();
            ResponseWriter.sendConnected(req);
        } else if (req.getMethod() == UNKNOWN) {
            req.skipEnitityQuietly();
            ResponseWriter.sendEmptyAnswer(req, HTTP_505);
        } else {
            req.skipEnitityQuietly();
            ResponseWriter.sendEmptyAnswer(req, HTTP_501);
        }
    }

    private boolean closeRequested(@Nonnull final Request req) {
        return req.getHeaderValue(HttpHeader.CONNECTION) //TODO search all values for close, not only the first
                .equalsIgnoreCase(CONNECTION_CLOSE.getName());
    }
}
