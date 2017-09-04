package ru.obolensk.afff.beetle.request;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import ru.obolensk.afff.beetle.protocol.ContentTransferEncoding;

import static ru.obolensk.afff.beetle.protocol.ContentTransferEncoding.BASE64;
import static ru.obolensk.afff.beetle.protocol.ContentTransferEncoding.QUOTED_PRINTABLE;

/**
 * Created by Afff on 04.09.2017.
 */
public class ContentDecoder {

	private static final Base64 base64Codec = new Base64();
	private static final QuotedPrintableCodec quotedPrintableCodec = new QuotedPrintableCodec();

	@Nonnull
	public static String decode(@Nonnull final String content, @Nonnull final String transferEncoding) throws IOException {
		final ContentTransferEncoding encoding = ContentTransferEncoding.getByName(transferEncoding);
		if (encoding == BASE64) {
			return new String(base64Codec.decode(content));
		} else if (encoding == QUOTED_PRINTABLE) {
			try {
				return quotedPrintableCodec.decode(content);
			} catch (DecoderException e) {
				throw new IOException(e.getMessage(), e);
			}
		} else {
			return content;
		}
	}
}
