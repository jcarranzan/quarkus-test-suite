package io.quarkus.ts.sqldb.sqlapp;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.Vertx;

@Path("/drivers")
public class DriverRegistrationResource {

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.username")
    String username;

    @ConfigProperty(name = "quarkus.datasource.password")
    String password;

    @Inject
    Vertx vertx;

    @GET
    @Path("/list")
    @Produces(MediaType.TEXT_PLAIN)
    public String listDrivers() {
        List<String> drivers = new ArrayList<>();
        Enumeration<Driver> driverEnum = DriverManager.getDrivers();

        while (driverEnum.hasMoreElements()) {
            Driver driver = driverEnum.nextElement();
            drivers.add(driver.getClass().getName());
        }
        return String.join("\n", drivers);
    }

}
