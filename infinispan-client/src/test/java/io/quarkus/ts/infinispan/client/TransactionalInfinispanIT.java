package io.quarkus.ts.infinispan.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.InfinispanService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class TransactionalInfinispanIT {

    private static final int INFINISPAN_PORT = 11222;

    @Container(image = "${infinispan.image}", expectedLog = "${infinispan.expected-log}", port = INFINISPAN_PORT)
    static InfinispanService infinispan = new InfinispanService()
            .withUsername("admin")
            .withPassword("password");

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.infinispan-client.hosts", infinispan::getInfinispanServerAddress)
            .withProperty("quarkus.infinispan-client.username", "admin")
            .withProperty("quarkus.infinispan-client.password", "password");

    @BeforeEach
    public void setup() {
        app.given()
                .delete("/tx-books/clear")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testTransactionCommit() {
        String response = app.given()
                .post("/tx-books/commit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertTrue(response.contains("Committed"), "Transaction should have been committed");

        int cacheSize = app.given()
                .get("/tx-books/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(Integer.class);

        assertEquals(3, cacheSize, "Cache should contain 3 books after commit");
    }

    @Test
    public void testTransactionRollback() {
        String response = app.given()
                .post("/tx-books/rollback")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        assertTrue(response.contains("Rolled back"), "Transaction should have been rolled back");

        int cacheSize = app.given()
                .get("/tx-books/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(Integer.class);

        assertEquals(0, cacheSize, "Cache should be empty after rollback");
    }

    @Test
    public void testCommitAndRollback() {
        // Commit 3 books
        app.given().post("/tx-books/commit").then().statusCode(HttpStatus.SC_OK);

        // Try to add 2 more but rollback
        app.given().post("/tx-books/rollback").then().statusCode(HttpStatus.SC_OK);

        // Only 3 books should exist
        int cacheSize = app.given()
                .get("/tx-books/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(Integer.class);

        assertEquals(3, cacheSize, "Cache should only contain 3 books (from commit, not rollback)");
    }
}
