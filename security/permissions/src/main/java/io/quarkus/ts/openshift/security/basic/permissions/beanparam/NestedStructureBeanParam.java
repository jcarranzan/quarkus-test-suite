package io.quarkus.ts.openshift.security.basic.permissions.beanparam;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

public class NestedStructureBeanParam {
    @QueryParam("userId")
    private String userId;

    @QueryParam("userName")
    private String userName;

    @QueryParam("resourceId")
    private String resourceId;

    @QueryParam("resourceType")
    private String resourceType;

    private User user;
    private Resource resource;

    @Context
    private SecurityContext securityContext;

    @Context
    public UriInfo uriInfo;

    private User getOrCreateUser() {
        if (user == null) {
            user = new User();
            if (userId != null) {
                user.id = userId;
            }
            if (userName != null) {
                user.name = userName;
            }
        }
        return user;
    }

    private Resource getOrCreateResource() {
        if (resource == null) {
            resource = new Resource();
            resource.details = new ResourceDetails();
            if (resourceId != null) {
                resource.details.id = resourceId;
            }
            if (resourceType != null) {
                resource.details.type = resourceType;
            }
        }
        return resource;
    }

    public User getUser() {
        return getOrCreateUser();
    }

    public Resource getResource() {
        return getOrCreateResource();
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    public String getId() {
        return userId;
    }

    public String getName() {
        return userName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getPrincipalName() {
        return securityContext != null && securityContext.getUserPrincipal() != null
                ? securityContext.getUserPrincipal().getName()
                : null;
    }

    public static class User {
        public String id;
        public String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Resource {
        public ResourceDetails details;

        public ResourceDetails getDetails() {
            return details;
        }
    }

    public static class ResourceDetails {
        public String id;
        public String type;

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }
}