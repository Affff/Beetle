package ru.obolensk.afff.beetle.core;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import ru.obolensk.afff.beetle.log.Logger;
import ru.obolensk.afff.beetle.protocol.HttpCode;
import ru.obolensk.afff.beetle.request.MultipartData;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.servlet.ServletContainer;
import ru.obolensk.afff.beetle.servlet.ServletResponse;
import ru.obolensk.afff.beetle.settings.Config;
import ru.obolensk.afff.beetle.settings.Options;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_200;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_201;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_400;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_500;
import static ru.obolensk.afff.beetle.settings.Options.ROOT_DIR;
import static ru.obolensk.afff.beetle.settings.Options.SERVLET_DIR;
import static ru.obolensk.afff.beetle.settings.Options.TEMP_DIR;
import static ru.obolensk.afff.beetle.settings.Options.WELCOME_FILE_NAME;
import static ru.obolensk.afff.beetle.settings.Options.WWW_DIR;
import static ru.obolensk.afff.beetle.util.StreamUtil.copy;

/**
 * Created by Afff on 10.04.2017.
 */
public class Storage {

    private static final Logger logger = new Logger(ClientConnection.class);

    private static final String URI_SEPARATOR = "/";

    public static final Path ROOT = Paths.get(URI_SEPARATOR);

    @Getter
    private final Config config;

    @Getter
    private final ServletContainer servletContainer;

    public Storage(@Nonnull final Config config) throws IOException {
        this.config = config;
        this.servletContainer = new ServletContainer(this);
    }

	@Nonnull
    public Path getRootDir() {
        return config.get(ROOT_DIR);
    }

    @Nonnull
    public Path getWwwDir() {
        return getPathFor(WWW_DIR);
    }

    @Nonnull
    public Path getServletDir() {
        return getPathFor(SERVLET_DIR);
    }

	@Nonnull
    public Path getTempDir() {
		return getPathFor(TEMP_DIR);
	}

    private Path getPathFor(@Nonnull final Options option) {
        return getRootDir().resolve((Path) config.get(option));
    }

    @Nonnull
    private String getWelcomeFileName() {
        return config.get(WELCOME_FILE_NAME);
    }

    @Nullable
    public Path getFilePath(@Nonnull String path) {
        path = path.substring(1); // delete starting '/'
        final Path rootDir = getWwwDir();
        final Path targetPath = path.isEmpty() ? rootDir.resolve(getWelcomeFileName())
                : path.endsWith(URI_SEPARATOR) ? rootDir.resolve(path).resolve(getWelcomeFileName()) : rootDir.resolve(path);
        if (!targetPath.normalize().startsWith(rootDir)) { // check prevents out-of-root-dir attacks
            return null;
        }
        return targetPath;
    }

    @Nullable
    public boolean execute(@Nonnull final Request req, @Nullable Path path, @Nonnull ResponseWriter writer) {
        if (path == null) { // wrong server path
            return false;
        }
        final ServletResponse response = servletContainer.process(req, path);
        if (response != null) {
            writer.sendServletResponse(response);
            return true;
        }
        return false;
    }

    public HttpCode putFile(@Nonnull final Request req, Path file) {
        if (file == null) {
            return HTTP_400; // wrong server path
        }
        final boolean exists = exists(file);
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
                writer.close();
                if (actualSize == size) {
                    move(tempFile, file, REPLACE_EXISTING);
                } else {
                    responseCode = HTTP_500;
                }
            } catch (IOException e) {
                logger.error(e);
                responseCode = HTTP_500;
            } finally {
                if (tempFile != null && exists(tempFile)) {
                    try {
                        delete(tempFile);
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            }
        }
        return responseCode;
    }

    public boolean readMultipartFileToRequest(@Nonnull final Request req, @Nonnull final String filename, @Nonnull final String fileAsStr)
			throws IOException
	{
        final Path file = getFilePath(req.getPathForFile(filename));
        if (file == null) {
            return false; // wrong server path
        }
		req.getMultipartData().add(new MultipartData(filename, file, fileAsStr, getTempDir()));
		return true;
    }

	public HttpCode putMultipartFile(MultipartData data) {
		final boolean exists = exists(data.getFilePath());
		HttpCode responseCode = exists ? HTTP_200 : HTTP_201;
		try {
			Files.move(data.getContentFile(), data.getFilePath(), REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error(e);
			responseCode = HTTP_500;
		}
		return responseCode;
	}

    public void updateContext() throws IOException {
        servletContainer.update();
    }
}
