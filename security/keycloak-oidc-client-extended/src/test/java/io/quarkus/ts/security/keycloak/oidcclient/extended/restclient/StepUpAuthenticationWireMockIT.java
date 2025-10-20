package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.apache.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@DisplayName("OIDC Step-Up Authentication Tests with WireMock")
public class StepUpAuthenticationWireMockIT {

    private static final String INSUFFICIENT_AUTH_ERROR = "insufficient_user_authentication";
    private static final String ACR_VALUES_PARAM = "acr_values";
    private static final String MAX_AGE_PARAM = "max_age";

    private static WireMockServer wireMockServer;
    private static MockTokenGenerator tokenUtils;

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> getWiremockUrl() + "/auth/realms/test-realm")
            .withProperty("quarkus.oidc.client-id", "test-client")
            .withProperty("quarkus.oidc.discovery-enabled", "true")
            .withProperty("quarkus.http.auth.permission.authenticated.paths", "");

    private static String getWiremockUrl() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(options().dynamicPort());
            wireMockServer.start();
            tokenUtils = new MockTokenGenerator(wireMockServer.port());
            setupWireMockStubs();
        }
        return wireMockServer.baseUrl();
    }

    private static void setupWireMockStubs() {
        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/test-realm/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withBody(tokenUtils.generateDiscoveryDocument())));

        wireMockServer.stubFor(
                get(urlEqualTo("/auth/realms/test-realm/protocol/openid-connect/certs"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withBody(tokenUtils.generateJwks())));
    }

    @AfterAll
    public static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }


    @Test
    public void testStepUpChallengeWithValidBareboneToken() {
        String token = tokenUtils.createBareboneToken();
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .log().ifValidationFails()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("Bearer"),
                        containsString("error=\"insufficient_user_authentication\""),
                        containsString("acr_values=\"silver\"")));
    }

    @Test
    @DisplayName("Should allow access when token contains required single ACR value")
    public void testSingleAcrSuccess() {
        String token = tokenUtils.createTokenWithSingleAcr("silver");
        app.given()
                .auth().oauth2(token)
                .when().get("/step-up/single-acr-silver")
                .then().statusCode(SC_OK);
    }

    @Test
    @DisplayName("Should deny access when token contains wrong ACR value")
    public void testWrongAcrFailure() {
        String token = tokenUtils.createTokenWithSingleAcr("copper");
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("Bearer"),
                        containsString(INSUFFICIENT_AUTH_ERROR),
                        containsString(ACR_VALUES_PARAM),
                        containsString("silver")));
    }

    @Test
    @DisplayName("Should deny access when token has no ACR claim")
    public void testMissingAcrFailure() {
        String token = tokenUtils.createTokenWithoutAcr();
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString(INSUFFICIENT_AUTH_ERROR));
    }

    @Test
    @DisplayName("Should allow access when token contains all required multiple ACR values")
    public void testMultipleAcrSuccess() {
        String token = tokenUtils.createTokenWithMultipleAcrs("copper", "silver");

        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/multiple-acr-copper-silver")
                .then()
                .statusCode(SC_OK)
                .body(is("Multiple ACR validated"));
    }

    @Test
    @DisplayName("Should deny access when token missing one of required ACR values")
    public void testPartialAcrFailure() {
        String token = tokenUtils.createTokenWithSingleAcr("copper");
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/multiple-acr-copper-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString(INSUFFICIENT_AUTH_ERROR),
                        containsString("copper"),
                        containsString("silver")));
    }

    @Test
    @DisplayName("Should allow access when token age is within maxAge limit")
    public void testMaxAgeSuccess() {
        // Token authenticated 60 seconds ago, maxAge is 120 seconds
        String token = tokenUtils.createTokenWithAcrAndMaxAge("silver", 60);

        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/acr-with-max-age")
                .then()
                .statusCode(SC_OK)
                .body(is("ACR with max age validated"));
    }

    @Test
    @DisplayName("Should deny access when token age exceeds maxAge limit")
    public void testMaxAgeExpired() {
        // Token authenticated 150 seconds ago, maxAge is 120 seconds
        String token = tokenUtils.createTokenWithAcrAndMaxAge("silver", 150);
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/acr-with-max-age")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString(INSUFFICIENT_AUTH_ERROR),
                        containsString(MAX_AGE_PARAM),
                        containsString("120")));
    }

    @Test
    @DisplayName("Should use iat claim when auth_time is missing")
    public void testMaxAgeWithIatFallback() {
        // Create token without auth_time but within acceptable iat
        String token = tokenUtils.createMockedToken(
                Set.of("silver"), null, Set.of("user"));
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/acr-with-max-age")
                .then()
                .statusCode(SC_OK);
    }

    @Test
    @DisplayName("Should allow access with correct ACR and role")
    public void testAcrWithRoleSuccess() {
        String token = tokenUtils.createMockedToken(
                Set.of("silver"), null, Set.of("user"));
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/rbac-user-role")
                .then()
                .statusCode(SC_OK)
                .body(is("ACR and user role validated"));
    }

    @Test
    @DisplayName("Should deny access with correct ACR but wrong role")
    public void testAcrSuccessRoleFailure() {
        String token = tokenUtils.createMockedToken(
                Set.of("silver"), null, Set.of("guest"));
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/rbac-user-role")
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Should deny access with wrong ACR even with correct role")
    public void testAcrFailureRoleSuccess() {
        String token = tokenUtils.createMockedToken(
                Set.of("copper"), null, Set.of("user"));
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/rbac-user-role")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("silver"));
    }

    @Test
    @DisplayName("Should return properly formatted challenge for missing ACR")
    public void testChallengeFormat() {
        String token = tokenUtils.createTokenWithoutAcr();
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("Bearer error=\"insufficient_user_authentication\""),
                        containsString("error_description=\"A different authentication level is required\""),
                        containsString("acr_values=\"silver\"")));
    }

    @Test
    @DisplayName("Should include both ACR and maxAge in challenge when both required")
    public void testCombinedChallengeFormat() {
        String token = tokenUtils.createTokenWithAcrAndMaxAge("copper", 150);
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/acr-with-max-age")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString(INSUFFICIENT_AUTH_ERROR),
                        containsString(ACR_VALUES_PARAM),
                        containsString(MAX_AGE_PARAM)));
    }

    @Test
    @DisplayName("Should require authentication for ACR-protected endpoints")
    public void testAnonymousAccessDenied() {
        app.given()
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("Bearer"));
    }
}
