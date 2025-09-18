package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.QuarkusApplication;

public abstract class AbstractStepUpAuthenticationIT {

    @LookupService
    static KeycloakService keycloak;

    @QuarkusApplication(properties = "stepup.properties")
    static RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl())
            .withProperties(() -> keycloak.getTlsProperties());

    @Test
    public void testSingleAcrSuccess() {
        String tokenWithAcr = TokenUtils.getAccessTokenWithAcr(keycloak, "acr-level-1");
        app.given()
                .auth().oauth2(tokenWithAcr)
                .when()
                .get("/step-up/single-acr")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(CoreMatchers.is("Single ACR validated"));
    }

}
