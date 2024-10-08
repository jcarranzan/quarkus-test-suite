package io.quarkus.ts.transactions;

import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
public class OpenShiftPostgresqlTransactionGeneralUsageIT extends AbstractPostgresqlTransactionGeneralUsageIT {

    @Container(image = "${postgresql.latest.image}", port = POSTGRESQL_PORT, expectedLog = "listening on IPv4 address")
    static final PostgresqlService database = new PostgresqlService()
            // following env variable is accepted by PG images from Red Hat registry
            .withProperty("POSTGRESQL_MAX_PREPARED_TRANSACTIONS", "100")
            .withProperty("PGDATA", "/tmp/psql");

    @QuarkusApplication
    public static final RestService app = createQuarkusApp(database);

    @Override
    protected RestService getApp() {
        return app;
    }

}
