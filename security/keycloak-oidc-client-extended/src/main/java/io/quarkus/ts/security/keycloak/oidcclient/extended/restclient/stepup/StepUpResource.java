package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.security.Authenticated;

@Path("/step-up")
@BearerTokenAuthentication
public class StepUpResource {

    @GET
    @Path("/no-acr")
    @Authenticated
    public String noAcr() {
        return "No ACR, but authentication required";
    }

    @GET
    @Path("/single-acr-copper")
    @AuthenticationContext("copper")
    public String singleAcrCopper() {
        return "Single ACR copper validated";
    }

    @GET
    @Path("/single-acr-silver")
    @AuthenticationContext("silver")
    public String singleAcrSilver() {
        return "Single ACR silver validated";
    }

    @GET
    @Path("/single-acr-gold")
    @AuthenticationContext("gold")
    public String singleAcrGold() {
        return "Single ACR gold validated";
    }

    @GET
    @Path("/multiple-acr-copper-silver")
    @AuthenticationContext({ "copper", "silver" })
    public String multipleAcrCopperSilver() {
        return "Multiple ACR copper and silver validated";
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
