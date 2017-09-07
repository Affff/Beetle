package ru.obolensk.afff.beetle;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.obolensk.afff.beetle.core.BeetleServer;
import ru.obolensk.afff.beetle.settings.ServerConfig;
import ru.obolensk.afff.beetle.test.HeaderValue;
import ru.obolensk.afff.beetle.test.HttpTestClient;
import ru.obolensk.afff.beetle.test.ServerAnswer;
import ru.obolensk.afff.beetle.test.TestProxy;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_200;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_201;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_400;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_404;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_414;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_415;
import static ru.obolensk.afff.beetle.protocol.HttpHeader.CONNECTION;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValue.CONNECTION_CLOSE;
import static ru.obolensk.afff.beetle.protocol.HttpHeaderValue.CONNECTION_KEEP_ALIVE;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.DELETE;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.GET;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.HEAD;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.POST;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.PUT;
import static ru.obolensk.afff.beetle.protocol.MimeType.APPLICATION_X_WWW_FORM_URLENCODED;
import static ru.obolensk.afff.beetle.protocol.MimeType.MULTIPART_FORM_DATA;
import static ru.obolensk.afff.beetle.protocol.MimeType.TEXT_PLAIN;
import static ru.obolensk.afff.beetle.settings.Options.REQUEST_MAX_LINE_LENGTH;
import static ru.obolensk.afff.beetle.settings.Options.ROOT_DIR;
import static ru.obolensk.afff.beetle.settings.Options.SERVER_PORT;
import static ru.obolensk.afff.beetle.settings.Options.SERVLETS_ENABLED;

/**
 * Created by Afff on 21.04.2017.
 */
public class ServerTest extends AbstractServerTest {

    private static final boolean TEST_PROXY_ENABLED = false;
    private static final int TEST_PROXY_PORT = 4079;

    private static TestProxy proxy;

    @BeforeClass
    public static void init() throws IOException, GeneralSecurityException, URISyntaxException {
        config = new ServerConfig();
        config.set(SERVER_PORT, serverPort());
        config.set(ROOT_DIR, getTestResourcesDir());
        config.set(SERVLETS_ENABLED, true);
        server = new BeetleServer(config);
        if (TEST_PROXY_ENABLED) {
            proxy = new TestProxy(TEST_PROXY_PORT, TEST_SERVER_PORT);
        }
    }

    private static int serverPort() {
        return TEST_PROXY_ENABLED ? TEST_PROXY_PORT : TEST_SERVER_PORT;
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
    public void parametersPostMultipartTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final String boundary = "RaNdOmDeLiMiTeR";
            final String boundaryAttr = "boundary=" + boundary;
            final String params = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"test_param\"\r\n"
                    + "\r\n"
                    + "test_value\r\n"
                    + "--" + boundary + "--\r\n";
            final ServerAnswer result = client.sendRequest(POST, "/", null, params, MULTIPART_FORM_DATA, boundaryAttr);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }

    @Test
    public void filesPostMultipartTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final String boundary = "RaNdOmDeLiMiTeR";
            final String boundaryAttr = "boundary=" + boundary;
            final String params = "--" + boundary + "\r\n"
                    + "Content-Disposition: file; filename=\"test.txt\"\r\n"
                    + "\r\n"
                    + "test_value\r\n"
                    + "--" + boundary + "--\r\n";
            final ServerAnswer result = client.sendRequest(POST, "/", null, params, MULTIPART_FORM_DATA, boundaryAttr);
            assertEquals(HTTP_200, result.getCode());
            assertEquals("test_value", readFileAsString("/www/test.txt"));
            Files.delete(getTestResourcesDir().resolve("www/test.txt"));
        }
    }

    @Test
    public void base64PostMultipartTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final String boundary = "RaNdOmDeLiMiTeR";
            final String boundaryAttr = "boundary=" + boundary;
            final String params = "--" + boundary + "\r\n"
                    + "Content-Disposition: file; filename=\"test.txt\"\r\n"
                    + "Content-Transfer-Encoding: Base64\r\n"
                    + "\r\n"
                    + new Base64().encodeAsString("test_value".getBytes()) + "\r\n"
                    + "--" + boundary + "--\r\n";
            final ServerAnswer result = client.sendRequest(POST, "/", null, params, MULTIPART_FORM_DATA, boundaryAttr);
            assertEquals(HTTP_200, result.getCode());
            assertEquals("test_value", readFileAsString("/www/test.txt"));
            Files.delete(getTestResourcesDir().resolve("www/test.txt"));
        }
    }

    @Test
    public void quotedPrintablePostMultipartTest() throws IOException, URISyntaxException, EncoderException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final String boundary = "RaNdOmDeLiMiTeR";
            final String boundaryAttr = "boundary=" + boundary;
            final String params = "--" + boundary + "\r\n"
                    + "Content-Disposition: file; filename=\"test.txt\"\r\n"
                    + "Content-Transfer-Encoding: quoted-printable\r\n"
                    + "\r\n"
                    + new QuotedPrintableCodec().encode("test_value") + "\r\n"
                    + "--" + boundary + "--\r\n";
            final ServerAnswer result = client.sendRequest(POST, "/", null, params, MULTIPART_FORM_DATA, boundaryAttr);
            assertEquals(HTTP_200, result.getCode());
            assertEquals("test_value", readFileAsString("/www/test.txt"));
            Files.delete(getTestResourcesDir().resolve("www/test.txt"));
        }
    }

    @Test
    public void parametersPostMultipartBrokenTest() throws IOException, URISyntaxException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final String boundary = "RaNdOmDeLiMiTeR";
            final String boundaryAttr = "boundary=" + boundary;
            final String params = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"test_param\"\r\n"
                    + "\r\n"
                    + "test_value\r\n";
            final ServerAnswer result = client.sendRequest(POST, "/", null, params, MULTIPART_FORM_DATA, boundaryAttr);
            assertEquals(HTTP_400, result.getCode());
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
                    ),null, null, null);
            assertEquals(HTTP_200, result.getCode());
            assertNotNull(result.getReceivedContent());
            try {
                client.sendRequest(GET, "/", null, null);
                fail();
            } catch (SocketException e) {
            }
        }
    }

    @Test
    public void servletTest() throws IOException, URISyntaxException, InterruptedException {
        try(final HttpTestClient client = new HttpTestClient(serverPort())) {
            final ServerAnswer result = client.sendRequest(POST, "/serv/echo", "data=test", APPLICATION_X_WWW_FORM_URLENCODED);
            assertEquals(HTTP_200, result.getCode());
            assertEquals("test", result.getReceivedContent());
        }
    }
}
