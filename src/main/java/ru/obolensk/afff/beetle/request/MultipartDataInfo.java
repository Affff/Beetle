package ru.obolensk.afff.beetle.request;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Afff on 06.09.2017.
 */
public interface MultipartDataInfo extends Closeable {

	String getFileName();

	InputStream getContent() throws IOException;
}
