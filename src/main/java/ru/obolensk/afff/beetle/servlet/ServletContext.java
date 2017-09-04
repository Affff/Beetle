package ru.obolensk.afff.beetle.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.Getter;
import ru.obolensk.afff.beetle.request.MultipartData;
import ru.obolensk.afff.beetle.request.Request;

import static java.util.stream.Collectors.toMap;

/**
 * Created by Afff on 30.08.2017.
 */
public class ServletContext {

	@Getter
	private final String host;

	@Getter
	private final String ip;

	@Getter
	private final String method;

	@Getter
	private final String contentType;

	@Getter
	private final Map<String, String> params;

	private final Map<String, MultipartData> parts;

	public ServletContext(@Nonnull final Request req) {
		host = req.getIp().getHostName();
		ip = req.getIp().getHostAddress();
		method = req.getMethod().name();
		contentType = req.getContentType().getName();
		params = new HashMap<>(req.getParameters());
		parts = req.getMultipartData().stream()
				.collect(toMap(MultipartData::getFileName, data -> data));
	}

	public String getParam(@Nonnull final String paramName) {
		return params.get(paramName);
	}

	public Collection<MultipartData> getParts() {
		return parts.values();
	}

	public MultipartData getPart(final String name) {
		return parts.get(name);
	}
}
