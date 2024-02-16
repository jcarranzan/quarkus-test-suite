package io.quarkus.ts.http.http2;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.URILike;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import java.util.concurrent.atomic.AtomicLong;

@QuarkusScenario
@ExtendWith(VertxExtension.class)
public class Http2IT {
    private static final Logger LOG = Logger.getLogger(Http2IT.class);
    @QuarkusApplication(ssl = true, classes = { MorningResource.class, CustomFrameHandler.class, CustomFrameResource.class, FrameProcessingService.class }
            , properties = "application.properties")
    static RestService app = new RestService();

    private static URILike baseUri;
    static private Vertx vertx;

    private static final String BASE_ENDPOINT = "/morning";
    private static final String GREETING = "Buenos dias";

    private static HttpClient httpClient;

    @BeforeAll
    static void setUp() {
        baseUri = app.getURI();
        vertx = Vertx.vertx();
    }

    @Test
    @DisplayName("HttpClient Vertx HTTP/1.1 Test")
    public void httpClientVertxHttp1(VertxTestContext vertxTestContext) {
        Checkpoint requestCheckpoint = vertxTestContext.checkpoint(2);
        assertNotNull(vertx);
        httpClient = vertx.createHttpClient();
        assertNotNull(httpClient);
        vertxTestContext.verify(() -> httpClient.request(HttpMethod.GET, baseUri.getPort(), baseUri.getHost(), BASE_ENDPOINT)
                .compose(req -> req.send()
                        .compose(httpClientResponse -> {
                            LOG.info("**** Status Code " + httpClientResponse.statusCode());

                            assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                            assertEquals(HttpVersion.HTTP_1_1, httpClientResponse.version());
                            requestCheckpoint.flag();
                            return httpClientResponse.body();
                        }))
                .onSuccess(body -> {
                    LOG.info("Got data " + body.toString("ISO-8859-1"));
                    assertThat("Body response", body.toString().contains(GREETING));

                    requestCheckpoint.flag();

                }).onFailure(Throwable::printStackTrace));

    }

    @Test
    @DisplayName("HttpClient Vertx HTTP/2 Test")
    public void testHttp2(VertxTestContext vertxTestContext) {

        Checkpoint requestCheckpoint = vertxTestContext.checkpoint(2);
        assertNotNull(vertx);
        httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setSsl(true)
                .setUseAlpn(true)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setProtocolVersion(HttpVersion.HTTP_2));
        assertNotNull(httpClient);
        vertxTestContext.verify(() -> httpClient.request(HttpMethod.GET, 8443, baseUri.getHost(), BASE_ENDPOINT)
                .compose(req -> req.send()
                        .compose(httpClientResponse -> {
                            requestCheckpoint.flag();
                            assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                            assertEquals(HttpVersion.HTTP_2, httpClientResponse.version());
                            return httpClientResponse.body();
                        }))
                .onSuccess(body -> {
                    assertThat("Body response", body.toString().contains(GREETING));
                    requestCheckpoint.flag();
                }).onFailure(Throwable::printStackTrace));
    }

    @Test
    @DisplayName("HTTP/2 with custom headers")
    public void testCustomHeaders(VertxTestContext vertxTestContext) throws Exception {

        Checkpoint requestCheckpoint = vertxTestContext.checkpoint(2);
        assertNotNull(vertx);
        httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setSsl(true)
                .setUseAlpn(true)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setProtocolVersion(HttpVersion.HTTP_2));
        assertNotNull(httpClient);
        vertxTestContext.verify(() -> httpClient.request(HttpMethod.GET, 8443, baseUri.getHost(), BASE_ENDPOINT)
                .compose(req -> {
                    return req.send()
                            .compose(httpClientResponse -> {
                                LOG.info("HEADERS " + httpClientResponse.headers().toString());
                                assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                                assertEquals(HttpVersion.HTTP_2, httpClientResponse.version());
                                requestCheckpoint.flag();
                                return httpClientResponse.body();
                            });
                })
                .onSuccess(body -> {
                    LOG.info("Got data " + body.toString("ISO-8859-1"));
                    assertThat("Body response", body.toString().contains(GREETING));
                    requestCheckpoint.flag();
                }).onFailure(Throwable::printStackTrace));

    }

    @Test
    @DisplayName("http2 protocol for http")
    void http2ProtocolTest(VertxTestContext vertxTestContext) {
        HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        httpClient = vertx.createHttpClient(options);
        httpClient.request(HttpMethod.GET, baseUri.getPort(), baseUri.getHost(), BASE_ENDPOINT)
                .compose(request -> request.send()
                        .compose(httpClientResponse -> {
                            LOG.info("Got response {} with protocol {}" + httpClientResponse.statusCode() + " "
                                    + httpClientResponse.version());
                            assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                            assertEquals(HttpVersion.HTTP_2, httpClientResponse.version());
                            return httpClientResponse.body();
                        }))
                .onSuccess(body -> vertxTestContext.verify(() -> {
                    assertThat("Body response", body.toString().contains(GREETING));
                    vertxTestContext.completeNow();
                })).onFailure(Throwable::printStackTrace);

    }

    @Test
    @DisplayName("Verify headers types in http/2")
    void verifyHeadersTypesHttp2(VertxTestContext vertxTestContext) {
        Checkpoint checkpoint = vertxTestContext.checkpoint(2);
        HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        httpClient = vertx.createHttpClient(options);
        httpClient.request(HttpMethod.GET, baseUri.getPort(), baseUri.getHost(), "/custom-frame")
                .compose(request -> request.send()
                        .compose(httpClientResponse -> {
                            assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                            assertEquals("nosniff", httpClientResponse.getHeader("X-Content-Type-Options"));
                            assertEquals("Custom-Header", httpClientResponse.getHeader("X-Header"));
                            assertEquals(HttpVersion.HTTP_2, httpClientResponse.version());
                            checkpoint.flag();
                            return httpClientResponse.body();
                        }))
                .onSuccess(body -> {
                    System.out.println(body.toString());
                    assertThat("Body response", body.toString().contains("pong"));
                    checkpoint.flag();
                })
                .onFailure(vertxTestContext::failNow);
    }

    @Test
    void customFramesTestHttp(VertxTestContext context) {
        AtomicLong timerId = new AtomicLong();
        Checkpoint entry = context.checkpoint(2);

        HttpClient httpClient = vertx.createHttpClient(
                new HttpClientOptions()
                        .setSsl(true)
                        .setUseAlpn(true)
                        .setTrustAll(true)
                        .setVerifyHost(false)
                        .setProtocolVersion(HttpVersion.HTTP_2)
        );

        context.verify(() -> {
            httpClient.request(HttpMethod.GET, 8443, "localhost", "/custom-frame")
                    .onFailure(context::failNow)
                    .compose(httpClientRequest ->
                            httpClientRequest.sendHead().map(head -> httpClientRequest)
                                    .flatMap(request ->
                                            request.response()
                                                    .onSuccess(httpClientResponse -> {
                                                        System.out.println("SERVER *****  " );
                                                        httpClientResponse.customFrameHandler(httpFrame -> {  // Access httpFrame within the handler
                                                            LOG.info("GOT FROM SERVER " + httpFrame.payload().toString());
                                                            System.out.println("HOLA");
                                                            assertEquals("pong", httpFrame.payload().toString().toLowerCase());
                                                            entry.flag();
                                                        });
                                                    })
                                    ).onFailure(context::failNow)
                                    .map(httpClientResponse -> {
                                        vertx.setPeriodic(1000, timerID -> {
                                            timerId.set(timerID);
                                            LOG.info("Sending ping frame to server");
                                            httpClientRequest.writeCustomFrame(10, 0, Buffer.buffer("ping"));
                                            entry.flag();
                                        });
                                        return httpClientRequest;
                                    })
                    ).result();
        });
    }





    @Test
    @DisplayName("Check frames")
    void verifyFrames() {
        app.given()
                .relaxedHTTPSValidation()
                .get("https://localhost:8443/custom-frame?frame=example-frame")
                .then()
                .statusCode(200);
    }


    @AfterAll
    public static void closeConnections() {
        httpClient.close();
        vertx.close();
    }

}
