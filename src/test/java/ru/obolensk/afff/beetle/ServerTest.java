package ru.obolensk.afff.beetle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.obolensk.afff.beetle.settings.ServerConfig;
import ru.obolensk.afff.beetle.test.HeaderValue;
import ru.obolensk.afff.beetle.test.HttpTestClient;
import ru.obolensk.afff.beetle.test.ServerAnswer;
import ru.obolensk.afff.beetle.test.TestProxy;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static org.junit.Assert.*;
import static ru.obolensk.afff.beetle.conn.MimeType.APPLICATION_X_WWW_FORM_URLENCODED;
import static ru.obolensk.afff.beetle.conn.MimeType.TEXT_PLAIN;
import static ru.obolensk.afff.beetle.request.HttpCode.*;
import static ru.obolensk.afff.beetle.request.HttpHeader.CONNECTION;
import static ru.obolensk.afff.beetle.request.HttpHeaderValue.CONNECTION_CLOSE;
import static ru.obolensk.afff.beetle.request.HttpHeaderValue.CONNECTION_KEEP_ALIVE;
import static ru.obolensk.afff.beetle.request.HttpMethod.*;
import static ru.obolensk.afff.beetle.settings.Options.REQUEST_MAX_LINE_LENGTH;
import static ru.obolensk.afff.beetle.settings.Options.WWW_ROOT_DIR;

/**
 * Created by Afff on 21.04.2017.
 */
public class ServerTest {

    private static final boolean TEST_PROXY_ENABLED = false;
    private static final int PROXY_PORT = 4079;
    private static final int SERVER_PORT = 4080;

    private static ServerConfig config;
    private static BeetleServer server;
    private static TestProxy proxy;

    @BeforeClass
    public static void init() throws IOException, URISyntaxException {
        config = new ServerConfig();
        config.set(WWW_ROOT_DIR, Paths.get(ServerTest.class.getResource("/www").toURI()));
        server = new BeetleServer(SERVER_PORT, config);
        if (TEST_PROXY_ENABLED) {
            proxy = new TestProxy(PROXY_PORT, SERVER_PORT);
        }
    }

    private static int serverPort() {
        return TEST_PROXY_ENABLED ? PROXY_PORT : SERVER_PORT;
    }

    @AfterClass
    public static void destroy() throws IOException {
        try {
            if (proxy != null) {
                proxy.close();
            }
        } finally {
            server.close();
        }
    }

    @Test
    public void simpleGetTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/", null, null);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void nestedGetTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/test/test.html", null, null);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/test/test.html"), result.getReceivedContent());
        }
    }

    @Test
    public void getParametersTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/test/test.html?test1=test1&test2=" + encode("другой язык", UTF_8.name()), null, null);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/test/test.html"), result.getReceivedContent());
            // TODO check input parameters values with servlet
        }
    }

    @Test
    public void simplePostTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(POST, "/", null, APPLICATION_X_WWW_FORM_URLENCODED);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void plainPostUnsupportedTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(POST, "/", null, TEXT_PLAIN);
            assertEquals(HTTP_415, result.getCode());
            Assert.assertNull(result.getReceivedContent());
        }
    }

    @Test
    public void parametersPostTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final String params = "test1=test1&test2=" + encode("другой язык", UTF_8.name());
            final ServerAnswer result = client.sendRequest(POST, "/", params, APPLICATION_X_WWW_FORM_URLENCODED);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void nestedPostTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(POST, "/test/test.html", null, APPLICATION_X_WWW_FORM_URLENCODED);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/test/test.html"), result.getReceivedContent());
        }
    }

    @Test
    public void simpleHeadTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(HEAD, "/", null, null);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(null, result.getReceivedContent());
        }
    }

    @Test
    public void simplePutDeleteTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(PUT, "/to_delete/delete.html", readFileAsString("/www/index.html"), TEXT_PLAIN);
            assertEquals(HTTP_201, result.getCode());
            assertEquals(null, result.getReceivedContent());
            final ServerAnswer result2 = client.sendRequest(PUT, "/to_delete/delete.html", readFileAsString("/www/index.html"), TEXT_PLAIN);
            assertEquals(HTTP_200, result2.getCode());
            assertEquals(null, result2.getReceivedContent());
            final ServerAnswer result3 = client.sendRequest(DELETE, "/to_delete/delete.html", null, null);
            assertEquals(HTTP_200, result3.getCode());
            assertEquals(null, result3.getReceivedContent());
        }
    }

    @Test
    public void tooLongRequestTest() throws IOException, URISyntaxException, InterruptedException {
        config.set(REQUEST_MAX_LINE_LENGTH, 1);
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/test", null, null);
            assertEquals(HTTP_414, result.getCode());
            Assert.assertNull(result.getReceivedContent());
        } finally {
            config.set(REQUEST_MAX_LINE_LENGTH, REQUEST_MAX_LINE_LENGTH.getDefaultValue());
        }
    }

    @Test
    public void outOfWwwRootAttackTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/../test/../", null, null);
            assertEquals(HTTP_404, result.getCode());
            assertNotNull(result.getReceivedContent());
        }
    }

    @Test
    public void multiplyCloseHeader() throws IOException, URISyntaxException, InterruptedException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/",
                    Arrays.asList(
                            new HeaderValue(CONNECTION, CONNECTION_CLOSE),
                            new HeaderValue(CONNECTION, CONNECTION_KEEP_ALIVE)
                    ),null, null);
            assertEquals(HTTP_200, result.getCode());
            assertNotNull(result.getReceivedContent());
            try {
                client.sendRequest(GET, "/", null, null);
                fail();
            } catch (SocketException e) {
            }
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
