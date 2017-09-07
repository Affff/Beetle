package ru.obolensk.afff.beetle.request;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;

/**
 * Created by Afff on 30.08.2017.
 */
public class MultipartData implements MultipartDataInfo {

	@Getter
	private final String fileName;

	@Getter
	private final Path targetPath;

	@Getter
	private final Path contentFile;

	private InputStream contentStream;

	public MultipartData(String fileName, Path targetPath, String content, Path tempDir) throws IOException {
		this.fileName = fileName;
		this.targetPath = targetPath;
		this.contentFile = Files.createTempFile(tempDir, null, null);
		Files.write(contentFile, content.getBytes());
	}

	@Override
	public InputStream getContent() throws IOException {
		if (contentStream == null) {
			contentStream = Files.newInputStream(contentFile);
		}
		return contentStream;
	}

	@Override
	public void close() throws IOException {
		if (contentStream != null) {
			contentStream.close();
		}
	}
}
