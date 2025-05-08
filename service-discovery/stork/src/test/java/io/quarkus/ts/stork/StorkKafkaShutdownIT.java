package io.quarkus.ts.stork;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaVendor;

@QuarkusScenario
public class StorkKafkaShutdownIT {

    private static final String NPE_ERROR = "Cannot invoke \"io.smallrye.stork.Stork.getService(String)\" because the return value of \"io.smallrye.stork.Stork.getInstance()\" is null";

    @KafkaContainer(vendor = KafkaVendor.STRIMZI)
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl);

    @Test
    public void testStorkShutdownOrderWithKafka() {
        await().atMost(Duration.ofSeconds(50))
                .until(() -> {
                    try {
                        int count = Integer.parseInt(app.given()
                                .get("/price/count")
                                .then()
                                .statusCode(200)
                                .extract().asString());
                        return count > 0;
                    } catch (Exception e) {
                        return false;
                    }
                });

        app.restart();

        await().atMost(Duration.ofSeconds(45))
                .until(() -> {
                    try {
                        app.given().get("/price/count").then().statusCode(200);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

        boolean hasStorkNpe = app.getLogs().contains(NPE_ERROR);
        assertFalse(hasStorkNpe, "NPE in Stork service discovery should not happen during shutdown");
    }
}