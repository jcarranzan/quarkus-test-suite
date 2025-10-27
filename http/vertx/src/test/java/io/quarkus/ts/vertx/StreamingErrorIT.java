package io.quarkus.ts.vertx;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.HttpMethod;

@Tag("https://github.com/quarkusio/quarkus/issues/50336")
@QuarkusScenario
public class StreamingErrorIT {

    @QuarkusApplication
    static final RestService app = new RestService();

    private static final int ITEMS_PER_EMIT = 100;
    private static final int TOTAL_ITEMS = ITEMS_PER_EMIT * 2;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private Vertx vertx;
    private HttpClient client;

    @BeforeEach
    public void setup() {
        vertx = Vertx.vertx();
        client = vertx.createHttpClient();
    }

    @AfterEach
    public void cleanup() throws Exception {
        if (client != null) {
            client.close().toCompletionStage().toCompletableFuture().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }
    }

    @Test
    public void testFailureMidStream() {
        AtomicLong count = new AtomicLong();
        CompletableFuture<Void> latch = new CompletableFuture<>();

        sendRequest("/streaming-error?fail=true", latch, b -> count.getAndIncrement());

        Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
            ExecutionException ex = Assertions.assertThrows(ExecutionException.class,
                    latch::get,
                    "Client should have failed as the server reset the connection");

            Assertions.assertInstanceOf(HttpClosedException.class, ex.getCause(),
                    "Expected the connection to be closed abruptly");
        });

        Assertions.assertEquals(ITEMS_PER_EMIT, count.get(),
                "Should have received first batch of items before failure");
    }

    @Test
    public void testNoFailure() {
        AtomicLong count = new AtomicLong();
        CompletableFuture<Void> latch = new CompletableFuture<>();

        sendRequest("/streaming-error", latch, b -> count.getAndIncrement());
        Assertions.assertTimeoutPreemptively(TIMEOUT,
                () -> latch.get(),
                "The stream should have completed successfully within the timeout");
        Assertions.assertEquals(TOTAL_ITEMS, count.get(),
                "Should have received all items in a successful stream");
    }

    private void sendRequest(String requestURI, CompletableFuture<Void> latch, Consumer<Buffer> bodyConsumer) {
        int port = app.getURI().getPort();
        String host = app.getURI().getHost();

        client.request(HttpMethod.GET, port, host, requestURI)
                .onFailure(latch::completeExceptionally)
                .onSuccess(request -> {
                    request.connect()
                            .onFailure(latch::completeExceptionally)
                            .onSuccess(response -> {
                                response.handler(buffer -> {
                                    if (buffer.length() > 0) {
                                        bodyConsumer.accept(buffer);
                                    }
                                });
                                response.exceptionHandler(latch::completeExceptionally);
                                response.end(v -> latch.complete(null));
                            });
                });
    }

    @Disabled("https://github.com/quarkusio/quarkus/issues/50754")
    @Test
    public void testStreamingOutputFailureMidStream() {
        AtomicLong count = new AtomicLong();
        CompletableFuture<Void> latch = new CompletableFuture<>();

        sendRequest("/streaming-output-error?fail=true", latch, b -> count.getAndIncrement());

        Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
            ExecutionException ex = Assertions.assertThrows(ExecutionException.class,
                    () -> latch.get(),
                    "Client should have failed as the server reset the connection (StreamingOutput)");

            Assertions.assertInstanceOf(HttpClosedException.class, ex.getCause(),
                    "Expected the connection to be closed abruptly (StreamingOutput)");
        });

        Assertions.assertEquals(ITEMS_PER_EMIT, count.get(),
                "Should have received first batch of items before failure (StreamingOutput)");
    }

    @Test
    public void testStreamingOutputNoFailure() {
        AtomicLong count = new AtomicLong();
        CompletableFuture<Void> latch = new CompletableFuture<>();

        sendRequest("/streaming-output-error", latch, b -> count.getAndIncrement());

        Assertions.assertTimeoutPreemptively(TIMEOUT,
                () -> latch.get(),
                "The stream should have completed successfully (StreamingOutput)");

        Assertions.assertEquals(TOTAL_ITEMS, count.get(),
                "Should have received all items (StreamingOutput)");
    }

}
