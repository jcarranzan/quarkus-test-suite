package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.security.Authenticated;

@Path("/step-up")
public class StepUpAuthenticationResource {

    @GET
    @Path("/single-acr")
    @AuthenticationContext("acr-level-1")
    public String singleAcr() {
        return "Single ACR validated";
    }

    @GET
    @Path("/authenticated-only")
    @Authenticated
    public String authenticatedEndpoint() {
        return "authenticated-success";
    }

    @GET
    @Path("/public")
    public String publicEndpoint() {
        return "public-success";
    }

}
