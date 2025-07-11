package io.quarkus.ts.vertx;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MdcTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.console.format", "%d{HH:mm:ss} %-5p endpoint_context=%X{endpoint.context} [%c{2.}] (%t) %s%e%n",
                "quarkus.smallrye-health.context-propagation.enabled", "true",
                "quarkus.log.min-level", "DEBUG",
                "quarkus.log.category.\"test-logger\".level", "ERROR");
    }
}