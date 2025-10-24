package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup.StepUpAuthOidcTenantResolver.WEB_APP_GOLD;
import static org.apache.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebClientOptions;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup.CustomAcrValidator;
import io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup.StepUpAuthOidcTenantResolver;
import io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup.StepUpResource;
import io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup.StepUpWebSocketEndpoint;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

public abstract class AbstractOidcStepUpAuthenticationIT {

    private static final String INSUFFICIENT_AUTH_ERROR = "insufficient_user_authentication";
    private static final String ACR_VALUES = "acr_values";
    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "test-user";

    @LookupService
    static KeycloakService keycloak;

    @QuarkusApplication(properties = "step-up-auth.properties", classes = {
            StepUpResource.class, StepUpAuthOidcTenantResolver.class,
            StepUpWebSocketEndpoint.class, CustomAcrValidator.class
    })
    static RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl())
            .withProperties(() -> keycloak.getTlsProperties());

    @TestHTTPResource("/ws/step-up/silver")
    java.net.URI websocketUri;

    @Inject
    Vertx vertx;

    @Test
    public void testNoAcrRequired() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/no-acr")
                .then()
                .statusCode(SC_OK)
                .body(is("No ACR, but authentication required"));
    }

    @Test
    public void testSingleAcrWithValidToken() {
        app.given()
                .auth().oauth2(getToken())
                .when()
                .get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_OK)
                .body(is("Single ACR silver validated"));
    }

    @Test
    public void testSingleAcrWrongLevel() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/single-acr-copper")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("copper"));
    }

    @Test
    public void testRbacWithValidAcrAndRole() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/rbac-user-role")
                .then()
                .statusCode(SC_OK)
                .body(is("ACR and user role validated"));
    }

    /**
     * This test verifies Keycloak's browser login flow with step-up mechanism documented
     * in <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#_step-up-flow">the official documentation</a>.
     * At this moment, Quarkus officially supports step-up authentication for the bearer token authentication and Keycloak
     * only documents it for the authorization code flow, so we could either retrieve the access token once user
     * authenticates with the code flow, or we can verify that authorization is enforced even for the code flow as in
     * this test case, without checking headers.
     */
    @Test
    public void testBrowserStepUpAuthenticationFlow() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            // no ACR
            String content = makeHttpPostFormLogin(webClient, "/step-up/no-acr-web-app").getContentAsString();
            assertThat(content, containsString("No ACR, but authentication required"));

            // "silver" ACR which is default for the 'test-app' OIDC client
            content = webClient.getPage(app.getURI(Protocol.HTTP).withPath("/step-up/single-acr-silver-web-app").toString())
                    .getWebResponse().getContentAsString();
            assertThat(content, containsString("Single ACR silver validated"));

            // "gold" ACR needs step-up authentication, we expect 401
            Assertions.assertThrows(FailingHttpStatusCodeException.class,
                    () -> webClient.getPage(app.getURI(Protocol.HTTP).withPath("/step-up/single-acr-gold-web-app").toString())
                            .getWebResponse().getContentAsString());

            // Clear the session cookie so that we can re-authenticate with the new ACR value
            webClient.getCookieManager().clearCookies();

            // there are 3 ways (or more) how to step-up authentication level, which we need to access this path
            // documentation is here https://www.keycloak.org/docs/latest/server_admin/index.html#_step-up-flow
            // 1. we could use 'acr_values' claim with the 'gold'
            // 2. we could use 'claim' code grant parameter and specify that we want 'gold'
            // 3. we could rely on the default client ACR value

            // re-authenticate and request ACR "gold" by using OIDC client 'test-app-gold' which has default ACR 'gold'
            // the 'gold' ACR is the second authentication level, therefore it requires 2-factor authentication
            // this specific browser workflow is configured to use password as the second level as well
            content = makeHttpPostForm2faLogin(webClient, "/step-up/single-acr-gold-web-app").getContentAsString();
            assertThat(content, containsString("Single ACR gold validated"));
        }
    }

    @Test
    public void testChallengeWithMultipleRequiredAcr() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/multiple-acr-copper-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString(INSUFFICIENT_AUTH_ERROR),
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

    /**
     * Tests maxAge validation with a long maxAge value (1 hour).
     * The token should be valid since it was just obtained and is within the maxAge limit.
     * According to the specification, if 'auth_time' claim is not present, the 'iat' claim is used as fallback.
     */
    @Test
    public void testMaxAgeLongWithValidToken() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/max-age-long")
                .then()
                .statusCode(SC_OK)
                .body(is("Max age long with ACR silver validated"));
    }

    /**
     * Tests maxAge validation with a reasonable maxAge value (2 minutes).
     * The token should be valid since it was just obtained.
     */
    @Test
    public void testMaxAgeMediumWithValidToken() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/max-age-with-acr")
                .then()
                .statusCode(SC_OK)
                .body(is("Max age and ACR silver validated"));
    }

    /**
     * Tests maxAge validation with a very short maxAge value (1 second).
     * Note: Due to the nature of Keycloak token generation and network latency,
     * this test may occasionally pass if the token validation happens within 1 second.
     * In a real scenario with WireMock, we could control the auth_time precisely.
     */
    @Test
    public void testMaxAgeShortMayFail() {
        String token = getToken();
        // Wait slightly more than 1 second to ensure maxAge is exceeded
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        app.given().auth().oauth2(token)
                .when().get("/step-up/max-age-short")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("insufficient_user_authentication"),
                        containsString("max_age")));
    }

    /**
     * Tests malformed ACR scenario where token doesn't contain the required ACR value.
     * This simulates a case where the token has different ACR values than expected.
     */
    @Test
    public void testMalformedAcrMissingRequiredValue() {
        // Keycloak assigns 'silver' ACR by default for test-app client
        // Requesting 'copper' should fail as the token won't have it
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/single-acr-copper")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("insufficient_user_authentication"),
                        containsString(ACR_VALUES),
                        containsString("copper")));
    }

    /**
     * Tests multiple ACR values requirement where not all required values are present.
     * The token has 'silver' but needs both 'copper' and 'silver'.
     */
    @Test
    public void testPartialAcrMatch() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/multiple-acr-copper-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("insufficient_user_authentication"),
                        containsString(ACR_VALUES),
                        containsString("copper"),
                        containsString("silver")));
    }

    /**
     * Tests that ACR validation fails when no authentication is provided.
     * This verifies proper challenge response for anonymous requests.
     */
    @Test
    public void testAcrRequiredWithNoAuth() {
        app.given()
                .when().get("/step-up/single-acr-silver")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("Bearer"));
    }

    /**
     * Tests the step-up authentication challenge response format.
     * Verifies that the WWW-Authenticate header contains all required elements:
     * - error code: "insufficient_user_authentication"
     * - acr_values parameter with the required ACR values
     */
    @Test
    public void testStepUpChallengeFormat() {
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/single-acr-copper")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("Bearer"),
                        containsString("error=\"insufficient_user_authentication\""),
                        containsString("error_description="),
                        containsString(ACR_VALUES),
                        containsString("copper")));
    }

    /**
     * Tests WebSocket endpoint with @AuthenticationContext annotation.
     * The annotation must be on the class level for WebSocket endpoints.
     * Verifies that the endpoint requires ACR 'silver' to establish connection.
     */
    @Test
    public void testWebSocketWithStepUpAuthentication() throws Exception {
        String token = getToken(); // This token has 'silver' ACR by default from Keycloak

        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(websocketUri.getHost())
                .setPort(websocketUri.getPort())
                .setURI(websocketUri.getPath())
                .addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token);

        List<String> messages = new ArrayList<>();
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(2); // onOpen + echo response
        AtomicReference<io.vertx.core.http.WebSocket> ws1 = new AtomicReference<>();

        try {
            client.connect(options).onComplete(result -> {
                if (result.succeeded()) {
                    io.vertx.core.http.WebSocket ws = result.result();
                    ws.textMessageHandler(msg -> {
                        messages.add(msg);
                        messagesLatch.countDown();
                    });
                    ws1.set(ws);
                    connectedLatch.countDown();
                } else {
                    Assertions.fail("WebSocket connection failed: " + result.cause().getMessage());
                }
            });

            Assertions.assertTrue(connectedLatch.await(10, TimeUnit.SECONDS),
                    "WebSocket should connect with valid ACR");
            ws1.get().writeTextMessage("test message");
            Assertions.assertTrue(messagesLatch.await(10, TimeUnit.SECONDS),
                    "Should receive onOpen and echo messages");

            Assertions.assertEquals(2, messages.size());
            assertThat(messages.get(0), containsString("WebSocket opened with ACR silver"));
            assertThat(messages.get(1), containsString("Echo with silver ACR: test message"));

            ws1.get().close();
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Tests WebSocket endpoint rejection when token doesn't have required ACR.
     * Note: This test is limited with real Keycloak as we can't easily get a token without ACR.
     */
    @Test
    public void testWebSocketWithoutRequiredAcr() {
        // With real Keycloak, we cannot easily obtain a token without ACR claims
        // This test would be better implemented with WireMock
        // For now, we test anonymous access (no token)
        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(websocketUri.getHost())
                .setPort(websocketUri.getPort())
                .setURI(websocketUri.getPath());
        // No Authorization header

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        try {
            client.connect(options).onComplete(result -> {
                if (result.failed()) {
                    errorRef.set(result.cause());
                    errorLatch.countDown();
                } else {
                    Assertions.fail("WebSocket should not connect without token");
                }
            });

            Assertions.assertTrue(errorLatch.await(10, TimeUnit.SECONDS),
                    "Should receive connection error");
            Assertions.assertNotNull(errorRef.get(), "Should have connection error");
        } catch (Exception e) {
            Assertions.fail("Test failed with exception: " + e.getMessage());
        } finally {
            try {
                client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Tests custom Jose4J validator that requires specific ACR values.
     * This validator requires both "gold" and "platinum" ACR values.
     * Since Keycloak by default provides "silver", this should fail and trigger step-up.
     */
    @Test
    public void testCustomJose4jValidatorRequiresMultipleAcr() {
        // Default Keycloak token has 'silver' ACR, but custom validator requires 'gold' and 'platinum'
        app.given().auth().oauth2(getToken())
                .when().get("/step-up/custom-validator")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, allOf(
                        containsString("Bearer"),
                        containsString("error=\"insufficient_user_authentication\""),
                        containsString(ACR_VALUES),
                        containsString("gold"),
                        containsString("platinum")));
    }

    /**
     * Tests anonymous access to endpoint with custom validator.
     * Should return 401 Unauthorized.
     */
    @Test
    public void testCustomValidatorWithNoAuth() {
        app.given()
                .when().get("/step-up/custom-validator")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, containsString("Bearer"));
    }

    private static String getToken() {
        var authzClient = keycloak.createAuthzClient("test-app", "password");
        var accessToken = authzClient.obtainAccessToken(TEST_USER_NAME, TEST_USER_PASSWORD);
        return accessToken.getToken();
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        WebClientOptions options = webClient.getOptions();
        options.setUseInsecureSSL(true);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        Logger.getLogger("org.htmlunit.css").setLevel(Level.OFF);
        options.setRedirectEnabled(true);
        return webClient;
    }

    private static WebResponse makeHttpPostFormLogin(WebClient webClient, String loginPath) {
        try {
            HtmlPage page = webClient.getPage(app.getURI(Protocol.HTTP).withPath(loginPath).toString());
            HtmlForm form = page.getHtmlElementById("kc-form-login");
            form.getInputByName("username").type(TEST_USER_NAME);
            form.getInputByName("password").type(TEST_USER_PASSWORD);

            return form.getButtonByName("login").click().getWebResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs in with OIDC client that has default ACR set to 'gold' and requires 2-factor authentication to make
     * authentication requirements "stronger" considering we are stepping-up the level.
     */
    private static WebResponse makeHttpPostForm2faLogin(WebClient webClient, String loginPath) {
        try {
            // first factor
            HtmlPage page = webClient.getPage(app.getURI(Protocol.HTTP).withPath(loginPath).toString() + "?" + WEB_APP_GOLD);
            HtmlForm form = page.getHtmlElementById("kc-form-login");
            form.getInputByName("username").type(TEST_USER_NAME);
            form.getInputByName("password").type(TEST_USER_PASSWORD);

            page = form.getButtonByName("login").click();

            // second factor; forced by the 'loa-max-age: 0' option
            form = page.getHtmlElementById("kc-form-login");
            form.getInputByName("password").type(TEST_USER_PASSWORD);

            return form.getButtonByName("login").click().getWebResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
