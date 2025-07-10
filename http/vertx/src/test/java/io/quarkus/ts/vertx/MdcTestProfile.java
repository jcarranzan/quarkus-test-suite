package io.quarkus.ts.vertx;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MdcTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.handler.in-memory.class", "io.quarkus.ts.vertx.InMemoryLogHandler",
                "quarkus.log.handler.in-memory.level", "INFO",
                "quarkus.log.handler.in-memory.formatter.pattern",
                "health-check=%X{health-check} context=%X{context} class=%X{class} [%c{2.}] (%t) %s%n",

                "quarkus.log.category.\"mdc-health-logger\".level", "INFO",
                "quarkus.log.category.\"mdc-health-logger\".handlers", "in-memory",
                "quarkus.log.category.\"mdc-health-logger\".use-parent-handlers", "false",

                "quarkus.smallrye-health.context-propagation", "true");
    }
}
