package ru.obolensk.afff.beetle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.obolensk.afff.beetle.core.BeetleServer;
import ru.obolensk.afff.beetle.settings.ServerConfig;
import ru.obolensk.afff.beetle.test.HttpTestClient;
import ru.obolensk.afff.beetle.test.ServerAnswer;

import static org.junit.Assert.assertEquals;
import static ru.obolensk.afff.beetle.protocol.HttpCode.HTTP_200;
import static ru.obolensk.afff.beetle.protocol.HttpMethod.GET;
import static ru.obolensk.afff.beetle.settings.Options.ROOT_DIR;
import static ru.obolensk.afff.beetle.settings.Options.SERVER_PORT;
import static ru.obolensk.afff.beetle.settings.Options.SERVLETS_ENABLED;
import static ru.obolensk.afff.beetle.settings.Options.SSL_ENABLED;
import static ru.obolensk.afff.beetle.settings.Options.SSL_KEYSTORE;
import static ru.obolensk.afff.beetle.settings.Options.SSL_KEYSTORE_PASS;

/**
 * Created by Afff on 21.04.2017.
 */
public class SslServerTest extends AbstractServerTest {

    @BeforeClass
    public static void init() throws IOException, GeneralSecurityException, URISyntaxException {
        config = new ServerConfig();
        config.set(SERVER_PORT, serverPort());
        config.set(ROOT_DIR, getTestResourcesDir());
        config.set(SERVLETS_ENABLED, true);
        config.set(SSL_ENABLED, true);
        config.set(SSL_KEYSTORE, getKeystore());
        config.set(SSL_KEYSTORE_PASS, getKeystorePass());
        server = new BeetleServer(config);
    }

    private static int serverPort() {
        return TEST_SERVER_PORT;
    }

    private static Path getKeystore() throws URISyntaxException {
        return getTestResourcesDir().resolve("keystore/test.pfx");
    }

    private static String getKeystorePass() {
        return "testtest";
    }

    @AfterClass
    public static void destroy() throws IOException {
        server.close();
    }

    @Test
    public void simpleGetTest() throws IOException, URISyntaxException, GeneralSecurityException {
        try(final HttpTestClient client = new HttpTestClient(getKeystore(), getKeystorePass(), serverPort())) {
            final ServerAnswer result = client.sendRequest(GET, "/", null, null);
            assertEquals(HTTP_200, result.getCode());
            assertEquals(readFileAsString("/www/index.html"), result.getReceivedContent());
        }
    }
}
