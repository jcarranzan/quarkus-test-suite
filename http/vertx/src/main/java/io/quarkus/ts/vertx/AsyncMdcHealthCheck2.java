package io.quarkus.ts.vertx;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

@Liveness
public class AsyncMdcHealthCheck2 implements AsyncHealthCheck {

    private static final Logger LOGGER = Logger.getLogger("mdc-health-logger");

    @Override
    public Uni<HealthCheckResponse> call() {
        MDC.put("health-check", getClass().getSimpleName());
        MDC.put("context", Vertx.currentContext() != null ? String.valueOf(Vertx.currentContext().hashCode()) : "null");
        LOGGER.info("Async Health check 2 - " + Thread.currentThread().getName());
        return Uni.createFrom().item(HealthCheckResponse.up("async-mdc-health-check-2"));
    }
}
