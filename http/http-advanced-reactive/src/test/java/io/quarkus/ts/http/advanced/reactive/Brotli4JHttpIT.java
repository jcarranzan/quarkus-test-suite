package io.quarkus.ts.http.advanced.reactive;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.Decoder;
import com.aayushatharva.brotli4j.decoder.DecoderJNI;
import com.aayushatharva.brotli4j.decoder.DirectDecompress;
import com.aayushatharva.brotli4j.encoder.Encoder;
import io.restassured.response.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Tag("QQE-378")
@QuarkusScenario
public class Brotli4JHttpIT {
    @QuarkusApplication(classes = {Brotli4JResource.class,Brotli4JHttpServerConfig.class, Brotli4JRestMock.class}, properties = "compression.properties")
    static RestService app = new RestService();

    private final static String DEFAULT_TEXT_PLAIN = "In life, you have to trust that every little bit helps. As you know, every small step forward counts." +
            " It's the accumulation of these efforts that ultimately leads to success." +
            " So, don't underestimate the power of persistence and determination in achieving your dreams";

    private final static String CONTENT_LENGTH_DEFAULT_TEXT_PLAIN = String.valueOf(DEFAULT_TEXT_PLAIN.length());
    private final static String CONTENT_LENGTH_BROTLI4J_COMPRESSION_TEXT_PLAIN = "187";

    private final static String CONTENT_LENGTH_BROTLI4J_COMPRESSION_JSON = "236";

    private final static String BROTLI_ENCODING = "br";

    @BeforeAll
    public static void setUp(){
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    public void disableCompression(){
        System.setProperty("quarkus.http.enable-compression", "false");
        app.given()
                .when()
                .contentType(MediaType.TEXT_PLAIN)
                .get("/compression/text")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, CONTENT_LENGTH_DEFAULT_TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_ENCODING, nullValue())
                .body(is(DEFAULT_TEXT_PLAIN))
                .log().all();
    }

    @Test
    public void checkTextPlainCompressedWithtBrotli4J() throws IOException {
       // Brotli4jLoader.ensureAvailability();
        byte[] textBytes = DEFAULT_TEXT_PLAIN.getBytes();
        int brotliEncodedLength = calculateBrotliLength(textBytes);

        Response response = app.given()
                .header(HttpHeaders.ACCEPT_ENCODING, BROTLI_ENCODING)
                .get("/compression/text")
                .then()
                .statusCode(200)
                .and()
                .header("content-length", String.valueOf(brotliEncodedLength))
                .header(HttpHeaders.CONTENT_ENCODING, "br")
               .extract().response();

    }


    @Test
    public void checkJsonBrotli4JCompression() {
        app.given()
                .header(HttpHeaders.ACCEPT_ENCODING, BROTLI_ENCODING)
                .get("/compression/brotli/json")
                .then()
                .statusCode(200)
                .header("content-length", CONTENT_LENGTH_BROTLI4J_COMPRESSION_JSON)
                .header("content-encoding", BROTLI_ENCODING)
                .log().all();
    }

    @Test
    public void checkCompressedAndDecompressedWithQuarkusFailed() {
        // Send the request with the Accept-Encoding header set to br
        Response response = app.given()
                .header(HttpHeaders.ACCEPT_ENCODING, "br")
                .get("/compression/text")
                .then()
                .statusCode(200)
                .and()
                .header("content-length", CONTENT_LENGTH_BROTLI4J_COMPRESSION_TEXT_PLAIN)
                .header("content-encoding", "br")
                .log().all()
                .extract().response();

        // Assert that the response body matches the original text
        assertThat(response.getBody().asString(), is(DEFAULT_TEXT_PLAIN));
    }

    @Test
    public void checkCompressedAndDecompressedWithQuarkus() {
        // Send the compressed text to the endpoint
        Response response = app.given()
                .header(HttpHeaders.ACCEPT_ENCODING, "br")
                .get("/compression/text")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_LENGTH, CONTENT_LENGTH_BROTLI4J_COMPRESSION_TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_ENCODING, "br")
                .log().all()
                .extract().response();
        byte[] compressedBytes = response.asByteArray();

       Response decompressionResponse = app.given()
               .header(HttpHeaders.ACCEPT_ENCODING, "br")
                .body(compressedBytes)
                .post("/compression/decompression")
                .then()
                .statusCode(200)
                .log().all()
               .extract().response();;
        String decompressedText = decompressionResponse.getBody().asString();

        System.out.println("LA respuesta " + decompressedText);

        // Assert that the decompressed text matches the original text
      //  assertThat(response.getBody().asString(), is(DEFAULT_TEXT_PLAIN));
    }

    @Test
    public void checkCompressedAndDecompressedWithBrotli4J() throws IOException {
        Response response = app.given()
                .header(HttpHeaders.ACCEPT_ENCODING, "br")
                .get("/compression/text")
                .then()
                .statusCode(200)
                .and()
                .header("content-length", CONTENT_LENGTH_BROTLI4J_COMPRESSION_TEXT_PLAIN)
                .log().all()
                .extract().response();
        byte[] compressedBytes = response.asByteArray();
        String decompressed =   testDecompressionWithBrotli4J(compressedBytes);
        assertThat(decompressed, is(DEFAULT_TEXT_PLAIN));
    }


    public String testDecompressionWithBrotli4J(byte[] compressedData) throws IOException {
        String decompresed = "";

        if(compressedData != null){
            DirectDecompress directDecompress = Decoder.decompress(compressedData);

            if (directDecompress.getResultStatus() == DecoderJNI.Status.DONE) {
                decompresed = new String(directDecompress.getDecompressedData());
                System.out.println("Decompression Successful: " + decompresed);

            } else {
                System.out.println("Some Error Occurred While Decompressing");
            }
        }else {
            System.out.println("compressData is null");
        }


        return decompresed;
    }

    private int calculateBrotliLength(byte[] data) throws IOException {
        Encoder.Parameters parameters = new Encoder.Parameters();
        byte[] compressedData = Encoder.compress(data, parameters);
        System.out.println("LENGTH = " + compressedData.length);
        return compressedData.length;
    }


}