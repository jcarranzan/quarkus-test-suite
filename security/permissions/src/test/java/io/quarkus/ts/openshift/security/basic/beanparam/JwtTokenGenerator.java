package io.quarkus.ts.openshift.security.basic.beanparam;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;

public class JwtTokenGenerator {
    /**
     * Generates a JWT token for a user with the specified roles.
     *
     * @param username the user name
     * @param roles the roles to include in the token
     * @return the signed JWT token
     */
    public static String generateToken(String username, String... roles) {
        Set<String> roleSet = new HashSet<>(Arrays.asList(roles));
        return Jwt.issuer("https://my.auth.server/")
                .upn(username)
                .subject(username)
                .groups(roleSet)
                .expiresAt(System.currentTimeMillis() + 3600 * 1000)
                .sign();
    }
}
