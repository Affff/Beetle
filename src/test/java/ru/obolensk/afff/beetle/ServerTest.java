package ru.obolensk.afff.beetle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.obolensk.afff.beetle.request.HttpCode;
import ru.obolensk.afff.beetle.request.HttpMethod;
import ru.obolensk.afff.beetle.settings.ServerConfig;
import ru.obolensk.afff.beetle.test.HttpTestClient;
import ru.obolensk.afff.beetle.test.ServerAnswer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static ru.obolensk.afff.beetle.conn.MimeType.APPLICATION_X_WWW_FORM_URLENCODED;
import static ru.obolensk.afff.beetle.conn.MimeType.TEXT_PLAIN;
import static ru.obolensk.afff.beetle.request.HttpCode.HTTP_415;
import static ru.obolensk.afff.beetle.settings.Options.REQUEST_MAX_LINE_LENGHT;
import static ru.obolensk.afff.beetle.settings.Options.WWW_ROOT_DIR;

/**
 * Created by Afff on 21.04.2017.
 */
public class ServerTest {

    private static final int SERVER_PORT = 4080;

    private static ServerConfig config;
    private static BeetleServer server;

    @BeforeClass
    public static void init() throws IOException, URISyntaxException {
        config = new ServerConfig();
        config.set(WWW_ROOT_DIR, Paths.get(ServerTest.class.getResource("/www").toURI()));
        server = new BeetleServer(SERVER_PORT, config);
    }

    @AfterClass
    public static void destroy() {
        server.close();
    }

    @Test
    public void simpleGetTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.GET, "/", null, null);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void nestedGetTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.GET, "/test/test.html", null, null);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(readFileAsString("/www/test/test.html"), result.getReceivedContent());
        }
    }

    @Test
    public void getParametersTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.GET, "/test/test.html?test1=test1&test2=" + encode("другой язык", UTF_8.name()), null, null);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(readFileAsString("/www/test/test.html"), result.getReceivedContent());
            // TODO check input parameters values with servlet
        }
    }

    @Test
    public void simplePostTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.POST, "/", null, APPLICATION_X_WWW_FORM_URLENCODED);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void plainPostUnsupportedTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.POST, "/", null, TEXT_PLAIN);
            Assert.assertEquals(HTTP_415, result.getCode());
            Assert.assertNull(result.getReceivedContent());
        }
    }

    @Test
    public void parametersPostTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final String params = "test1=test1&test2=" + encode("другой язык", UTF_8.name());
            final ServerAnswer result = client.sendRequest(HttpMethod.POST, "/", params, APPLICATION_X_WWW_FORM_URLENCODED);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void nestedPostTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.POST, "/test/test.html", null, APPLICATION_X_WWW_FORM_URLENCODED);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(readFileAsString("/www/test/test.html"), result.getReceivedContent());
        }
    }

    @Test
    public void simpleHeadTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.HEAD, "/", null, null);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(null, result.getReceivedContent());
        }
    }

    @Test
    public void simplePutDeleteTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.PUT, "/to_delete/delete.html", readFileAsString("/www/index.html"), TEXT_PLAIN);
            Assert.assertEquals(HttpCode.HTTP_200, result.getCode());
            Assert.assertEquals(null, result.getReceivedContent());
        }
    }

    @Test
    public void tooLongRequestTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            config.set(REQUEST_MAX_LINE_LENGHT, 1);
            final ServerAnswer result = client.sendRequest(HttpMethod.GET, "/test", null, null);
            Assert.assertEquals(HttpCode.HTTP_414, result.getCode());
            Assert.assertNull(result.getReceivedContent());
            config.set(REQUEST_MAX_LINE_LENGHT, REQUEST_MAX_LINE_LENGHT.getDefaultValue());
        }
    }

    @Test
    public void outOfWwwRootAttackTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(SERVER_PORT)) {
            final ServerAnswer result = client.sendRequest(HttpMethod.GET, "/../test/../", null, null);
            Assert.assertEquals(HttpCode.HTTP_404, result.getCode());
            Assert.assertNotNull(result.getReceivedContent());
        }
    }

    private String readFileAsString(@Nonnull final String fileName) throws URISyntaxException, IOException {
        final StringBuilder builder = new StringBuilder();
        final Path file = Paths.get(getClass().getResource(fileName).toURI());
        readAllLines(file).forEach(s -> builder.append(s).append('\r').append('\n'));
        builder.setLength(builder.length() - 2); // remove last line break
        return builder.toString();
    }
}
