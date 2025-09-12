package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthenticationContext;

@Path("/step-up")
public class StepUpAuthenticationResource {

    @GET
    @Path("/single-acr")
    @AuthenticationContext("acr-level-1")
    public String singleAcr() {
        return "Single ACR validated";
    }

}
