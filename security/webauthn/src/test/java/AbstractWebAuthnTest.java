import static io.restassured.RestAssured.given;

import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.security.webauthn.WebAuthnController;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

public abstract class AbstractWebAuthnTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("AbstractWebAuthnTest");

    protected abstract RestService getApp();

    enum User {
        USER;
    }

    enum Endpoint {
        DEFAULT,
        MANUAL;
    }

    @Test
    public void testWebAuthnUser() {
        testWebAuthn("Roosvelt", User.USER, Endpoint.MANUAL);
    }

    private void testWebAuthn(String userName, User user, Endpoint endpoint) {
        Filter cookieFilter = new CookieFilter();
        WebAuthnHardware token = new WebAuthnHardware();

        verifyLoggedOut(cookieFilter);

        // two-step registration
        String challenge = WebAuthnEndpointHelper.invokeRegistration(userName, cookieFilter);
        JsonObject registrationJson = token.makeRegistrationJson(challenge);
        if (endpoint == Endpoint.DEFAULT)
            WebAuthnEndpointHelper.invokeCallback(registrationJson, cookieFilter);
        else {
            invokeCustomEndpoint("/register", cookieFilter, request -> {
                WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registrationJson);
                request.formParam("userName", userName);
            });
        }

        // verify that we can access logged-in endpoints
        verifyLoggedIn(cookieFilter, userName, user);

        // logout
        WebAuthnEndpointHelper.invokeLogout(cookieFilter);

        verifyLoggedOut(cookieFilter);

        // two-step login
        challenge = WebAuthnEndpointHelper.invokeLogin(userName, cookieFilter);
        JsonObject loginJson = token.makeLoginJson(challenge);
        if (endpoint == Endpoint.DEFAULT)
            WebAuthnEndpointHelper.invokeCallback(loginJson, cookieFilter);
        else {
            invokeCustomEndpoint("/login", cookieFilter, request -> {
                WebAuthnEndpointHelper.addWebAuthnLoginFormParameters(request, loginJson);
                request.formParam("userName", userName);
            });
        }

        // verify that we can access logged-in endpoints
        verifyLoggedIn(cookieFilter, userName, user);

        // logout
        WebAuthnEndpointHelper.invokeLogout(cookieFilter);

        verifyLoggedOut(cookieFilter);
    }

    private void invokeCustomEndpoint(String uri, Filter cookieFilter, Consumer<RequestSpecification> requestCustomiser) {
        RequestSpecification request = given()
                .when();
        requestCustomiser.accept(request);
        request
                .filter(cookieFilter)
                .redirects().follow(false)
                .log().ifValidationFails()
                .post(uri)
                .then()
                .statusCode(200)
                .log().ifValidationFails()
                .cookie(WebAuthnController.CHALLENGE_COOKIE, Matchers.is(""))
                .cookie(WebAuthnController.USERNAME_COOKIE, Matchers.is(""))
                .cookie("quarkus-credential", Matchers.notNullValue());
    }

    private void verifyLoggedIn(Filter cookieFilter, String userName, User user) {
        // public API still good
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

        // user API accessible
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

    }

    private void verifyLoggedOut(Filter cookieFilter) {
        // public API still good
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .when()
                .get("/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is("<not logged in>"));

        // user API not accessible
        RestAssured.given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(302)
                .header("Location", Matchers.is("http://localhost:1101/login.html"));

    }
}
