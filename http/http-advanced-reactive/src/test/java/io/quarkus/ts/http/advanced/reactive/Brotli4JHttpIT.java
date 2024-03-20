package io.quarkus.ts.http.advanced.reactive;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

import java.io.IOException;

@Tag("QQE-378")
@QuarkusScenario
public class Brotli4JHttpIT {
    @QuarkusApplication(classes = Brotli4JResource.class, properties = "compression.properties")
    static RestService app = new RestService();

    private final static String DEFAULT_TEXT_PLAIN = "As you know, every little bit counts";

    @Test
    public void checkTextPlainWithoutBrotli4JEncoding() {
        app.given()
                .get("/compression/text")
                .then()
                .statusCode(200)
                .and()
                .header("content-length", "62")
                .header("content-encoding","gzip")
                .body(is(DEFAULT_TEXT_PLAIN)).log().all();
    }

    @Test
    public void checkTextPlainWithtBrotli4J() {
        app.given()
                .header("Accept-Encoding", "br")
                .get("/compression/text")
                .then()
                .statusCode(200)
                .and()
                //.header("content-length", "43")
                .header("content-encoding", "br");

    }


}