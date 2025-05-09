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

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Blocking;

@ApplicationScoped
@Path("/price")
public class PriceConsumer {

    private static final Logger LOG = Logger.getLogger(PriceConsumer.class);
    private static final AtomicInteger messageCount = new AtomicInteger(0);

    @Inject
    @RestClient
    IGreetingResource greetingResource;

    @Incoming("prices")
    @RunOnVirtualThread
    @Blocking(ordered = false)
    public void consume(double price) throws InterruptedException {
        LOG.info("consume() - Received price: " + price);

        if (greetingResource == null) {
            LOG.error("consume() - greetingResource is null!");
        } else {
            LOG.info("consume() - greetingResource is NOT null");
        }

        try {
            Thread.sleep(1000);
            LOG.info("consume() - Calling greetingResource.hello()");
            String greeting = greetingResource.hello();
            LOG.info("consume() - greeting => " + greeting);
            messageCount.incrementAndGet();
        } catch (jakarta.ws.rs.WebApplicationException e) {
            LOG.error("consume() - WebApplicationException: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("consume() - Unexpected exception: " + e.getMessage(), e);
            throw e;
        } finally {
            LOG.info("consume() - Finished processing price: " + price);
        }
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessageCount() {
        return String.valueOf(messageCount.get());
    }

    public static int getCount() {
        return messageCount.get();
    }
}
