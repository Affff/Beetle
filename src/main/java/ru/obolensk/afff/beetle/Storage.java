package ru.obolensk.afff.beetle;

import ru.obolensk.afff.beetle.conn.ClientConnection;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.settings.Config;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ru.obolensk.afff.beetle.request.HttpCode.*;
import static ru.obolensk.afff.beetle.settings.Options.WWW_ROOT_DIR;
import static ru.obolensk.afff.beetle.util.StreamUtil.copy;

/**
 * Created by Afff on 10.04.2017.
 */
public class Storage {

    //TODO add virtual hosts support
    //FIXME !!!prevent top level attacks for server

    private static final Logger logger = new Logger(ClientConnection.class);

    public static final Path ROOT = Paths.get("/");

    private static final String WELCOME_FILE_NAME = "index.html";

    private final Config config;

    public Storage(@Nonnull final Config config) {
        this.config = config;
    }

    private Path getRootDir() {
        return config.get(WWW_ROOT_DIR);
    }

    @Nonnull
    public Path getFilePath(@Nonnull final Path path) {
        final Path rootDir = getRootDir();
        return ROOT.equals(path) ? rootDir.resolve(WELCOME_FILE_NAME) : rootDir.resolve(path.subpath(0, path.getNameCount())); //TODO add welcome files list
    }

    public HttpCode putFile(@Nonnull final Request req) {
        final Path file = getFilePath(req.getLocalPath());
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
                final OutputStream outputStream = Files.newOutputStream(tempFile);
                final int actualSize = copy(req.getEntityStream(), outputStream, size);
                if (actualSize == size) {
                    Files.move(tempFile, file, REPLACE_EXISTING);
                } else {
                    responseCode = HTTP_500;
                }
            } catch (IOException e) {
                logger.error(e);
                responseCode = HTTP_500;
            } finally {
                if (tempFile != null) {
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
