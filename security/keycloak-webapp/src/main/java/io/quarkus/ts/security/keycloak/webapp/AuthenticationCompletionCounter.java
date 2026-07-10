package io.quarkus.ts.security.keycloak.webapp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import io.quarkus.arc.Unremovable;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.oidc.AuthenticationCompletionAction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
@Unremovable
public class AuthenticationCompletionCounter implements AuthenticationCompletionAction {

    @Inject
    EntityManager entityManager;

    private volatile boolean shouldFail;

    @Override
    public Uni<Void> action(AuthenticationCompletionContext authCompletionContext) {
        if (shouldFail) {
            return Uni.createFrom().failure(new RuntimeException("Forced authentication completion failure"));
        }
        String principal = authCompletionContext.identity().getPrincipal().getName();
        return Uni.createFrom().voidItem()
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .invoke(() -> {
                    QuarkusTransaction.requiringNew().run(() -> {
                        AuthCompletionLog log = new AuthCompletionLog();
                        log.principalName = principal;
                        entityManager.persist(log);
                    });
                });
    }

    void enableFailure() {
        shouldFail = true;
    }

    void resetFailure() {
        shouldFail = false;
    }
}
