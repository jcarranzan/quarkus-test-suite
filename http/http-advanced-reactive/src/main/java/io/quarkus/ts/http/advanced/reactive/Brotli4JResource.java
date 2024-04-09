package io.quarkus.ts.http.advanced.reactive;

import java.util.HashMap;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.vertx.http.Compressed;

@Path("/compression")
public class Brotli4JResource {

    @Inject
    Brotli4JRestMock brotli4JRestMock;

    private final static String DEFAULT_TEXT_PLAIN = "In life, you have to trust that every little bit helps. As you know, every small step forward counts."
            +
            " It's the accumulation of these efforts that ultimately leads to success." +
            " So, don't underestimate the power of persistence and determination in achieving your dreams";

    @GET
    @Path("/text")
    @Produces(MediaType.TEXT_PLAIN)
    @Compressed
    public String textPlainHttpResponse() {
        return DEFAULT_TEXT_PLAIN;
    }

    @POST
    @Path("/decompression")
    @Produces(MediaType.TEXT_PLAIN)
    public String decompressionHttpResponse(byte[] compressedData) {
        // No need to implement decompression logic; Quarkus should handles it automatically
        return new String(compressedData);
    }

    @GET
    @Path("/brotli/json")
    @Produces(MediaType.APPLICATION_JSON)
    public HashMap<String, Object> jsonHttpResponse() {
        return brotli4JRestMock.returnResponse();
    }

}
