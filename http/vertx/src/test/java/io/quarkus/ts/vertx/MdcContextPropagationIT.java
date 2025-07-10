package io.quarkus.ts.vertx;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@TestProfile(MdcTestProfile.class)
public class MdcContextPropagationIT {

    @QuarkusApplication(classes = {
            MdcHealthCheck.class, MdcHealthCheck2.class,
            AsyncMdcHealthCheck.class, AsyncMdcHealthCheck2.class,
            LogQueryEndpoint.class, InMemoryLogHandler.class, InMemoryLogHandlerProducer.class
    })
    static final RestService app = new RestService();

    @BeforeEach
    void resetLogs() {
        app.given().get("/logs/reset").then().statusCode(204);

    }

    @RepeatedTest(10)
    void testMdcPropagationInHealthChecks() {
        app.given().get("/q/health").then().statusCode(200);

        await().atMost(5, TimeUnit.SECONDS).until(() -> getLogRecords().size() >= 4);
        List<String> records = getLogRecords();

        assertEquals(4, records.size(), "Waiting for 4 logs, but found " + records.size() + ": " + records);

        assertTrue(
                records.stream().anyMatch(r -> r.contains("health-check=MdcHealthCheck") && r.contains("Sync Health check 1")),
                "Missing log MdcHealthCheck");
        assertTrue(
                records.stream().anyMatch(r -> r.contains("health-check=MdcHealthCheck2") && r.contains("Sync Health check 2")),
                "Missing log MdcHealthCheck2");
        assertTrue(
                records.stream()
                        .anyMatch(r -> r.contains("health-check=AsyncMdcHealthCheck") && r.contains("Async Health check 1")),
                "Missing log AsyncMdcHealthCheck");
        assertTrue(
                records.stream()
                        .anyMatch(r -> r.contains("health-check=AsyncMdcHealthCheck2") && r.contains("Async Health check 2")),
                "Missing log AsyncMdcHealthCheck2");
    }

    private List<String> getLogRecords() {
        return app.given()
                .when()
                .get("/logs/records")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", String.class);
    }
}