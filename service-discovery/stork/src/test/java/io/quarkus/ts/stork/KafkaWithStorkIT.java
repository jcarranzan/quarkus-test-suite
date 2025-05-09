package io.quarkus.ts.stork;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaVendor;

@QuarkusScenario
public class KafkaWithStorkIT {

    private static final Logger testLog = Logger.getLogger(KafkaWithStorkIT.class);

    @KafkaContainer(vendor = KafkaVendor.STRIMZI)
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication(classes = {
            IGreetingResource.class, GreetingResource.class, PriceConsumer.class, KafkaPriceProducer.class
    })
    static RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl);

    @Test
    public void testNoStorkNPEOnGracefulShutdown_LogCheck() {
        testLog.info("Starting test: Verifying no Stork NPE in logs during graceful shutdown.");

        long initialRunDurationMillis = 5000;
        try {
            Thread.sleep(initialRunDurationMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while waiting for initial Kafka processing.", e);
        }
        // shutdown
        app.stop();
        testLog.info("Application shutdown completed.");

        testLog.info("Verifying application logs for the specific Stork NPE signature...");
        List<String> appLogs = app.getLogs();

        assertNotNull(appLogs,
                "Application logs should be available for verification. If null, the test cannot confirm NPE absence.");
        if (appLogs.isEmpty()) {
            testLog.warn("Application logs are empty. Cannot verify absence of NPE via logs.");
        }

        String npeStringPart1 = "Stork.getInstance()\" is null";
        String npeStringPart2 = "StorkClientRequestFilter";

        boolean storkNpeFound = false;
        for (String logLine : appLogs) {
            if (logLine.contains(npeStringPart1) && logLine.contains(npeStringPart2)) {
                testLog.error("Stork NPE String FOUND! in application logs: " + logLine);
                storkNpeFound = true;
                break;
            }
        }
        assertFalse(storkNpeFound,
                "The Stork NPE related to 'Stork.getInstance() is null' in 'StorkClientRequestFilter' " +
                        "should NOT be present in the application logs after graceful shutdown with the fix.");
    }
}
