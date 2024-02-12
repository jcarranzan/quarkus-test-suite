package io.quarkus.ts.http.http2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.URILike;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@QuarkusScenario
@ExtendWith(VertxExtension.class)
public class Http2IT {

    @QuarkusApplication(ssl = true)
    static RestService app = new RestService();

    private static URILike baseUri;
    static private Vertx vertx;

    private static final String GREETING = "Buenos dias";

    private static final String HTTP_2_VERSION = "HTTP_2";

    private static HttpClient httpClient;

    @BeforeAll
    static void setUp() {
        baseUri = app.getURI();
        vertx = Vertx.vertx();
    }

    @Test
    @DisplayName("HttpClient Vertx HTTP/1.1 Test")
    public void httpClientVertxHttp1(VertxTestContext vertxTestContext) {
        Checkpoint requestCheckpoint = vertxTestContext.checkpoint();
        assertNotNull(vertx);
        httpClient = vertx.createHttpClient();
        assertNotNull(httpClient);
        String expectedHTTP1Version = "HTTP_1_1";

        vertxTestContext.verify(() -> {
            httpClient.request(HttpMethod.GET, baseUri.getPort(), baseUri.getHost(), "/greeting")
                    .compose(req -> req.send()
                            .compose(httpClientResponse -> {
                                System.out.println("**** Status Code " + httpClientResponse.statusCode());
                                requestCheckpoint.flag();
                                assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                                assertEquals(expectedHTTP1Version, httpClientResponse.version().toString());
                                return httpClientResponse.body();
                            }))
                    .onSuccess(body -> {
                        System.out.println("Got data " + body.toString("ISO-8859-1"));
                        assertThat("Body response", body.toString().contains(GREETING));

                        requestCheckpoint.flag();

                    }).onFailure(Throwable::printStackTrace);
        });

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
        vertxTestContext.verify(() -> {
            httpClient.request(HttpMethod.GET, 8443, baseUri.getHost(), "/greeting")
                    .compose(req -> req.send()
                            .compose(httpClientResponse -> {
                                requestCheckpoint.flag();
                                assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                                assertEquals(HTTP_2_VERSION, httpClientResponse.version().toString());
                                return httpClientResponse.body();
                            }))
                    .onSuccess(body -> {
                        assertThat("Body response", body.toString().contains(GREETING));
                        requestCheckpoint.flag();
                    }).onFailure(Throwable::printStackTrace);
        });
    }

    @Test
    @DisplayName("http2 protocol for http")
    void http2H2CTest(VertxTestContext vertxTestContext) {
        HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        httpClient = vertx.createHttpClient(options);
        httpClient.request(HttpMethod.GET, baseUri.getPort(), baseUri.getHost(), "/greeting")
                .compose(request -> request.send()
                        .compose(httpClientResponse -> {
                            System.out.println("Got response {} with protocol {}" + httpClientResponse.statusCode() + " "
                                    + httpClientResponse.version());
                            assertEquals(HttpStatus.SC_OK, httpClientResponse.statusCode());
                            assertEquals(HTTP_2_VERSION, httpClientResponse.version().toString());
                            return httpClientResponse.body();
                        }))
                .onSuccess(body -> vertxTestContext.verify(() -> {
                    assertThat("Body response", body.toString().contains(GREETING));
                    vertxTestContext.completeNow();
                })).onFailure(Throwable::printStackTrace);

    }

    @Test
    @DisplayName("undertow servlet test http2")
    void underTowHttp2Servert(VertxTestContext vertxTestContext) {
        String uriUnderTow = "/servlet/hello";
        String expectedResponseFromUndertow = "From the Web Servlet man";
        HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        httpClient = vertx.createHttpClient(options);
        httpClient.request(HttpMethod.GET, baseUri.getPort(), baseUri.getHost(), uriUnderTow)
                .compose(request -> request.send()
                        .compose(httpClientResponse -> {
                            System.out.println("Got response {} with protocol {}" + httpClientResponse.statusCode() + " "
                                    + httpClientResponse.version());
                            return httpClientResponse.body();
                        }))
                .onSuccess(body -> vertxTestContext.verify(() -> {
                    assertThat("Body response", body.toString().contains(expectedResponseFromUndertow));
                    vertxTestContext.completeNow();
                })).onFailure(Throwable::printStackTrace);

    }

    @AfterAll
    public static void closeConnections() {
        httpClient.close();
        vertx.close();
    }

}
