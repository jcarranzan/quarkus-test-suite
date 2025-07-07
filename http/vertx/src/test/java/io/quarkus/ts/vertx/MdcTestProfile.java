package io.quarkus.ts.vertx;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MdcTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.console.format", "endpoint_context=%X{endpoint.context} %s%e%n",
                "quarkus.smallrye-qhealth.context-propagation.enabled", "true");
    }
}