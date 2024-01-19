package io.quarkus.ts.http.advanced.reactive;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.MultiPartSpecification;

@QuarkusScenario
public class NonJsonPayLoadIT {
    @QuarkusApplication(classes = { CityResource.class, City.class, CityListDTO.class, CityListWrapper.class,
            CityListWrapperSerializer.class,
    }, properties = "nonjson.properties")
    static RestService app = new RestService();

    private static File imageFile;
    private static final String IMAGE = "image";

    private static byte[] imageBytes;
    private static final String EXPECTED_XML_PAYLOAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            " <cityListDTO> <city> <name>Paris</name>" +
            " <country>France</country>" +
            " </city> </cityListDTO>";

    private final static String EXPECTED_RESPONSE_BODY_YAML = """
            [{description=Send a request with a YAML payload to the endpoint, including cities data, steps=[{action=POST, endpoint=/media-type/yaml, requestHeaders={Content-Type=application/yaml}, requestBody={cities=[{name=Tokio, country=Japan}]}}], expectedResponse={status=200, headers={Content-Type=application/json}, body={cities=[{name=Tokio, country=Japan}]}}}]""";

    @Test
    public void testExpectedXmlResponse() {
        app.given()
                .get("/city").then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(MediaType.APPLICATION_XML)
                .body("cityListWrapper.cities[0].name", equalTo("San Bernardino"))
                .body("cityListWrapper.cities[0].country", equalTo("EEUU"));

    }

    @Test
    public void testXMLPayloadPostRequest() throws JAXBException {

        City city = new City("Paris", "France");
        List<City> cityList = new ArrayList<>();
        cityList.add(city);

        CityListDTO requestCityList = new CityListDTO(cityList);

        String payload = app.given().contentType(MediaType.APPLICATION_XML)
                .body(requestCityList)
                .when()
                .post("/city")
                .then()
                .extract().asString();
        Document doc = Jsoup.parse(payload);
        String unescapedXml = doc.text();
        assertThat(unescapedXml, equalTo(EXPECTED_XML_PAYLOAD));
    }

    @Test
    public void testExpectedYamlResponse() {

        final Response response = app.given()
                .contentType("application/yaml")
                .get("/city/getYamlFile")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().response();

        assertThat(response.asString(), containsStringIgnoringCase(EXPECTED_RESPONSE_BODY_YAML));

    }

    @Test
    public void testPostYamlPayloadRequest() throws IOException {

        String yamlString = readYamlFile("src/test/resources/payload.yaml");

        Response response = RestAssured
                .given()
                .config(RestAssured.config()
                        .encoderConfig(encoderConfig().encodeContentTypeAs("application/x-yaml", ContentType.TEXT)))
                .contentType("application/x-yaml")
                .body(yamlString)
                .when()
                .post("/city/cities")
                .then()
                .statusCode(200).extract().response();
        JsonPath jsonPath = response.jsonPath();

        List<Object> cities = jsonPath.getList("expectedResponse.body.cities");
        assertEquals("[{name=Tokio, country=Japan}]", cities.get(0).toString());
    }

    @Test
    public void testGetImage() throws IOException {
        imageBytes = IOUtils.toByteArray(NonJsonPayLoadIT.class.getResourceAsStream("/wolf_moon_howling.jpg"));

        byte[] receivedBytes = app.given()
                .contentType("image/jpg")
                .get("/city/wolf_moon_howling.jpg")
                .then()
                .extract().asByteArray();

        assertThat(receivedBytes, CoreMatchers.equalTo(imageBytes));
    }

    @Test
    public void testImagePartFromMultipart() throws IOException {
        imageFile = new File(NonJsonPayLoadIT.class.getResource("/wolf_moon_howling.jpg").getFile());
        imageBytes = IOUtils.toByteArray(NonJsonPayLoadIT.class.getResourceAsStream("/wolf_moon_howling.jpg"));
        byte[] receivedBytes = postWithMultiPart("/city/image")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .extract().asByteArray();
        assertThat(receivedBytes, CoreMatchers.equalTo(imageBytes));
    }

    private static ValidatableResponse postWithMultiPart(String path) {
        MultiPartSpecification imageSpec = createImageSpec();

        return RestAssured.given()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPart(imageSpec)
                .post(path)
                .then()
                .statusCode(200);
    }

    private static MultiPartSpecification createImageSpec() {
        return new MultiPartSpecBuilder(imageFile)
                .controlName(IMAGE)
                .mimeType("image/png")
                .build();
    }

    private String readYamlFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return new String(Files.readAllBytes(path));
    }

}
