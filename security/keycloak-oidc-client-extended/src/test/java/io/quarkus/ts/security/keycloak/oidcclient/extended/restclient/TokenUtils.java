package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.test.bootstrap.KeycloakService;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

final class TokenUtils {

    private static final Logger LOG = Logger.getLogger(TokenUtils.class);
    static final String USER = "test-user";

    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

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

    public static String getAccessTokenWithAcr(Set<String> acrValues) {
        return getAccessTokenVerifiedWithOidcServer(acrValues, null);
    }

    private static String getAccessTokenVerifiedWithOidcServer(Set<String> acrValues, Long authTime) {
        // get access token from simple OIDC resource
        String json = RestAssured
                .given()
                .queryParam("auth_time", authTime == null ? "" : Long.toString(authTime))
                .queryParam("acr", acrValues == null ? "" : String.join(",", acrValues))
                .when()
                .post("/step-up/accesstoken-with-acr")
                .body().asString();
        JsonObject object = new JsonObject(json);
        return object.getString("access_token");
    }

}
