package io.quarkus.ts.openshift.security.basic.beanparam;

import static org.hamcrest.Matchers.containsString;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.specification.RequestSpecification;

@QuarkusScenario
public class JwtBeanParamPermissionIT extends BaseBeanParamPermissionsIT {

    @QuarkusApplication
    static RestService app = new RestService();

    @Override
    protected RequestSpecification givenAuthenticatedUser(String role) {
        String token = JwtTokenGenerator.generateToken(role, role);
        return app.given().header("Authorization", "Bearer " + token);
    }

    @Test
    public void testSimpleBeanParamWithJwtAdmin() {
        givenAuthenticatedUser(ADMIN)
                .header("CustomAuthorization", "valid-token")
                .queryParam("resourceId", "res-123")
                .queryParam("action", "basic")
                .when()
                .get(SIMPLE_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Simple access granted to resource res-123"));
    }
}
