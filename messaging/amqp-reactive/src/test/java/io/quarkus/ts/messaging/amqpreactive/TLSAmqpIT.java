package io.quarkus.ts.messaging.amqpreactive;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.AmqService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.AmqContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.AmqProtocol;

/**
 * Test AMQP connection over TLS with ALPN explicitly disabled.
 * Verifies the fix for ALPN-related UnsupportedOperationException (quarkusio/quarkus#46652).
 * Ensures connection succeeds, messages flow, and the specific exception is absent.
 */
@QuarkusScenario
public class TLSAmqpIT {

    private static final Logger log = Logger.getLogger(TLSAmqpIT.class);

    private static final Duration ASSERT_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration READINESS_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private static final String AMQP_TLS_PROFILE_NAME = "amqp-tls";
    private static final int AMQP_TLS_PORT = 5671;
    private static final int AMQP_NON_TLS_PORT = 5672;
    private static final String BROKER_KEYSTORE_PATH_IN_CONTAINER = "/etc/artemis/broker-keystore.p12";
    private static final String BROKER_KEYSTORE_RESOURCE_PATH = "certs/broker-keystore.p12";
    private static final String CLIENT_TRUSTSTORE_RESOURCE_PATH = "certs/client-truststore.p12";
    private static final String KEY_TRUST_STORE_PASSWORD = "password";
    private static final String EXCEPTION_TO_AVOID = "UnsupportedOperationException";

    @AmqContainer(image = "${amqbroker.image}", protocol = AmqProtocol.AMQP)
    static AmqService amq = new AmqService()
            .withProperty("AMQ_ACCEPTORS", String.format(
                    "amqp:tcp://0.0.0.0:%d;amqps:tcp://0.0.0.0:%d?sslEnabled=true&keyStorePath=%s&keyStorePassword=%s",
                    AMQP_NON_TLS_PORT, AMQP_TLS_PORT, BROKER_KEYSTORE_PATH_IN_CONTAINER, KEY_TRUST_STORE_PASSWORD))
            .withProperty(String.format("resource::%s|%s", BROKER_KEYSTORE_RESOURCE_PATH, BROKER_KEYSTORE_PATH_IN_CONTAINER),
                    "");

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("amqp-host", amq::getAmqpHost)
            .withProperty("amqp-port", () -> "" + amq.getPort())
            .withProperty("quarkus.tls." + AMQP_TLS_PROFILE_NAME + ".alpn", "false")
            .withProperty("quarkus.tls." + AMQP_TLS_PROFILE_NAME + ".trust-store.p12.path", CLIENT_TRUSTSTORE_RESOURCE_PATH)
            .withProperty("quarkus.tls." + AMQP_TLS_PROFILE_NAME + ".trust-store.p12.password", KEY_TRUST_STORE_PASSWORD);

    @Test
    public void testTlsMessagingWithAlpnDisabled() {
        await().atMost(READINESS_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .ignoreExceptions()
                .until(() -> app.given().get("/").statusCode() == 200);

        await().atMost(ASSERT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    app.given()
                            .when().get("/price")
                            .then()
                            .statusCode(200)
                            .body(not(equalTo("[]")));
                });

        app.logs().assertDoesNotContain(EXCEPTION_TO_AVOID);

    }
}
