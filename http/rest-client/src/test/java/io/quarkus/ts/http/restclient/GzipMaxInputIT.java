package io.quarkus.ts.http.restclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import jakarta.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class GzipMaxInputIT {

     final byte[] gzip_bellow_max_input_1K = new byte[1000];
    final byte[] gzip_max_input_10M = new byte[10000000];

    @QuarkusApplication(classes = { GzipResource.class, GzipClientService.class }, properties = "gzip.properties")
    static RestService app = new RestService();

    /**
     *
     * According "All configurations options" guide the property 'quarkus.resteasy.gzip.max-input' refers to
     *
     * Maximum deflated file bytes size
     *
     * If the limit is exceeded, Resteasy will return Response with status 413("Request Entity Too Large")
     *
     */

    @Test
    void testGzipOverTheMaxLimit() throws IOException {

        byte[] compressedData = generateCompressedData(gzip_max_input_10M);

        Response response = app.given()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Encoding", "gzip")
                .body(compressedData)
                .when()
                .post("/gzip")
                .then()
                .extract().response();
        assertEquals(HttpStatus.SC_REQUEST_TOO_LONG, response.statusCode());
    }

    @Test
    void testGzipBelowMaxLimit() throws IOException {
         byte[] compressedData = generateCompressedData(gzip_bellow_max_input_1K);

        Response response = app.given()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Encoding", "gzip")
                .body(compressedData)
                .when()
                .post("/gzip")
                .then().log().all()
                .extract().response();
    }

    private byte[] generateCompressedData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
        gzipOut.write(data);
        gzipOut.close();
        return baos.toByteArray();
    }

}
