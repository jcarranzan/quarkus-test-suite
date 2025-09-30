package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

@Path("/admin-client")
public class AdminClientResource {

    private static final Logger LOG = Logger.getLogger(AdminClientResource.class);

    public static final String ACR_LOA_MAP = "acr.loa.map";
    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "test-application-client";

    @Inject
    Keycloak keycloak;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("configure-stepup")
    public Response configureStepUpRealm() {
        try {
            LOG.info("Configuring realm: " + REALM_NAME);

            RealmResource realmResource = keycloak.realm(REALM_NAME);
            RealmRepresentation realm = realmResource.toRepresentation();

            Map<String, Integer> acrLoaMap = new HashMap<>();
            acrLoaMap.put("copper", 0);
            acrLoaMap.put("silver", 1);
            acrLoaMap.put("gold", 2);

            if (realm.getAttributes() == null) {
                realm.setAttributes(new HashMap<>());
            }
            realm.getAttributes().put(ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
            realm.setOtpPolicyCodeReusable(true);

            realmResource.update(realm);

            createTestUsers(realmResource);

            ClientsResource clientsResource = realmResource.clients();
            ClientRepresentation client = clientsResource.findByClientId(CLIENT_ID)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (client != null) {
                if (client.getAttributes() == null) {
                    client.setAttributes(new HashMap<>());
                }
                client.getAttributes().put(ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));

                ProtocolMapperRepresentation acrMapper = new ProtocolMapperRepresentation();
                acrMapper.setName("acr-mapper");
                acrMapper.setProtocol("openid-connect");
                acrMapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
                Map<String, String> acrConfig = new HashMap<>();
                acrConfig.put("user.attribute", "acr_level");
                acrConfig.put("claim.name", "acr");
                acrConfig.put("jsonType.label", "String");
                acrConfig.put("id.token.claim", "true");
                acrConfig.put("access.token.claim", "true");
                acrConfig.put("userinfo.token.claim", "false");
                acrMapper.setConfig(acrConfig);

                ProtocolMapperRepresentation rolesMapper = new ProtocolMapperRepresentation();
                rolesMapper.setName("realm-roles-mapper");
                rolesMapper.setProtocol("openid-connect");
                rolesMapper.setProtocolMapper("oidc-usermodel-realm-role-mapper");
                Map<String, String> rolesConfig = new HashMap<>();
                rolesConfig.put("claim.name", "realm_access.roles");
                rolesConfig.put("jsonType.label", "String");
                rolesConfig.put("multivalued", "true");
                rolesConfig.put("access.token.claim", "true");
                rolesConfig.put("id.token.claim", "true");
                rolesMapper.setConfig(rolesConfig);

                client.setProtocolMappers(Arrays.asList(acrMapper, rolesMapper));

                clientsResource.get(client.getId()).update(client);
                LOG.info("Client updated with ACR and roles mappers");
            }

            return Response.ok("Realm configured for Step-Up Authentication").build();

        } catch (Exception e) {
            LOG.error("Error configuring realm: ", e);
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    private void createTestUsers(RealmResource realmResource) {
        UsersResource usersResource = realmResource.users();
        // Let's create realm roles first to avoid issues
        createRealmRolesIfNotExist(realmResource);

        createUser(realmResource, usersResource, "test-user-copper", "test-user-copper",
                Arrays.asList("user"), null); // User without ACR
        createUser(realmResource, usersResource, "test-user-silver", "test-user-silver",
                Arrays.asList("user"), "silver");
        createUser(realmResource, usersResource, "test-user-gold", "test-user-gold",
                Arrays.asList("user", "admin"), "gold");
        LOG.info("✓ Test users created: copper (no ACR), silver (ACR), gold (ACR + admin)");
    }

    private void createRealmRolesIfNotExist(RealmResource realmResource) {
        String[] roles = { "user", "admin" };

        for (String roleName : roles) {
            try {
                realmResource.roles().get(roleName).toRepresentation();
                LOG.info("Role " + roleName + " already exists");
            } catch (Exception e) {
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setDescription("Role " + roleName);
                realmResource.roles().create(role);
                LOG.info("Created realm role: " + roleName);
            }
        }
    }

    private void createUser(RealmResource realmResource, UsersResource usersResource,
            String username, String password, List<String> realmRoles, String acrLevel) {

        List<UserRepresentation> existingUsers = usersResource.search(username);
        if (!existingUsers.isEmpty()) {
            LOG.info("Deleting existing user: " + username);
            usersResource.delete(existingUsers.get(0).getId());
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEmail(username + "@test.com");
        user.setFirstName(username);
        user.setLastName("Test");

        if (acrLevel != null) {
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("acr_level", Arrays.asList(acrLevel));
            user.setAttributes(attributes);
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Arrays.asList(credential));

        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            String location = response.getLocation().getPath();
            String userId = location.substring(location.lastIndexOf('/') + 1);

            for (String roleName : realmRoles) {
                try {
                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    usersResource.get(userId).roles().realmLevel().add(Arrays.asList(role));
                } catch (Exception e) {
                    LOG.error("Failed to assign role " + roleName + " to user " + username, e);
                }
            }

            String acrInfo = acrLevel != null ? " with ACR: " + acrLevel : " without ACR";
            LOG.info("✓ Created user: " + username + acrInfo + ", roles: " + realmRoles);
            response.close();
        } else {
            String errorBody = response.readEntity(String.class);
            LOG.error("❌ Failed to create user " + username + ": " + response.getStatus() + " - " + errorBody);
            response.close();
        }
    }
}