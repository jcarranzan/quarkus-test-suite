package io.quarkus.ts.openshift.security.basic.permissions.beanparam.resources;

import java.util.function.Supplier;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.NestedStructureBeanParam;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions.NestedBeanParamPermission;

@Path("/bean-param/nested-structure")
@Produces(MediaType.TEXT_PLAIN)
public class NestedStructureResource {
    /**
     * Endpoint access to nested fields for documents.
     * Requires a valid username to access documents.
     */
    @GET
    @Path("/document")
    @PermissionsAllowed(value = "read:nested", permission = NestedBeanParamPermission.class, params = {
            "nestedParam.id",
            "nestedParam.name",
            "nestedParam.resourceId",
            "nestedParam.resourceType",
            "nestedParam.principalName"
    }

    )
    public String accessNestedDocumentsStructure(@BeanParam NestedStructureBeanParam nestedParam) {
        String userId = getValueCheckingNulls(() -> nestedParam.getUser().getId());
        String userName = getValueCheckingNulls(() -> nestedParam.getUser().getName());
        String resourceId = getValueCheckingNulls(() -> nestedParam.getResource().getDetails().getId());
        String resourceType = getValueCheckingNulls(() -> nestedParam.getResource().getDetails().getType());

        return String.format("Access granted to the document, User  %s ID: %s Resource %s (Type %s)",
                userName, userId, resourceId, resourceType);
    }

    /**
     * Endpoint to nested fields for profiles.
     * Requires user ID matches resource ID.
     */
    @GET
    @Path("/profile")
    @PermissionsAllowed(value = "read:profile", permission = NestedBeanParamPermission.class, params = {
            "nestedParam.id",
            "nestedParam.name",
            "nestedParam.resourceId",
            "nestedParam.resourceType",
            "nestedParam.principalName"
    })
    public String accessNestedProfileStructure(@BeanParam NestedStructureBeanParam nestedParam) {
        String userId = getValueCheckingNulls(() -> nestedParam.getUser().getId());
        String userName = getValueCheckingNulls(() -> nestedParam.getUser().getName());
        String resourceId = getValueCheckingNulls(() -> nestedParam.getResource().details.id);

        return String.format("Access granted to profile. User: %s (ID: %s), Profile ID: %s",
                userName, userId, resourceId);
    }

    private <T> String getValueCheckingNulls(Supplier<T> supplier) {
        try {
            T value = supplier.get();
            return value != null ? value.toString() : "null";
        } catch (NullPointerException e) {
            return "NPE " + e.getMessage() + "in getValueCheckingNulls";
        }
    }
}
