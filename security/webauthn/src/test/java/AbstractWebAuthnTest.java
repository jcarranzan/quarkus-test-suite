import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;

public abstract class AbstractWebAuthnTest {

    protected abstract RestService getApp();

    private static HttpClient httpClient = Vertx.vertx().createHttpClient();

    private String registerUrl = "/q/webauthn/register";
    private String logOutUrl = "/q/webauthn/logout";
    private String userName = "Roosvelt";
    private Filter cookieFilter;

    @BeforeEach
    public void setup() {
        cookieFilter = new CookieFilter();
        verifyLoggedOut(cookieFilter);
    }

    @Test
    public void testRegisterWebAuthnUser() {

        httpClient.request(HttpMethod.POST, registerUrl)
                .compose(httpClientRequest -> {
                    httpClientRequest.putHeader("Content-Type", "text/plain")
                            .write(userName);
                    return httpClientRequest.send();
                }).onSuccess(httpClientResponse -> {
                    assertThat(httpClientResponse.statusCode(), equalTo(200));
                    //  LOGGER.info("RESPONSE --A " + httpClientResponse.toString());
                    verifyLoggedIn(cookieFilter, userName);
                });

    }

    @Test
    public void testLogOutUser() {
        httpClient.request(HttpMethod.POST, logOutUrl)
                .compose(httpClientRequest -> {
                    httpClientRequest.putHeader("Content-Type", "text/plain")
                            .write(userName);
                    return httpClientRequest.send();
                }).onSuccess(httpClientResponse -> {
                    assertThat(httpClientResponse.statusCode(), equalTo(200));
                    verifyLoggedOut(cookieFilter);
                });

    }

    private void verifyLoggedIn(Filter cookieFilter, String userName) {

        // public API still good
        RestAssured.given().filter(cookieFilter)
                .get("/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .get("/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

        // user API accessible
        RestAssured.given().filter(cookieFilter)
                .get("/api/users/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

    }

    private void verifyLoggedOut(Filter cookieFilter) {
        // public API still good
        RestAssured.given().filter(cookieFilter)
                .get("/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .get("/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is("<not logged in>"));

        // user API not accessible
        RestAssured.given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .get("/api/users/me")
                .then()
                .statusCode(302)
                .header("Location", Matchers.is("http://localhost:1101/login.html"));

    }
}
