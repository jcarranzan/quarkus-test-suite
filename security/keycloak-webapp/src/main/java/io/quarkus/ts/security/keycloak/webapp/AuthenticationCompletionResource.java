package io.quarkus.ts.security.keycloak.webapp;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth-completion")
public class AuthenticationCompletionResource {

    @Inject
    AuthenticationCompletionCounter action;

    @Inject
    Instance<AuthenticationCompletionSecondary> secondaryAction;

    @Inject
    EntityManager entityManager;

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String count() {
        Long result = entityManager.createQuery("SELECT COUNT(a) FROM AuthCompletionLog a", Long.class)
                .getSingleResult();
        return Long.toString(result);
    }

    @GET
    @Path("/principal")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String principal() {
        List<String> results = entityManager
                .createQuery("SELECT a.principalName FROM AuthCompletionLog a ORDER BY a.id DESC", String.class)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? "null" : results.get(0);
    }

    @GET
    @Path("/secondary-count")
    @Produces(MediaType.TEXT_PLAIN)
    public String secondaryCount() {
        return secondaryAction.isResolvable() ? Integer.toString(secondaryAction.get().getCallCount()) : "0";
    }

    @POST
    @Path("/enable-fail")
    @Produces(MediaType.TEXT_PLAIN)
    public String enableFail() {
        action.enableFailure();
        return "enabled";
    }

    @POST
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String reset() {
        action.resetFailure();
        if (secondaryAction.isResolvable()) {
            secondaryAction.get().reset();
        }
        entityManager.createQuery("DELETE FROM AuthCompletionLog").executeUpdate();
        return "reset";
    }
}
