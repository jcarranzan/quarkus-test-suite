package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;

/**
 * Custom Jose4J validator for ACR values enforcement using Step-Up Authentication protocol.
 * This validator requires both "gold" and "platinum" ACR values to be present in the token.
 * If validation fails, it throws AuthenticationFailedException with ACR_VALUES attribute,
 * which triggers the step-up authentication challenge response.
 */
@Unremovable
@ApplicationScoped
@TenantFeature("custom-validator")
public class CustomAcrValidator implements Validator {

    private static final String REQUIRED_ACR_GOLD = "gold";
    private static final String REQUIRED_ACR_PLATINUM = "platinum";

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        var jwtClaims = jwtContext.getJwtClaims();

        if (!jwtClaims.hasClaim(Claims.acr.name())) {
            throw new AuthenticationFailedException(
                    "Token missing ACR claim",
                    Map.of(OidcConstants.ACR_VALUES, REQUIRED_ACR_GOLD + "," + REQUIRED_ACR_PLATINUM));
        }

        var acrClaim = jwtClaims.getStringListClaimValue(Claims.acr.name());
        if (acrClaim.contains(REQUIRED_ACR_GOLD) && acrClaim.contains(REQUIRED_ACR_PLATINUM)) {
            // Validation passed
            return null;
        }

        // Validation failed - trigger step-up authentication
        throw new AuthenticationFailedException(
                "Token does not contain required ACR values: gold and platinum",
                Map.of(OidcConstants.ACR_VALUES, REQUIRED_ACR_GOLD + "," + REQUIRED_ACR_PLATINUM));
    }
}
