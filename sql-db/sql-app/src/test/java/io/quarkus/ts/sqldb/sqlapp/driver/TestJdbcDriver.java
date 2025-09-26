package io.quarkus.ts.sqldb.sqlapp.driver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Test wrapper driver that demonstrates the ServiceLoader initialization issue.
 * This driver wraps the real MySQL driver to show that custom drivers loaded
 * via ServiceLoader get unregistered between test restarts.
 */
public class TestJdbcDriver implements Driver {

    private static final String URL_PREFIX = "jdbc:mysql:";
    private static boolean registered = false;
    private static Driver mysqlDriver;

    static {
        try {
            // Register this test driver
            DriverManager.registerDriver(new TestJdbcDriver());
            registered = true;
            System.out.println("TestJdbcDriver registered via static initializer");

            // Find the real MySQL driver to delegate to
            var drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver d = drivers.nextElement();
                if (d.getClass().getName().equals("com.mysql.cj.jdbc.Driver")) {
                    mysqlDriver = d;
                    break;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register TestJdbcDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        // Delegate to real MySQL driver
        if (mysqlDriver != null && acceptsURL(url)) {
            return mysqlDriver.connect(url, info);
        }
        return null;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        // Accept MySQL URLs
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (mysqlDriver != null) {
            return mysqlDriver.getPropertyInfo(url, info);
        }
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return mysqlDriver != null && mysqlDriver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        if (mysqlDriver != null) {
            return mysqlDriver.getParentLogger();
        }
        throw new SQLFeatureNotSupportedException();
    }

    public static boolean isRegistered() {
        return registered;
    }
}