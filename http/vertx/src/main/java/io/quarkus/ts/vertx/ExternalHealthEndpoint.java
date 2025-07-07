package io.quarkus.ts.vertx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.MDC;

import io.smallrye.health.SmallRyeHealthReporter;

@Path("/external-health")
@ApplicationScoped
public class ExternalHealthEndpoint {

    public static final String MDC_KEY = "endpoint.context";
    public static final String MDC_VALUE = "value-from-endpoint";

    @Inject
    SmallRyeHealthReporter healthReporter;

    @GET
    public Response triggerHealthChecks() {
        MDC.put(MDC_KEY, MDC_VALUE);

        System.out.println("DEBUG: MDC set in endpoint: " + MDC.get(MDC_KEY));

        try {
            return Response.ok(healthReporter.getHealth().getPayload()).build();
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}