package ru.obolensk.afff.beetle;

import ru.obolensk.afff.beetle.conn.ClientConnection;
import ru.obolensk.afff.beetle.log.Logger;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Afff on 10.04.2017.
 */
public class BeetleServer implements Closeable {

    private static final Logger logger = new Logger(BeetleServer.class);

    private static final int DEFAULT_PORT = 80;

    private static final int DEFAULT_THREAD_COUNT = 5; // TODO move to settings

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private volatile boolean terminated;

    public BeetleServer() throws IOException {
        this(DEFAULT_PORT);
    }

    public BeetleServer(final int port) throws IOException {
        Logger.addConsoleAppender(); // TODO switch it by settings
        logger.info("Starting {} on port {}...", Version.nameAndVersion(), port);
        this.executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        this.serverSocket = new ServerSocket(port);
        Runnable mainLoop = () -> {
            while (!terminated) {
                try {
                    proceed(serverSocket.accept());
                } catch (SocketException e) {
                    if (!"socket closed".equals(e.getMessage())) {
                        logger.error(e);
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            logger.info("Server was stopped.");
        };
        new Thread(mainLoop).start();
        logger.info("Server was started.");
    }

    @Override
    public void close() {
        terminated = true;
        try {
            executor.shutdown();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    private void proceed(@Nonnull final Socket accept) {
        executor.submit(() -> {
            try {
                new ClientConnection(accept);
            } catch (IOException e) {
                logger.error(e);
            }
        });
    }
}
