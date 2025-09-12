package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthenticationContext;

@Path("/step-up-class")
@AuthenticationContext("acr-class-level")
public class StepUpClassLevelResource {

    @GET
    @Path("/test")
    public String test() {
        return "Class level ACR validated";
    }
}
