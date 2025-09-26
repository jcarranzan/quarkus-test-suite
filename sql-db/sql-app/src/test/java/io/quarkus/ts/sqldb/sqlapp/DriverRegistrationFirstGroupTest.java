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

@DisabledOnNative
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Testcontainers requires a supported container runtime environment")
@Tag("QUARKUS-47552")
@QuarkusTest
@TestProfile(FirstGroupProfile.class)
@WithTestResource(value = MySQLTestResourceLifecycleManager.class, scope = TestResourceScope.MATCHING_RESOURCES)
public class DriverRegistrationFirstGroupTest {

    @Test
    public void firstGroupVerifyDrivers() {
        String drivers = given()
                .when()
                .get("/drivers/list")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(drivers.contains("io.quarkus.ts.sqldb.sqlapp.driver.TestJdbcDriver"),
                "Custom test driver should be registered in first group");
        assertTrue(drivers.contains("com.mysql.cj.jdbc.Driver"),
                "MySQL driver should be registered");
    }

}
