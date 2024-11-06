package io.quarkus.ts.http.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.zip.GZIPOutputStream;

import jakarta.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sun.management.OperatingSystemMXBean;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class GzipMaxInputIT {

    final String invalid_value = "";
    final long SMALL_PAYLOAD = 512;
    final long LIMIT_PAYLOAD = 100 * 1024 * 1024;
    final long OVER_LIMIT_PAYLOAD = 200 * 1024 * 1024;

    private static final long BYTES_IN_MB = 1024 * 1024;
    private final byte[] buffer = new byte[4096];

    private ByteArrayInputStream generateCompressedDataStream(long sizeInBytes) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] compressedData;
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                long bytesRemaining = sizeInBytes;

                while (bytesRemaining > 0) {
                    int bytesToWrite;

                    if (bytesRemaining >= buffer.length) {
                        bytesToWrite = buffer.length;
                    } else {
                        bytesToWrite = (int) bytesRemaining;
                    }

                    gzipOutputStream.write(buffer, 0, bytesToWrite);
                    bytesRemaining = bytesRemaining - bytesToWrite;
                }
            }

            compressedData = byteArrayOutputStream.toByteArray();

            return new ByteArrayInputStream(compressedData);

        } catch (IOException e) {
            throw new RuntimeException("Error generating compressed data stream", e);
        }
    }

    private void logMemoryUsage(String message) {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory() / BYTES_IN_MB;
        long totalMemory = runtime.totalMemory() / BYTES_IN_MB;
        long maxMemory = runtime.maxMemory() / BYTES_IN_MB;

        System.out.println("[" + message + "]");
        System.out.println("=== JVM Memory Info ===");
        System.out.println("Free Memory: " + freeMemory + " MB");
        System.out.println("Total Memory: " + totalMemory + " MB");
        System.out.println("Max Memory: " + maxMemory + " MB");

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalPhysicalMemorySize = osBean.getTotalPhysicalMemorySize() / BYTES_IN_MB;
        long freePhysicalMemorySize = osBean.getFreePhysicalMemorySize() / BYTES_IN_MB;

        System.out.println("=== System Memory Info ===");
        System.out.println("Total Physical Memory: " + totalPhysicalMemorySize + " MB");
        System.out.println("Free Physical Memory: " + freePhysicalMemorySize + " MB");
        System.out.println("--------------------------------------------------");
    }

    /**
     *
     * Tests are checking server response on different size of sent payload
     * Limit is configured using quarkus.resteasy.gzip.max-input property
     * (According "All configurations options" guide the property 'quarkus.resteasy.gzip.max-input' refers to
     * Maximum deflated file bytes size)
     * If the limit is exceeded, Resteasy will return a response with status 413("Request Entity Too Large")
     */
    @QuarkusApplication(classes = { GzipResource.class }, properties = "gzip.properties")
    static RestService app = new RestService();

    @Test
    void sendInvalidContent() {
        logMemoryUsage("Before sending maximum allowed payload");
        Response response = sendStringDataToGzipEndpoint(invalid_value);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode(),
                "Invalid data as this void string should result in 400 BAD_REQUEST response");
        logMemoryUsage("After sending invalid content");
    }

    @Test
    void sendZeroBytesPayload() throws IOException {
        logMemoryUsage("Before sending maximum allowed payload");
        ByteArrayInputStream compressedData = generateCompressedDataStream(0);
        Response response = sendDataToGzipEndpoint(compressedData);
        assertEquals(HttpStatus.SC_OK, response.statusCode(),
                "The response should be 200 OK because the compression returns 2 bytes");
        logMemoryUsage("After sending invalid content");
    }

    @Test
    void sendPayloadBelowMaxInputLimit() throws IOException {
        logMemoryUsage("Before sending maximum allowed payload");
        ByteArrayInputStream compressedData = generateCompressedDataStream(SMALL_PAYLOAD);
        Response response = sendDataToGzipEndpoint(compressedData);
        assertEquals(HttpStatus.SC_OK, response.statusCode(),
                "The response should be 200 OK because sending just 512 bytes");
        logMemoryUsage("After sending invalid content");
    }

    @Tag("https://github.com/quarkusio/quarkus/issues/39636")
    @Test
    void sendMaximumAllowedPayload() throws IOException {
        logMemoryUsage("Before sending maximum allowed payload");
        ByteArrayInputStream compressedData = generateCompressedDataStream(LIMIT_PAYLOAD);
        Response response = sendDataToGzipEndpoint(compressedData);
        assertEquals(HttpStatus.SC_OK, response.statusCode(),
                "The response should be 200 OK because sending just the limit payload configured using " +
                        "quarkus.resteasy.gzip.max-input=100M. This fails if the suffix format parsing is not " +
                        "working and RESTEasy falls back to its default which is 10M");
        logMemoryUsage("After sending invalid content");
    }

    @Test
    void sendMoreThanMaximumAllowedPayload() throws IOException {
        logMemoryUsage("Before sending maximum allowed payload");
        ByteArrayInputStream compressedData = generateCompressedDataStream(OVER_LIMIT_PAYLOAD);
        Response response = sendDataToGzipEndpoint(compressedData);
        assertEquals(HttpStatus.SC_REQUEST_TOO_LONG, response.statusCode(),
                "The response should be 413 REQUEST_TOO_LONG when sending larger payload than the limit");
        logMemoryUsage("After sending invalid content");
    }

    private Response sendDataToGzipEndpoint(InputStream dataStream) throws IOException {
        return app.given()
                .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                .body(dataStream)
                .when()
                .post("/gzip")
                .then()
                .extract().response();
    }

    private Response sendStringDataToGzipEndpoint(String data) {
        return app.given()
                .header("Content-Encoding", "gzip")
                .body(data)
                .when()
                .post("/gzip")
                .then()
                .extract().response();
    }

}
