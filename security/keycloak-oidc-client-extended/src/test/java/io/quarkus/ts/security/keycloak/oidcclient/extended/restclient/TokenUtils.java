package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPasswordInput;
import org.htmlunit.html.HtmlTextInput;
import org.jboss.logging.Logger;

import io.quarkus.test.bootstrap.KeycloakService;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;

final class TokenUtils {

    private static final Logger LOG = Logger.getLogger(TokenUtils.class);
    static final String USER = "test-user";

    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    private TokenUtils() {
    }

    private static String simulateBrowserLogin(String authUrl, KeycloakService keycloak) {
        WebClient webClient = new WebClient();
        try {
            webClient.getOptions().setRedirectEnabled(false);
            webClient.getOptions().setWebSocketEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            webClient.getOptions().setUseInsecureSSL(true);

            LOG.info("Navigating to authorization URL: " + authUrl);
            HtmlPage loginPage = webClient.getPage(authUrl);

            HtmlForm loginForm = findLoginForm(loginPage);
            if (loginForm == null) {
                throw new RuntimeException("Login form not found on page");
            }

            // Fill username
            HtmlTextInput usernameField = loginForm.getFirstByXPath(".//input[@name='username' or @id='username']");
            if (usernameField != null) {
                usernameField.setValueAttribute(USER);
            }

            HtmlPasswordInput passwordField = loginForm.getFirstByXPath(".//input[@name='password' or @id='password']");
            if (passwordField != null) {
                passwordField.setValueAttribute(USER);
            }

            HtmlElement submitButton = loginForm.getFirstByXPath(".//input[@type='submit']");
            if (submitButton == null) {
                submitButton = loginForm.getFirstByXPath(".//button[@type='submit']");
            }

            LOG.info("Submitting login form");
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage resultPage = submitButton.click();

            String code = extractAuthorizationCode(webClient, resultPage);

            if (code == null || code.isEmpty()) {
                throw new RuntimeException("Failed to extract authorization code from redirect");
            }

            LOG.info("Successfully obtained authorization code");
            return code;

        } catch (Exception e) {
            LOG.error("Error during browser login simulation", e);
            throw new RuntimeException("Browser login simulation failed", e);
        } finally {
            webClient.close();
        }
    }

    private static HtmlForm findLoginForm(HtmlPage page) {

        // Strategy 1: Find by form ID
        HtmlForm form = page.getHtmlElementById("kc-form-login");
        if (form != null)
            return form;

        // Strategy 2: Find by action containing "authenticate"
        for (HtmlForm f : page.getForms()) {
            if (f.getActionAttribute().contains("authenticate")) {
                return f;
            }
        }

        // Strategy 3: Find form containing username and password fields
        for (HtmlForm f : page.getForms()) {
            if (f.getFirstByXPath(".//input[@name='username']") != null &&
                    f.getFirstByXPath(".//input[@name='password']") != null) {
                return f;
            }
        }

        return null;
    }

    private static String extractAuthorizationCode(WebClient webClient, HtmlPage resultPage) {
        // Check if we're on a page with the authorization code
        URL currentUrl = resultPage.getUrl();
        String code = extractCodeFromUrl(currentUrl.toString());
        if (code != null) {
            return code;
        }

        // Check WebClient's history for redirects
        for (org.htmlunit.TopLevelWindow window : webClient.getTopLevelWindows()) {
            if (window.getEnclosedPage() instanceof HtmlPage) {
                HtmlPage page = (HtmlPage) window.getEnclosedPage();
                code = extractCodeFromUrl(page.getUrl().toString());
                if (code != null) {
                    return code;
                }
            }
        }

        // Check response headers for Location header
        if (resultPage.getWebResponse() != null) {
            String location = resultPage.getWebResponse().getResponseHeaderValue("Location");
            if (location != null) {
                code = extractCodeFromUrl(location);
                if (code != null) {
                    return code;
                }
            }
        }

        return null;
    }

    private static String extractCodeFromUrl(String url) {
        if (url == null || !url.contains("code=")) {
            return null;
        }

        try {
            URL parsedUrl = new URL(url);
            String query = parsedUrl.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] parts = param.split("=", 2);
                    if (parts.length == 2 && "code".equals(parts[0])) {
                        return parts[1];
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse URL: " + url, e);
        }

        return null;
    }

    static String createToken(KeycloakService keycloak) {
        // we retry and create AuthzClient as we experienced following exception in the past: 'Caused by:
        // org.apache.http.NoHttpResponseException: keycloak-ts-juwpkvyduk.apps.ocp4-15.dynamic.quarkus:80 failed to respond'
        return createToken(1, keycloak);
    }

    private static String createToken(int attemptCount, KeycloakService keycloak) {
        try {
            return keycloak.createAuthzClient(CLIENT_ID_DEFAULT, CLIENT_SECRET_DEFAULT).obtainAccessToken(USER, USER)
                    .getToken();
        } catch (RuntimeException e) {
            LOG.error("Attempt #%d to create token failed with exception:".formatted(attemptCount), e);
            if (e.getCause() instanceof org.apache.http.NoHttpResponseException && attemptCount < 3) {
                LOG.info("Retrying to create token.");
                return createToken(attemptCount + 1, keycloak);
            }
            throw e;
        }

    }

    public static String getAccessTokenWithAcr(KeycloakService keycloak, String acrValue) {
        try {
            String authServerUrl = keycloak.getRealmUrl();

            String authorizationCode = getAuthorizationCode(keycloak, acrValue);

            String tokenEndpoint = authServerUrl + "/protocol/openid-connect/token";

            RestAssuredConfig config = createRestAssuredConfig(keycloak);

            Response tokenResponse = RestAssured
                    .given()
                    .config(config)
                    .formParam("grant_type", "authorization_code")
                    .formParam("client_id", CLIENT_ID_DEFAULT)
                    .formParam("client_secret", CLIENT_SECRET_DEFAULT)
                    .formParam("code", authorizationCode)
                    .formParam("redirect_uri", "http://localhost:8081/callback")
                    .when()
                    .post(tokenEndpoint);

            if (tokenResponse.statusCode() != 200) {
                LOG.errorf("Error getting token: Status: %d, Body: %s",
                        tokenResponse.statusCode(), tokenResponse.body().asString());
                throw new RuntimeException("Failed to get access token");
            }

            String accessToken = tokenResponse.jsonPath().getString("access_token");

            // Verify the token contains the expected ACR claim
            if (acrValue != null && !acrValue.isEmpty()) {
                verifyAcrClaim(accessToken, acrValue);
            }

            return accessToken;

        } catch (Exception e) {
            LOG.error("Failed to get access token with ACR", e);
            throw new RuntimeException("Failed to get access token with ACR: " + e.getMessage(), e);
        }
    }

    private static RestAssuredConfig createRestAssuredConfig(KeycloakService keycloak) {
        Map<String, String> tlsProperties = keycloak.getTlsProperties();
        String trustStorePath = tlsProperties.get("quarkus.tls.keycloak.trust-store.p12.path");
        String trustStorePassword = tlsProperties.get("quarkus.tls.keycloak.trust-store.p12.password");

        RestAssuredConfig config = RestAssured.config();
        if (trustStorePath != null) {
            config = config.sslConfig(new SSLConfig()
                    .trustStore(trustStorePath, trustStorePassword)
                    .relaxedHTTPSValidation());
        }
        return config;
    }

    private static void verifyAcrClaim(String accessToken, String expectedAcr) {
        String[] parts = accessToken.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException("Invalid JWT token format");
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            if (!payload.contains("\"acr\":\"" + expectedAcr + "\"")) {
                LOG.error("Token payload does not contain expected ACR: " + payload);
                throw new RuntimeException("Token does not contain expected ACR value: " + expectedAcr);
            }
        } catch (Exception e) {
            LOG.error("Failed to verify ACR claim", e);
            throw new RuntimeException("Failed to verify ACR claim", e);
        }
    }

    private static String getAuthorizationCode(KeycloakService keycloak, String acrValue) {
        String authServerUrl = keycloak.getRealmUrl();
        String clientId = CLIENT_ID_DEFAULT;

        // Build authorization URL with ACR
        String authUrl = authServerUrl + "/protocol/openid-connect/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode("http://localhost:8081/callback", StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=openid";

        if (acrValue != null && !acrValue.isEmpty()) {
            // Use claims parameter for essential ACR request
            String claims = "{\"id_token\":{\"acr\":{\"essential\":true,\"values\":[\"" + acrValue + "\"]}}}";
            authUrl += "&claims=" + URLEncoder.encode(claims, StandardCharsets.UTF_8);
        }

        // Simulate browser login flow
        return simulateBrowserLogin(authUrl, keycloak);
    }

}
