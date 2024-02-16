package io.quarkus.ts.http.http2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/morning")
public class MorningResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String morning() {
        return "Buenos dias";
    }

}
