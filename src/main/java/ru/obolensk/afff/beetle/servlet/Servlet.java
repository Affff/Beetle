package ru.obolensk.afff.beetle.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.obolensk.afff.beetle.protocol.HttpCode;

/**
 * Created by Afff on 25.07.2017.
 */
public class Servlet {

    private final static String CONTENT_ROOT_METHOD_NAME = "getContentRoots";
    private final static String RUN_SERVLET_METHOD_NAME = "run";

    private final static String SERVLET_RESPONSE_CODE_FIELD = "code";
    private final static String SERVLET_RESPONSE_DATA_FIELD = "data";

    private static final Logger logger = LoggerFactory.getLogger(ServletContainer.class);

    @Getter
    private final String name;

    @Getter
    private final List<String> contentRootList = new ArrayList<>();

    private final Invocable invocable;

    @SuppressWarnings("unchecked")
    public Servlet(@Nonnull final String name, @Nonnull final ScriptEngine engine) throws ScriptException, NoSuchMethodException {
        this.name = name;
        this.invocable = (Invocable) engine;
        final Object result = invocable.invokeFunction(CONTENT_ROOT_METHOD_NAME);
        if (result instanceof String) {
            contentRootList.add((String) result);
        } else if (result instanceof Map) {
            contentRootList.addAll(((Map<?, String>) result).values());
        }
    }

    @SuppressWarnings("unchecked")
    public ServletResponse run(ServletContext context) {
        try {
            final ScriptObjectMirror result = (ScriptObjectMirror) invocable.invokeFunction(RUN_SERVLET_METHOD_NAME, context);
            HttpCode resultCode = HttpCode.getByStatusCode((Number) result.get(SERVLET_RESPONSE_CODE_FIELD));
            String data = (String) result.get(SERVLET_RESPONSE_DATA_FIELD);
            return new ServletResponse(resultCode, data);
        } catch (ScriptException | NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
            return new ServletResponse(HttpCode.HTTP_500, e.getMessage());
        }
    }
}
