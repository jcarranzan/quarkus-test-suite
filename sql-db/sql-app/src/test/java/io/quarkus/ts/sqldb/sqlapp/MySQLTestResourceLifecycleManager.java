package io.quarkus.ts.sqldb.sqlapp;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.MySQLContainer;

import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MySQLTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = Logger.getLogger(MySQLTestResourceLifecycleManager.class);

    private static final String MYSQL_IMAGE = System.getProperty("mysql.80.image", "registry.access.redhat.com/rhscl/mysql-80-rhel7:latest");

    private static final MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE);

    @Override
    public Map<String, String> start() {
        LOGGER.info("Starting database container");
        mysql.start();
        await().pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            LOGGER.info("Waiting for database to start");
            mysql.isRunning();
        });
        Log.warn("jdbc url: " + mysql.getJdbcUrl());
        return Map.of(
                "properties.db.url", mysql.getJdbcUrl(),
                "properties.db.user", mysql.getUsername(),
                "properties.db.password", mysql.getPassword());
    }

    @Override
    public void stop() {
        if (mysql.isRunning()) {
            mysql.stop();
        }
    }
}
