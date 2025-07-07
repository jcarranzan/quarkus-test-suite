package io.quarkus.ts.vertx;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;

import org.jboss.logmanager.formatters.PatternFormatter;

@ApplicationScoped
public class InMemoryLogHandlerProducer {
    private static final String LOG_FORMAT = "endpoint_context=%X{endpoint.context} %s%n";

    @Produces
    public InMemoryLogHandler createHandler() {
        InMemoryLogHandler handler = new InMemoryLogHandler();
        Formatter formatter = new PatternFormatter(LOG_FORMAT);
        handler.setFormatter(formatter);
        handler.setLevel(Level.INFO);

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.addHandler(handler);

        return handler;
    }
}