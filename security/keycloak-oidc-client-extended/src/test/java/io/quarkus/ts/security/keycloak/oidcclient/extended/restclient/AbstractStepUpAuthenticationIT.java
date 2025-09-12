package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static org.hamcrest.Matchers.is;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.smallrye.jwt.build.Jwt;

@QuarkusScenario
public abstract class AbstractStepUpAuthenticationIT {

    @LookupService
    static KeycloakService keycloak;

    @QuarkusApplication(properties = "stepup.properties")
    static RestService app = new RestService()
            .withProperty("quarkus.oidc.public-key ", AbstractStepUpAuthenticationIT::getPublicKeyInPemFormat)
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl())
            .withProperties(() -> keycloak.getTlsProperties());

    private static String getPublicKeyInPemFormat() {
        PublicKey publicKey = new StepUpAuthenticationIT().getPublicKey();
        String pem = "-----BEGIN PUBLIC KEY-----\n";
        pem += Base64.getMimeEncoder().encodeToString(publicKey.getEncoded());
        pem += "\n-----END PUBLIC KEY-----\n";
        return pem;
    }

    @Test
    public void testSingleAcrSuccess() {
        String tokenWithAcr = createTokenWithAcr();

        app.given()
                .auth().oauth2(tokenWithAcr)
                .when()
                .get("/step-up/single-acr")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Single ACR validated"));
    }

    protected String createTokenWithAcr() {
        return Jwt.preferredUserName("alice")
                .issuer(keycloak.getRealmUrl())
                .audience("quarkus-web-app")
                .claim("acr", "acr-level-1")
                .claim("scope", "openid profile")
                .jws()
                .keyId("test-key")
                .sign(getPrivateKey());
    }

    protected abstract PrivateKey getPrivateKey();

    protected abstract PublicKey getPublicKey();

}
