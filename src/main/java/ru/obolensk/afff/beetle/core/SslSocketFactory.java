package ru.obolensk.afff.beetle.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import ru.obolensk.afff.beetle.settings.Config;

import static ru.obolensk.afff.beetle.settings.Options.ROOT_DIR;
import static ru.obolensk.afff.beetle.settings.Options.SERVER_PORT;
import static ru.obolensk.afff.beetle.settings.Options.SSL_KEYSTORE;
import static ru.obolensk.afff.beetle.settings.Options.SSL_KEYSTORE_PASS;

/**
 * Created by Afff on 05.09.2017.
 */
public class SslSocketFactory {

	private static final String DEFAULT_KEYSTORE_FORMAT = "PKCS12";
	private static final String DEFAULT_SERTIFICATE_FORMAT = "SunX509";
	private static final String DEFAULT_SSL_PROTOCOL = "TLS";

	@Nonnull
	public static ServerSocket createSslServerSocket(@Nonnull final Config config) throws IOException, GeneralSecurityException {
		final int port = config.get(SERVER_PORT);
		final Path root = config.get(ROOT_DIR);
		final Path keystore = root.resolve((Path) config.get(SSL_KEYSTORE));
		final String keystorePass = config.get(SSL_KEYSTORE_PASS);
		final SSLContext sc = getSslContext(keystore, keystorePass.toCharArray());
		final SSLServerSocketFactory ssf = sc.getServerSocketFactory();
		return ssf.createServerSocket(port);
	}

	@Nonnull
	public static Socket createSslSocket(@Nonnull final Path keystore,
										 @Nonnull final String keystorePass,
										 @Nonnull final Socket socket) throws IOException, GeneralSecurityException {
		final SSLContext sc = getSslContext(keystore, keystorePass.toCharArray());
		final SSLSocketFactory sf = sc.getSocketFactory();
		return sf.createSocket(socket, socket.getLocalAddress().toString(), socket.getLocalPort(), true);
	}

	private static SSLContext getSslContext(final @Nonnull Path keystore, final char[] keystorePass) throws GeneralSecurityException, IOException {
		final KeyStore ks = KeyStore.getInstance(DEFAULT_KEYSTORE_FORMAT);
		ks.load(new FileInputStream(keystore.toFile()), keystorePass);

		final KeyManagerFactory kmf = KeyManagerFactory.getInstance(DEFAULT_SERTIFICATE_FORMAT);
		kmf.init(ks, keystorePass);

		final TrustManagerFactory tmf = TrustManagerFactory.getInstance(DEFAULT_SERTIFICATE_FORMAT);
		tmf.init(ks);

		final SSLContext sc = SSLContext.getInstance(DEFAULT_SSL_PROTOCOL);
		TrustManager[] trustManagers = tmf.getTrustManagers();
		sc.init(kmf.getKeyManagers(), trustManagers, null);
		return sc;
	}
}
