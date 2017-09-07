package ru.obolensk.afff.beetle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;

import ru.obolensk.afff.beetle.core.BeetleServer;
import ru.obolensk.afff.beetle.settings.ServerConfig;

import static java.nio.file.Files.readAllLines;

/**
 * Created by Afff on 06.09.2017.
 */
public abstract class AbstractServerTest {

	protected static final int TEST_SERVER_PORT = 4080;

	protected static ServerConfig config;
	protected static BeetleServer server;

	protected static Path getTestResourcesDir() throws URISyntaxException {
		final ClassLoader classLoader = ServerTest.class.getClassLoader();
		return Paths.get(classLoader.getResource("www/index.html").toURI()).getParent().getParent();
	}

	protected String readFileAsString(@Nonnull final String fileName) throws URISyntaxException, IOException {
		final StringBuilder builder = new StringBuilder();
		final Path file = Paths.get(getClass().getResource(fileName).toURI());
		readAllLines(file).forEach(s -> builder.append(s).append('\r').append('\n'));
		builder.setLength(builder.length() - 2); // remove last line break
		return builder.toString();
	}
}
