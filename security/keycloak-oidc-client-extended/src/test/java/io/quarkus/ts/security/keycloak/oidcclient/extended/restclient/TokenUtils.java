package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.restassured.RestAssured.given;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.test.bootstrap.KeycloakService;
import io.restassured.response.Response;

final class TokenUtils {

    private static final Logger LOG = Logger.getLogger(TokenUtils.class);
    static final String USER = "test-user";

    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    static final String TOKEN_ENDPOINT = "/protocol/openid-connect/token";
    static final String CLIENT_ID = "test-application-client";
    static final String CLIENT_SECRET = "test-application-client-secret";

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

    public static String createToken(KeycloakService keycloak, String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("username", username);
        params.put("password", password);
        params.put("scope", "openid profile email roles");

        Response response = given()
                .relaxedHTTPSValidation()
                .formParams(params)
                .when()
                .post(keycloak.getRealmUrl() + TOKEN_ENDPOINT);

        response.then().statusCode(200);
        String token = response.jsonPath().getString("access_token");

        LOG.info("Token created for user: " + token);
        return response.jsonPath().getString("access_token");
    }

}
