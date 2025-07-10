package io.quarkus.ts.vertx;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

import io.vertx.core.Vertx;

@Liveness
public class MdcHealthCheck implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger("mdc-health-logger");

    @Override
    public HealthCheckResponse call() {
        MDC.put("health-check", getClass().getSimpleName());
        MDC.put("context", Vertx.currentContext() != null ? String.valueOf(Vertx.currentContext().hashCode()) : "null");
        LOGGER.info("Sync Health check 1 - " + Thread.currentThread().getName());
        return HealthCheckResponse.up("mdc-health-check");
    }
}
