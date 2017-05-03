package ru.obolensk.afff.beetle.test;

import ru.obolensk.afff.beetle.conn.MimeType;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.HttpMethod;
import ru.obolensk.afff.beetle.request.HttpVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import static ru.obolensk.afff.beetle.request.HttpHeader.*;
import static ru.obolensk.afff.beetle.request.HttpHeaderValue.CONNECTION_KEEP_ALIVE;
import static ru.obolensk.afff.beetle.request.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.request.HttpVersion.HTTP_1_1;

/**
 * Created by Afff on 21.04.2017.
 */
public class HttpTestClient implements Closeable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final HttpVersion version;

    public HttpTestClient(final int port) throws IOException {
        this(port, HTTP_1_1);
    }

    private HttpTestClient(final int port, @Nonnull final HttpVersion version) throws IOException {
        this.socket = new Socket("localhost", port);
        this.socket.setTcpNoDelay(true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.version = version;
    }

    public ServerAnswer sendRequest(@Nonnull final HttpMethod method,
                                    @Nonnull final String path,
                                    @Nullable final String content,
                                    @Nullable final MimeType contentType) throws IOException {
        return sendRequest(method, path, Collections.emptyList(), content, contentType);
    }

    public ServerAnswer sendRequest(@Nonnull final HttpMethod method,
                                    @Nonnull final String path,
                                    @Nullable final List<HeaderValue> headers,
                                    @Nullable final String content,
                                    @Nullable final MimeType contentType) throws IOException {
        System.out.println("[TEST] wrote request " + method.name() + " " + path + " " + version.getName());
        writer.write(method.name() + " " + path + " " + version.getName());
        writer.newLine();
        if (contentType != null) {
            writer.write(CONTENT_TYPE.getName() + ": " + contentType.getName());
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