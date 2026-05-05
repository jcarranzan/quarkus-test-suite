package io.quarkus.ts.sqldb.sqlapp;

import java.sql.Connection;

import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import javax.sql.DataSource;

@Path("/oracle-rollback")
public class OracleRollbackResource {

    private static final Logger LOG = Logger.getLogger(OracleRollbackResource.class);
    public static final String INSERT_EXECUTED_LOG = "INSERT executed, blocking to simulate long-running operation";
    private static final String CREATE_TABLE = "CREATE TABLE rollback_test ("
            + "id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "value VARCHAR2(255) NOT NULL)";
    private static final String DROP_TABLE = "DROP TABLE rollback_test";
    private static final String INSERT = "INSERT INTO rollback_test (value) VALUES ('should-be-rolled-back')";

    @Inject
    DataSource dataSource;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    UserTransaction userTransaction;

    @POST
    @Path("/init")
    public Response init() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.createStatement().execute(DROP_TABLE);
            } catch (Exception ignored) {
                // table may not exist yet
            }
            conn.createStatement().execute(CREATE_TABLE);
        }
        return Response.ok("initialized").build();
    }

    @POST
    @Path("/trigger")
    public Response trigger() {
        managedExecutor.execute(this::longRunningTransaction);
        return Response.accepted().build();
    }

    void longRunningTransaction() {
        try {
            userTransaction.begin();
            try (Connection conn = dataSource.getConnection()) {
                conn.prepareStatement(INSERT).executeUpdate();
                LOG.info(INSERT_EXECUTED_LOG);
                Thread.sleep(30_000);
            }
            userTransaction.commit();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // expected during shutdown
        }
    }
}
