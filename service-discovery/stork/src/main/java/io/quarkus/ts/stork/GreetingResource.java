package io.quarkus.ts.stork;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

@Path("/greeting")
public class GreetingResource implements IGreetingResource {

    private static final Logger LOG = Logger.getLogger(GreetingResource.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("GreetingResource.hello() called");
        return "Hello from Stork!";
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessageCount() {
        LOG.info("GreetingResource.getMessageCount() called");
        return String.valueOf(PriceConsumer.getCount());
    }
}