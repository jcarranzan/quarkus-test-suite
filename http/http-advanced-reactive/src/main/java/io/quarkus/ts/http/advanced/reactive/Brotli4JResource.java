package io.quarkus.ts.http.advanced.reactive;

import java.util.HashMap;

import io.quarkus.vertx.http.Compressed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/compression")
public class Brotli4JResource {

    final static String DEFAULT_TEXT_PLAIN = "As you know, every little bit counts";


    @GET
    @Path("/text")
    @Compressed
    @Produces(MediaType.TEXT_PLAIN)
    public String textPlainHttpResponse() {
        return DEFAULT_TEXT_PLAIN;
    }

}