package io.quarkus.ts.sqldb.sqlapp;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.restassured.response.Response;

@DisabledOnNative
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Testcontainers requires a supported container runtime environment")
@Tag("QUARKUS-47552")
@QuarkusTest
@TestProfile(SecondGroupProfile.class)
@WithTestResource(value = MySQLTestResourceLifecycleManager.class, scope = TestResourceScope.RESTRICTED_TO_CLASS)
public class DriverRegistrationSecondGroupTest {

    @Test
    public void verifyDriversStillRegistered() {
        Response response = given()
                .when()
                .get("/drivers/list")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String drivers = response.asString();
        assertTrue(drivers.contains("io.quarkus.ts.sqldb.sqlapp.driver.TestJdbcDriver"),
                "Custom test driver should still be registered in second group");
        assertTrue(drivers.contains("com.mysql.cj.jdbc.Driver"),
                "MySQL driver should still be registered");
    }

}
