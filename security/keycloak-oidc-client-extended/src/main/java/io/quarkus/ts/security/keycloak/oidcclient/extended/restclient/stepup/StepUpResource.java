package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.oidc.BearerTokenAuthentication;

@Path("/step-up")
@BearerTokenAuthentication
public class StepUpResource {

    @GET
    @Path("/no-acr")
    @RolesAllowed("user")
    public String noAcrRequired() {
        return "No ACR, but authentication required";
    }

    @GET
    @Path("/ping")
    @RolesAllowed("user")
    public String ping() {
        return "pong";
    }

    @GET
    @Path("/single-acr-silver")
    @AuthenticationContext("silver")
    public String singleAcrSilver() {
        return "Single ACR silver validated";
    }

    @GET
    @Path("/single-acr-copper")
    @AuthenticationContext("copper")
    public String singleAcrCopper() {
        return "Single ACR copper validated";
    }

    @GET
    @Path("/multiple-acr-copper-silver")
    @AuthenticationContext({ "copper", "silver" })
    public String multipleAcr() {
        return "Multiple ACR validated";
    }

    @GET
    @Path("/acr-with-max-age")
    @AuthenticationContext(value = "silver", maxAge = "PT120s")
    public String acrWithMaxAge() {
        return "ACR with max age validated";
    }

    @GET
    @Path("/rbac-user-role")
    @AuthenticationContext("silver")
    @RolesAllowed("user")
    public String rbacUserRole() {
        return "ACR and user role validated";
    }

    @GET
    @Path("/rbac-admin-role")
    @AuthenticationContext("gold")
    @RolesAllowed("admin")
    public String rbacAdminRole() {
        return "ACR and admin role validated";
    }

}
