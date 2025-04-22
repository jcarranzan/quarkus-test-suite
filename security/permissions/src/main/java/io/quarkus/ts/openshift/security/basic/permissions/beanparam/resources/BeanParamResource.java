package io.quarkus.ts.openshift.security.basic.permissions.beanparam.resources;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.CommonFieldBeanParam;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.RecordBeanParam;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.SimpleBeanParam;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions.CommonFieldPermission;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions.RecordBeanParamPermission;
import io.quarkus.ts.openshift.security.basic.permissions.beanparam.permissions.SimpleBeanParamPermission;

@Path("/bean-param")
public class BeanParamResource {
    @GET
    @Path("/simple")
    @PermissionsAllowed(value = "read:basic", permission = SimpleBeanParamPermission.class, params = {
            "beanParam.resourceId",
            "beanParam.customAuthorizationHeader",
            "beanParam.action",
            "beanParam.principalName"
    })
    public String simpleAccess(@BeanParam SimpleBeanParam beanParam) {
        return "Simple access granted to resource " + beanParam.resourceId +
                " from path " + beanParam.uriInfo.getPath();
    }

    @GET
    @Path("/record")
    @PermissionsAllowed(value = "read:detailed", permission = RecordBeanParamPermission.class, params = {
            "recordParam.documentId",
            "recordParam.customAuthorizationHeader",
            "recordParam.accessLevel",
            "recordParam.principalName"
    })
    public String recordAccess(@BeanParam RecordBeanParam recordParam) {
        String path = recordParam.uriInfo() != null ? recordParam.uriInfo().getPath() : "/bean-param/record";

        return "Record access granted to document " + recordParam.documentId() +
                " from path " + path;
    }

    @GET
    @Path("/common-field")
    @PermissionsAllowed(value = "read:basic", permission = CommonFieldPermission.class, params = {
            "commonFieldBeanParam.customAuthorizationHeader",
            "commonFieldBeanParam.principalName"
    })
    public String commonField(@BeanParam CommonFieldBeanParam commonFieldBeanParam) {
        return "Common field access granted for operation " + commonFieldBeanParam.operation +
                " from path " + commonFieldBeanParam.uriInfo.getPath();
    }

    @POST
    @Path("/write")
    @Consumes(MediaType.TEXT_PLAIN)
    @PermissionsAllowed(value = "write:any", permission = SimpleBeanParamPermission.class, params = {
            "beanParam.resourceId",
            "beanParam.customAuthorizationHeader",
            "beanParam.action",
            "beanParam.principalName"
    })
    public String writeAccess(@BeanParam SimpleBeanParam beanParam, String content) {
        return "Write successful to resource " + beanParam.resourceId +
                " from path " + beanParam.uriInfo.getPath();
    }
}
