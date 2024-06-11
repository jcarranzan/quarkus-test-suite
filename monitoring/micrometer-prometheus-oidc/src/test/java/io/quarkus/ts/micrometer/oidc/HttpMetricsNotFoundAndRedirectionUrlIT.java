package io.quarkus.ts.micrometer.oidc;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class HttpMetricsNotFoundAndRedirectionUrlIT extends BaseMicrometerOidcSecurityIT {

    static final String REDIRECTION_PATH = "/testredirection";
    static final String NO_VALID_PATH = "/starwars";
    static final String HTTP_METRIC = "http_server_requests_seconds_count{method=\"GET\",";
    static final String REDIRECT_HTTP_CALL_METRIC = HTTP_METRIC + "outcome=\"REDIRECTION\",status=\"302\",uri=\"%s\"}";
    static final String NOT_FOUND_HTTP_CALL_METRIC = HTTP_METRIC + "outcome=\"CLIENT_ERROR\",status=\"404\",uri=\"%s\"}";

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", keycloak::getRealmUrl)
            .withProperty("quarkus.oidc.client-id", CLIENT_ID_DEFAULT)
            .withProperty("quarkus.oidc.credentials.secret", CLIENT_SECRET_DEFAULT);

    @Test
    public void shouldTraceUriNotFound() {
        whenCallNoExistEndpoint();
        thenMetricIsExposedInServiceEndpoint(NOT_FOUND_HTTP_CALL_METRIC, 1, NO_VALID_PATH);
    }

    @Test
    public void shouldTraceUriWithRedirection() {
        whenCallRedirectionEndpoint();
        thenMetricIsExposedInServiceEndpoint(REDIRECT_HTTP_CALL_METRIC, 1, USER_PATH + REDIRECTION_PATH);
    }

    private void whenCallNoExistEndpoint() {
        getApp().given().get(NO_VALID_PATH)
                .then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    private void whenCallRedirectionEndpoint() {
        getApp().given()
                .auth().oauth2(getToken(NORMAL_USER, NORMAL_USER))
                .redirects().follow(false)
                .get(USER_PATH + "/testredirection")
                .then().statusCode(HttpStatus.SC_MOVED_TEMPORARILY);

    }

    private String getToken(String userName, String password) {
        return authzClient.obtainAccessToken(userName, password).getToken();
    }

    private void thenMetricIsExposedInServiceEndpoint(String metricFormat, Integer expected, String path) {
        await().ignoreExceptions().atMost(ASSERT_SERVICE_TIMEOUT_MINUTES, TimeUnit.MINUTES).untilAsserted(() -> {
            String shouldContain = String.format(metricFormat, path);

            if (expected != null) {
                shouldContain += " " + expected;
            }

            getApp().given().get("/q/metrics")
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .body(containsString(shouldContain));

        });
    }

    @Override
    protected RestService getApp() {
        return app;
    }
}
