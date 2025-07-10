package io.quarkus.ts.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.scenarios.QuarkusScenario;

@Tag("https://github.com/quarkusio/quarkus/pull/48057/")
@QuarkusScenario
@TestProfile(MdcTestProfile.class)
public class MdcContextPropagationIT {

    @BeforeEach
    public void clearLogs() {
        InMemoryLogHandler.reset();
    }

    @RepeatedTest(5)
    void testMdcIsPropagatedFromExternalEndpointToHealthCheck() {
        given()
                .when()
                .get("/external-health")
                .then()
                .statusCode(200);

        List<String> records = given().when().get("/external-health/log-records")
                .then().statusCode(200).extract().body().jsonPath().getList(".", String.class);

        System.out.println("=== Captured records === " + records.size());
        records.forEach(record -> System.out.println("Record: " + record));
        System.out.println("=== End of records ===");

        assertThat("Should have captured some log records", records.isEmpty(), is(false));

        boolean matchFound = records.stream()
                .anyMatch(record -> record.contains("Executing MdcPropagationHealthCheck") &&
                        record.contains("endpoint_context=value-from-endpoint"));

        assertThat(
                "Captured logs should contain a record from MdcPropagationHealthCheck with the propagated context",
                matchFound,
                is(true));
    }
}
