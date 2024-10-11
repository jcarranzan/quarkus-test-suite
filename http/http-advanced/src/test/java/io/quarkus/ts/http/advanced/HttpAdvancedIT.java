package io.quarkus.ts.http.advanced;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;

import java.io.File;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Certificate;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class HttpAdvancedIT extends BaseHttpAdvancedIT {

    static {
        printSystemMemoryInfo();
    }

    private static void printSystemMemoryInfo() {
        try {
            // Get the OperatingSystemMXBean instance
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

            // Get total and free physical memory
            long totalPhysicalMemorySize = osBean.getTotalPhysicalMemorySize();
            long freePhysicalMemorySize = osBean.getFreePhysicalMemorySize();

            // Convert bytes to megabytes
            long totalPhysicalMemorySizeMB = totalPhysicalMemorySize / (1024 * 1024);
            long freePhysicalMemorySizeMB = freePhysicalMemorySize / (1024 * 1024);

            System.out.println("=== System Memory Info ===");
            System.out.println("Total Physical Memory: " + totalPhysicalMemorySizeMB + " MB");
            System.out.println("Free Physical Memory: " + freePhysicalMemorySizeMB + " MB");

            // Get total and free disk space
            File root = new File("/");
            long totalDiskSpace = root.getTotalSpace();
            long freeDiskSpace = root.getFreeSpace();

            long totalDiskSpaceMB = totalDiskSpace / (1024 * 1024);
            long freeDiskSpaceMB = freeDiskSpace / (1024 * 1024);

            System.out.println("=== Disk Space Info ===");
            System.out.println("Total Disk Space: " + totalDiskSpaceMB + " MB");
            System.out.println("Free Disk Space: " + freeDiskSpaceMB + " MB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @KeycloakContainer(command = { "start-dev", "--import-realm", "--hostname-strict=false" }, memoryLimitMiB = 512)
    static KeycloakService keycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM, DEFAULT_REALM_BASE_PATH);

    @QuarkusApplication(ssl = true, certificates = @Certificate(configureKeystore = true, configureHttpServer = true, useTlsRegistry = false))
    static RestService app = new RestService().withProperty("quarkus.oidc.auth-server-url", keycloak::getRealmUrl);

    @Override
    protected RestService getApp() {
        return app;
    }
}
