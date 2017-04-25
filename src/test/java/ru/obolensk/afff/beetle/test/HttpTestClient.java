package ru.obolensk.afff.beetle.test;

import ru.obolensk.afff.beetle.conn.MimeType;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.HttpMethod;
import ru.obolensk.afff.beetle.request.HttpVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONNECTION;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_LENGTH;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONTENT_TYPE;
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
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.version = version;
    }

    public ServerAnswer sendRequest(@Nonnull final HttpMethod method,
                                    @Nonnull final String path,
                                    @Nullable final String content,
                                    @Nullable final MimeType contentType) throws IOException {
        writer.write(method.name() + " " + path + " " + version.getName());
        writer.newLine();
        if (contentType != null) {
            writer.write(CONTENT_TYPE.getName() + ": " + contentType.getName());
            writer.newLine();
        }
        byte[] contentBytes = null;
        if (content != null) {
            contentBytes = content.getBytes(UTF_8);
            writer.write(CONTENT_LENGTH.getName() + ": " + contentBytes.length);
            writer.newLine();
        }
        writer.write(CONNECTION.getName() + ": " + CONNECTION_KEEP_ALIVE.getName());
        writer.newLine();
        writer.newLine();
        writer.flush();
        if (contentBytes != null) {
            socket.getOutputStream().write(contentBytes);
            socket.getOutputStream().flush();
        }
        final String response = reader.readLine();
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
        }
        return new ServerAnswer(statusCode, receivedContent);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}