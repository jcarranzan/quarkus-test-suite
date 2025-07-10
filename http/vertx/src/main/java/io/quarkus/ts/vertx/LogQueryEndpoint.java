package io.quarkus.ts.vertx;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/logs")
public class LogQueryEndpoint {

    @GET
    @Path("/records")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getRecords() {
        // Calls the static method on the handler
        return InMemoryLogHandler.getRecords();
    }

    @GET
    @Path("/reset")
    public void reset() {
        // Calls the static method on the handler
        InMemoryLogHandler.reset();
    }
}
