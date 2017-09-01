package ru.obolensk.afff.beetle.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.obolensk.afff.beetle.core.Storage;
import ru.obolensk.afff.beetle.request.Request;
import ru.obolensk.afff.beetle.settings.Config;

import static java.nio.file.Files.isRegularFile;
import static ru.obolensk.afff.beetle.settings.Options.SERVLETS_ENABLED;

/**
 * Created by Afff on 25.07.2017.
 */
public class ServletContainer {

    private static final Logger logger = LoggerFactory.getLogger(ServletContainer.class);

    private final ClassFilter securityFilter = new BlockAnyClassFilter();

    private final Storage storage;

    private volatile Map<Path, Servlet> servlets;

    public ServletContainer(@Nonnull final Storage storage) throws IOException {
        this.storage = storage;
        update();
    }

    public void update() throws IOException {
        servlets = loadServlets();
    }

    private Map<Path, Servlet> loadServlets() throws IOException {
        final Config config = storage.getConfig();
        final Map<Path, Servlet> servlets = new HashMap<>();
        if (!config.is(SERVLETS_ENABLED)) {
            return servlets;
        }
        logger.debug("Starting servlets refreshing...");
        final Path wwwDir = storage.getWwwDir();
        final Path servletPath = storage.getServletDir();
        if (Files.exists(servletPath)) {
            Files.walk(servletPath)
                    .filter((path) -> isRegularFile(path) && path.toString().toLowerCase().endsWith(".js"))
                    .forEach((path) -> {
                        final String servletScript;
                        try {
                            servletScript = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            logger.error("Unable to read servlet ['{}']: " + e.getMessage(), path.getFileName());
                            return;
                        }
                        final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
                        final ScriptEngine engine = factory.getScriptEngine(securityFilter);
                        try {
                            engine.eval(servletScript);
                            final Servlet servlet = new Servlet(path.getFileName().toString(), engine);
                            servlet.getContentRootList().forEach(contentRoot -> {
                                servlets.put(wwwDir.resolve(contentRoot), servlet);
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Servlet '{}' registered for path '{}'.", servlet.getName(), contentRoot);
                                }
                            });
                        } catch (ScriptException | NoSuchMethodException e) {
                            logger.error("Servlet ['{}'] has syntax errors: " + e.getMessage(), path.getFileName());
                        }
                    });
        }
        logger.debug("Servlets refreshing done. Loaded {} servlets.", servlets.size());
        return servlets;
    }

    @Nullable
    public ServletResponse process(@Nonnull final Request req, @Nonnull final Path path) {
        final Servlet servlet = servlets.get(path);
        if (servlet != null) {
            final ServletContext context = new ServletContext(req);
            return servlet.run(context);
        }
        return null;
    }

    private class BlockAnyClassFilter implements ClassFilter {
        @Override
        public boolean exposeToScripts(String s) {
            return false;
        }
    }
}
