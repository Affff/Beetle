package ru.obolensk.afff.beetle.core;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import lombok.Getter;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.settings.Config;
import ru.obolensk.afff.beetle.settings.ServerConfig;
import ru.obolensk.afff.beetle.util.Version;

import static ru.obolensk.afff.beetle.core.SslSocketFactory.createSslServerSocket;
import static ru.obolensk.afff.beetle.settings.Options.LOG_LEVEL;
import static ru.obolensk.afff.beetle.settings.Options.LOG_TO_CONSOLE;
import static ru.obolensk.afff.beetle.settings.Options.SERVER_PORT;
import static ru.obolensk.afff.beetle.settings.Options.SERVER_THREAD_COUNT;
import static ru.obolensk.afff.beetle.settings.Options.SERVLET_REFRESH_FILES_SERVICE_ENABLED;
import static ru.obolensk.afff.beetle.settings.Options.SERVLET_REFRESH_FILES_SERVICE_INTERVAL;
import static ru.obolensk.afff.beetle.settings.Options.SSL_ENABLED;

/**
 * Created by Afff on 10.04.2017.
 */
public class BeetleServer implements Closeable {

    private static final Logger logger = new Logger(BeetleServer.class);

    @Getter
    private final Config config;

    @Getter
    private final Storage storage;

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private volatile boolean terminated;

    private final Thread mainLoop;
    private final Thread updateServletLoop;

    public BeetleServer() throws IOException, GeneralSecurityException {
        this(new ServerConfig());
    }

    public BeetleServer(@Nonnull final Config config) throws IOException, GeneralSecurityException {
        if (config.get(LOG_TO_CONSOLE)) {
            Logger.addConsoleAppender(config.get(LOG_LEVEL));
        }
        final int port = config.get(SERVER_PORT);
        logger.info("Starting {} on port {}...", Version.nameAndVersion(), port);
        this.config = config;
        this.storage = new Storage(config);
        this.executor = Executors.newFixedThreadPool(config.get(SERVER_THREAD_COUNT));
        this.serverSocket = config.is(SSL_ENABLED) ? createSslServerSocket(config) : new ServerSocket(port);
        final Runnable mainLoopRunnable = () -> {
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
        this.mainLoop = new Thread(mainLoopRunnable);
        mainLoop.start();
        logger.info("Server was started.");
        if (config.is(SERVLET_REFRESH_FILES_SERVICE_ENABLED)) {
            final Runnable updateServletLoopRunnable = () -> {
                while(!terminated) {
                    try {
                        Thread.sleep(((Number) config.get(SERVLET_REFRESH_FILES_SERVICE_INTERVAL)).longValue());
                    } catch (InterruptedException e) {
                        logger.trace(e.getMessage(), e);
                    }
                    try {
                        storage.updateContext();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info("Servlet update service was stopped.");
            };
            this.updateServletLoop = new Thread(updateServletLoopRunnable);
            updateServletLoop.start();
            logger.info("Servlet update service was started.");
        } else {
            this.updateServletLoop = null;
        }
    }

    @Override
    public void close() {
        terminated = true;
        mainLoop.interrupt();
        if (updateServletLoop != null) {
            updateServletLoop.interrupt();
        }
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
                new ClientConnection(this, accept);
            } catch (IOException e) {
                logger.error(e);
            }
        });
    }
}
