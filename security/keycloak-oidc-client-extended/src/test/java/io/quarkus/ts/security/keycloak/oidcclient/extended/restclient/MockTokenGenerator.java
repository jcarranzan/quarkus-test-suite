package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Set;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;

import io.smallrye.jwt.build.Jwt;

public class MockTokenGenerator {

    private final String issuer;
    private final RsaJsonWebKey rsaJsonWebKey;

    public MockTokenGenerator(int wireMockPort) {
        // The issuer must be the same as 'quarkus.oidc.auth-server-url'
        this.issuer = "http://localhost:" + wireMockPort + "/auth/realms/test-realm";
        try {
            this.rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
            this.rsaJsonWebKey.setKeyId("test-key-id");
            this.rsaJsonWebKey.setAlgorithm("RS256");
            this.rsaJsonWebKey.setUse("sig");
        } catch (Exception e) {
            throw new RuntimeException("Error generating RSA", e);
        }
    }

    public String generateDiscoveryDocument() {
        String jwksUrl = this.issuer + "/protocol/openid-connect/certs";
        String tokenUrl = this.issuer + "/protocol/openid-connect/token";

        return String.format("""
                {
                    "issuer": "%s",
                    "jwks_uri": "%s",
                    "token_endpoint": "%s"
                }
                """, this.issuer, jwksUrl, tokenUrl);
    }

    public String generateJwks() {
        return String.format("{\"keys\":[%s]}", rsaJsonWebKey.toJson());
    }

    public String createBareboneToken() {
        long nowSeconds = System.currentTimeMillis() / 1000;

        return Jwt.issuer(issuer)
                .subject("test-user")
                .upn("test-user")
                .preferredUserName("test-user")
                .audience("test-client")
                .groups(Set.of("user"))
                .claim("typ", "Bearer")
                .issuedAt(nowSeconds)
                .expiresIn(500)
                .jws().keyId(rsaJsonWebKey.getKeyId())
                .sign((RSAPrivateKey) rsaJsonWebKey.getPrivateKey());
    }

    public String createMockedToken(Set<String> acrValues, Long authTimeSeconds, Set<String> groups) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        var builder = Jwt.issuer(issuer)
                .subject("test-user")
                .upn("test-user")
                .preferredUserName("test-user")
                .audience("test-client")
                .groups(groups != null ? groups : Set.of("user"))
                .claim("typ", "Bearer")
                .issuedAt(nowSeconds)
                .expiresAt(nowSeconds + 3600);

        if (acrValues != null && !acrValues.isEmpty()) {
            builder.claim("acr", new ArrayList<>(acrValues));
        }

        if (authTimeSeconds != null) {
            builder.claim("auth_time", authTimeSeconds);
        }

        return builder.jws().keyId(rsaJsonWebKey.getKeyId()).sign((RSAPrivateKey) rsaJsonWebKey.getPrivateKey());
    }

    public String createTokenWithSingleAcr(String acrValue) {
        return createMockedToken(Set.of(acrValue), null, Set.of("user"));
    }

    public String createTokenWithMultipleAcrs(String... acrValues) {
        return createMockedToken(Set.of(acrValues), null, Set.of("user"));
    }

    public String createTokenWithAcrAndMaxAge(String acr, long secondsInThePast) {
        long authTime = (System.currentTimeMillis() / 1000) - secondsInThePast;
        return createMockedToken(Set.of(acr), authTime, Set.of("user"));
    }

    public String createTokenWithoutAcr() {
        return createMockedToken(null, null, Set.of("user"));
    }
}
