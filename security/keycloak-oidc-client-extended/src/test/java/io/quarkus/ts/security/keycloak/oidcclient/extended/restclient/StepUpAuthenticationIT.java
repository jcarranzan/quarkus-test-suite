package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KeycloakContainer;

@QuarkusScenario
public class StepUpAuthenticationIT extends AbstractStepUpAuthenticationIT {

    @KeycloakContainer(runKeycloakInProdMode = true)
    static KeycloakService keycloak = new KeycloakService("test-realm-simple.json", DEFAULT_REALM, DEFAULT_REALM_BASE_PATH);
}
