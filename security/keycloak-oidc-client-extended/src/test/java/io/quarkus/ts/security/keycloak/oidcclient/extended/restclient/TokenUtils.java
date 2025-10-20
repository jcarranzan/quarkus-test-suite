package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.test.bootstrap.KeycloakService;

final class TokenUtils {

    private static final Logger LOG = Logger.getLogger(TokenUtils.class);
    static final String USER = "test-user";

    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    static final String TOKEN_ENDPOINT = "/protocol/openid-connect/token";

    private TokenUtils() {
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

    static String createToken(KeycloakService keycloak, String username, String password) {
        LOG.infof("Requesting token for user '%s'", username);
        String token = given()
                .relaxedHTTPSValidation()
                .formParams(Map.of(
                        "grant_type", "password",
                        "client_id", CLIENT_ID_DEFAULT,
                        "client_secret", CLIENT_SECRET_DEFAULT,
                        "username", username,
                        "password", password))
                .when()
                .post(keycloak.getRealmUrl() + TOKEN_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().path("access_token");

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Failed to get token for user " + username);
        }

        LOG.debugf("Token for user '%s' acquired", username);
        return token;
    }

}
