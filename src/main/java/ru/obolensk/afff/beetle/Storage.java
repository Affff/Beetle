package ru.obolensk.afff.beetle;

import ru.obolensk.afff.beetle.conn.ClientConnection;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.settings.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ru.obolensk.afff.beetle.request.HttpCode.*;
import static ru.obolensk.afff.beetle.settings.Options.WELCOME_FILE_NAME;
import static ru.obolensk.afff.beetle.settings.Options.WWW_ROOT_DIR;
import static ru.obolensk.afff.beetle.util.StreamUtil.copy;

/**
 * Created by Afff on 10.04.2017.
 */
public class Storage {

    private static final Logger logger = new Logger(ClientConnection.class);

    private static final String URI_SEPARATOR = "/";

    public static final Path ROOT = Paths.get(URI_SEPARATOR);

    private final Config config;

    public Storage(@Nonnull final Config config) {
        this.config = config;
    }

    @Nonnull
    private Path getRootDir() {
        return config.get(WWW_ROOT_DIR);
    }

    @Nonnull
    private String getWelcomeFileName() {
        return config.get(WELCOME_FILE_NAME);
    }

    @Nullable
    public Path getFilePath(@Nonnull String path) {
        path = path.substring(1); // delete starting '/'
        final Path rootDir = getRootDir();
        final Path targetPath = path.isEmpty() ? rootDir.resolve(getWelcomeFileName())
                : path.endsWith(URI_SEPARATOR) ? rootDir.resolve(path).resolve(getWelcomeFileName()) : rootDir.resolve(path);
        if (!targetPath.normalize().startsWith(rootDir)) { // check prevents out-of-root-dir attacks
            return null;
        }
        return targetPath;
    }

    public HttpCode putFile(@Nonnull final Request req) {
        final Path file = getFilePath(req.getLocalPath());
        if (file == null) {
            return HTTP_400; // wrong server path
        }
        final boolean exists = Files.exists(file);
        HttpCode responseCode = exists ? HTTP_200 : HTTP_201;
        final int size = req.getEntitySize() != null ? req.getEntitySize() : 0;
        if (size > 0) {
            Path tempFile = null;
            try {
                if (!exists) {
                    Files.createDirectories(file.getParent());
                }
                tempFile = Files.createTempFile(file.getFileName().toString(), "_temp");
                final Writer writer = Files.newBufferedWriter(tempFile);
                final int actualSize = copy(req.getReader(), writer, size);
                if (actualSize == size) {
                    Files.move(tempFile, file, REPLACE_EXISTING);
                } else {
                    responseCode = HTTP_500;
                }
            } catch (IOException e) {
                logger.error(e);
                responseCode = HTTP_500;
            } finally {
                if (tempFile != null && Files.exists(tempFile)) {
                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            }
        }
        return responseCode;
    }
}
