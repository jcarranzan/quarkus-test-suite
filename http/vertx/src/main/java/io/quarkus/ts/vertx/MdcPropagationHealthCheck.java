package io.quarkus.ts.vertx;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Liveness
@ApplicationScoped
public class MdcPropagationHealthCheck implements HealthCheck {
    private static final Logger LOG = Logger.getLogger("io.quarkus.ts.vertx.MdcPropagationHealthCheck");

    @Override
    public HealthCheckResponse call() {
        Object mdcValue = MDC.get(ExternalHealthEndpoint.MDC_KEY);
        System.out.println("DEBUG: MDC in health check: " + mdcValue);

        LOG.infof("Executing MdcPropagationHealthCheck with MDC: %s", mdcValue);

        return HealthCheckResponse.up("mdc-propagation-check");
    }
}