package ru.obolensk.afff.beetle.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.obolensk.afff.beetle.core.SslSocketFactory;
import ru.obolensk.afff.beetle.protocol.HttpCode;
import ru.obolensk.afff.beetle.protocol.HttpMethod;
import ru.obolensk.afff.beetle.protocol.HttpVersion;
import ru.obolensk.afff.beetle.protocol.MimeType;

import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONNECTION;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_LENGTH;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONTENT_TYPE;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValue.CONNECTION_KEEP_ALIVE;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.protocol.HttpVersion.HTTP_1_1;

/**
 * Created by Afff on 21.04.2017.
 */
public class HttpTestClient implements Closeable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final HttpVersion version;

    public HttpTestClient(final int port) throws IOException {
        this(createPlainSocket("localhost", port), HTTP_1_1);
    }

    public HttpTestClient(@Nonnull final Path keystore, @Nonnull final String keystorePass, final int port)
            throws IOException, GeneralSecurityException {
        this(createSslSocket(keystore, keystorePass, "localhost", port), HTTP_1_1);
    }

    private HttpTestClient(final Socket socket, @Nonnull final HttpVersion version) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.version = version;
    }

    private static Socket createPlainSocket(@Nonnull final String localhost, final int port) throws IOException {
        final Socket socket = new Socket(localhost, port);
        socket.setTcpNoDelay(true);
        return socket;
    }

    private static Socket createSslSocket(@Nonnull final Path keystore, @Nonnull final String keystorePass,
                                          @Nonnull final String localhost, final int port) throws IOException, GeneralSecurityException {
        final Socket socket = new Socket(localhost, port);
        socket.setTcpNoDelay(true);
        return SslSocketFactory.createSslSocket(keystore, keystorePass, socket);
    }

    public ServerAnswer sendRequest(@Nonnull final HttpMethod method,
                                    @Nonnull final String path,
                                    @Nullable final String content,
                                    @Nullable final MimeType contentType) throws IOException {
        return sendRequest(method, path, Collections.emptyList(), content, contentType, null);
    }

    public ServerAnswer sendRequest(@Nonnull final HttpMethod method,
                                    @Nonnull final String path,
                                    @Nullable final List<HeaderValue> headers,
                                    @Nullable final String content,
                                    @Nullable final MimeType contentType,
                                    @Nullable final String contentTypeAttrs) throws IOException {
        System.out.println("[TEST] wrote request " + method.name() + " " + path + " " + version.getName());
        writer.write(method.name() + " " + path + " " + version.getName());
        writer.newLine();
        if (contentType != null) {
            writer.write(CONTENT_TYPE.getName() + ": " + contentType.getName());
            if (contentTypeAttrs != null) {
                writer.write("; " + contentTypeAttrs);
            }
            writer.newLine();
        }
        char[] contentBytes = null;
        if (content != null) {
            contentBytes = content.toCharArray();
            writer.write(CONTENT_LENGTH.getName() + ": " + contentBytes.length);
            writer.newLine();
        }
        writer.write(CONNECTION.getName() + ": " + CONNECTION_KEEP_ALIVE.getName());
        writer.newLine();
        if (headers != null) {
            for (final HeaderValue header : headers) {
                writer.write(header.toString());
                writer.newLine();
            }
        }
        writer.newLine();
        if (contentBytes != null) {
            System.out.println("[TEST] write " + contentBytes.length + " bytes of content.");
            writer.write(contentBytes);
            System.out.println("[TEST] content >> " + content);
            System.out.println("[TEST] content was written.");
        }
        writer.flush();
        final String response = reader.readLine();
        if (response == null) {
            throw new SocketException("Connection has already closed!");
        }
        System.out.println("[TEST] read response: " + response);
        final HttpCode statusCode = HttpCode.valueOf("HTTP_" + response.substring("HTTP/1.1 ".length(), "HTTP/1.1 ".length() + 3));
        int contentSize = 0;
        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            if (line.startsWith(CONTENT_LENGTH.getName())) {
                contentSize = Integer.valueOf(line.split(":")[1].trim());
            }
        }
        String receivedContent = null;
        if (contentSize > 0 && method != HEAD) {
            final char[] buf = new char[contentSize];
            assert reader.read(buf, 0, contentSize) == contentSize;
            receivedContent = new String(buf);
            System.out.println("[TEST] read content: " + receivedContent);
        }
        return new ServerAnswer(statusCode, receivedContent);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}