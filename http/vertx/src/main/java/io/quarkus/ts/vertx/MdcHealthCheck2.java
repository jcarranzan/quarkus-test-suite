package io.quarkus.ts.vertx;

import java.util.logging.Logger;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.logmanager.MDC;

import io.vertx.core.Vertx;

@Liveness
public class MdcHealthCheck2 implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger("mdc-health-logger");

    @Override
    public HealthCheckResponse call() {
        MDC.put("health-check", getClass().getSimpleName());
        MDC.put("context", Vertx.currentContext() != null ? String.valueOf(Vertx.currentContext().hashCode()) : "null");
        LOGGER.info("Sync Health check 2 - " + Thread.currentThread().getName());
        return HealthCheckResponse.up("mdc-health-check-2");
    }
}
