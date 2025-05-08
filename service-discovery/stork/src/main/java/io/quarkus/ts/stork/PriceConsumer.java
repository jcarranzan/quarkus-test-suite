package io.quarkus.ts.stork;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/price")
public class PriceConsumer {
    private static final Logger LOG = Logger.getLogger(PriceConsumer.class);
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @Inject
    @RestClient
    IGreetingResource greetingResource;

    @Incoming("prices")
    public void consume(double price) throws InterruptedException {
        LOG.info("Received price: " + price);
        Thread.sleep(1000);

        try {
            String result = greetingResource.hello();
            LOG.info("Greeting result => " + result);
            processedCount.incrementAndGet();
        } catch (Exception e) {
            LOG.error("Error calling greeting service", e);
        }
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int getProcessedCount() {
        return processedCount.get();
    }
}
