package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import java.time.Duration;
import java.util.Arrays;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.security.Authenticated;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;

@Path("/step-up")
public class StepUpAuthenticationResource {

    RsaJsonWebKey key;

    @PostConstruct
    public void init() throws Exception {
        key = RsaJwkGenerator.generateJwk(2048);
        key.setUse("sig");
        key.setKeyId("1");
        key.setAlgorithm("RS256");
    }

    @POST
    @Path("accesstoken-with-acr")
    @Produces("application/json")
    public String testAccessTokenWithAcr(@QueryParam("acr") String acr, @QueryParam("auth_time") String authTime) {
        return "{\"access_token\": \"" + jwt(null, "123456789", "1", false, acr, authTime)
                + "\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

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

    private String jwt(String audience, String subject, String kid, boolean withEmptyScope, String acr, String authTime) {
        JwtClaimsBuilder builder = Jwt.claim("typ", "Bearer")
                .upn("alice")
                .preferredUserName("alice")
                .groups("user")
                .expiresIn(Duration.ofSeconds(4));
        if (audience != null) {
            builder.audience(audience);
        }
        if (subject != null) {
            builder.subject(subject);
        }

        if (withEmptyScope) {
            builder.claim("scope", "");
        }

        if (acr != null && !acr.isEmpty()) {
            builder.claim("acr", Arrays.asList(acr.split(",")));
        }

        if (authTime != null && !authTime.isEmpty()) {
            builder.claim("auth_time", Long.parseLong(authTime));
        }
        return builder.jws().keyId(kid)
                .sign(key.getPrivateKey());
    }

}
