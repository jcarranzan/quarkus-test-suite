package io.quarkus.ts.micrometer.oidc;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/user")
@Authenticated
public class UserResource {
    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "Hello, user " + identity.getPrincipal().getName();
    }

    @GET
    @Path("/testredirection")
    public Response redirect() {
        String targetUrl = "https://www.google.com";
        return Response.status(Response.Status.FOUND)
                .location(UriBuilder.fromUri(targetUrl).build())
                .build();
    }

}
