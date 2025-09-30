package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;
import static org.apache.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class StepUpAuthenticationIT {

    private static final String INSUFFICIENT_AUTH = "insufficient_user_authentication";
    private static final String ACR_VALUES = "acr_values";

    @KeycloakContainer(runKeycloakInProdMode = true)
    static KeycloakService keycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM, DEFAULT_REALM_BASE_PATH);

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl())
            .withProperty("quarkus.keycloak.admin-client.server-url",
                    () -> {
                        String realmUrl = keycloak.getRealmUrl();
                        return realmUrl.substring(0, realmUrl.indexOf("/realms"));
                    })
            .withProperty("quarkus.keycloak.admin-client.realm", "master")
            .withProperties(() -> keycloak.getTlsProperties())
            .withProperty("quarkus.keycloak.admin-client.tls-configuration-name", "keycloak");

    @BeforeAll
    public static void setupRealm() {
        // Configure Keycloak with user with ACR "silver"
        app.given()
                .when()
                .get("/admin-client/configure-stepup")
                .then()
                .statusCode(SC_OK)
                .body(is("Realm configured for Step-Up Authentication"));
    }

    @Test
    public void testNoAcrRequired() {
        String token = TokenUtils.createToken(keycloak, "test-user-copper", "test-user-copper");

        app.given().auth().oauth2(token)
                .when().get("/step-up/no-acr")
                .then()
                .statusCode(SC_OK)
                .body(is("No ACR, but authentication required"));
    }

    @Test
    public void testSingleAcrWithValidToken() {
        String token = TokenUtils.createToken(keycloak, "test-user-silver", "test-user-silver");

        app.given().auth().oauth2(token).get("/step-up/no-acr").then().statusCode(SC_OK)
                .body(is("No ACR, but authentication required"));

        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_OK)
                .body(is("Single ACR silver validated"));
    }

    @Test
    public void testSingleAcrWrongLevel() {
        String tokenGold = TokenUtils.createToken(keycloak, "test-user-gold", "test-user-gold");

        app.given().auth().oauth2(tokenGold)
                .when().get("/step-up/single-acr-copper")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("copper"));
    }

    @Test
    public void testNoAcrInToken() {
        String token = TokenUtils.createToken(keycloak, "test-user-copper", "test-user-copper");

        app.given().auth().oauth2(token)
                .when().get("/step-up/single-acr-copper")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString(INSUFFICIENT_AUTH));
    }

    @Test
    public void testRbacWithValidAcrAndRole() {
        String token = TokenUtils.createToken(keycloak, "test-user-silver", "test-user-silver");

        app.given().auth().oauth2(token)
                .when().get("/step-up/rbac-user-role")
                .then()
                .statusCode(SC_OK)
                .body(is("ACR and user role validated"));
    }

    @Test
    public void testChallengeResponseFormat() {
        String token = TokenUtils.createToken(keycloak, "test-user-copper", "test-user-copper");

        app.given().auth().oauth2(token)
                .when().get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("Bearer"),
                        containsString(INSUFFICIENT_AUTH),
                        containsString("error_description"),
                        containsString(ACR_VALUES),
                        containsString("silver")));
    }

    @Test
    public void testChallengeWithMultipleRequiredAcr() {
        String token = TokenUtils.createToken(keycloak, "test-user-copper", "test-user-copper");

        app.given().auth().oauth2(token)
                .when().get("/step-up/multiple-acr-copper-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString(INSUFFICIENT_AUTH),
                        containsString(ACR_VALUES)));
    }

    @Test
    public void testAnonymousAccessToAcrProtectedEndpoint() {
        app.given()
                .when().get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("Bearer"));
    }

}
