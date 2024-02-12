package io.quarkus.ts.http.http2;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.QuarkusScenario;

@QuarkusScenario
public class ServletHttp2IT {
    /*
     * @QuarkusApplication(ssl = true)
     * static RestService app = new RestService();
     */

    /*
     * @RegisterExtension
     * static final QuarkusUnitTest config = new QuarkusUnitTest()
     * .w();
     */

    /* private final URILike baseUri = app.getURI(); */

    @Test
    public void testGreeting() {
        given().port(1101)
                .when()
                .get("/servlet-greeting")
                .then().statusCode(200).body(containsString("From the Web Servlet man"));
    }
}
