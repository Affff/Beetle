package ru.obolensk.afff.beetle.test;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by Afff on 26.04.2017.
 */
public class TestProxy implements Closeable {

    private final ServerSocket source;
    private final Socket target;

    public TestProxy(int sourcePort, int targetPort) throws IOException {
        source = new ServerSocket(sourcePort);
        target = new Socket("localhost", targetPort);
        new Thread(() -> {
            try {
                final Socket clientSocket = source.accept();
                byte[] buffer = new byte[1024];
                while (!clientSocket.isClosed() && !target.isClosed()) {
                    System.out.print("[NET>>{" + copy(clientSocket, target, buffer));
                    System.out.println("}-NET]");
                    System.out.print("[NET<<{" + copy(target, clientSocket, buffer));
                    System.out.println("}-NET]");
                }
            } catch (IOException e) {
                if (!"Socket closed".equals(e.getMessage())
                        && !"Socket is closed".equals(e.getMessage())) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String copy(@Nonnull final Socket from, @Nonnull final Socket to, @Nonnull final byte[] buffer) throws IOException {
        final int size = from.getInputStream().read(buffer);
        if (size < 0) {
            return "socket closed!";
        }
        to.getOutputStream().write(buffer, 0, size);
        return new String(buffer, UTF_8);
    }

    @Override
    public void close() throws IOException {
        try {
            source.close();
        } finally {
            target.close();
        }
    }
}
