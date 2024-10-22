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

    public static void printSystemMemoryInfo() {
        try {
            // JVM Memory Info
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            System.out.println("=== JVM Memory Info ===");
            System.out.println("Free Memory: " + freeMemory + " MB");
            System.out.println("Total Memory: " + totalMemory + " MB");
            System.out.println("Max Memory: " + maxMemory + " MB");

            // System Memory Info
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            long totalPhysicalMemorySize = osBean.getTotalPhysicalMemorySize() / (1024 * 1024);
            long freePhysicalMemorySize = osBean.getFreePhysicalMemorySize() / (1024 * 1024);
            System.out.println("=== System Memory Info ===");
            System.out.println("Total Physical Memory: " + totalPhysicalMemorySize + " MB");
            System.out.println("Free Physical Memory: " + freePhysicalMemorySize + " MB");

            // Disk Space Info
            File root = new File("/");
            long totalDiskSpace = root.getTotalSpace() / (1024 * 1024);
            long freeDiskSpace = root.getFreeSpace() / (1024 * 1024);
            System.out.println("=== Disk Space Info ===");
            System.out.println("Total Disk Space: " + totalDiskSpace + " MB");
            System.out.println("Free Disk Space: " + freeDiskSpace + " MB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @KeycloakContainer(command = { "start-dev", "--import-realm", "--hostname-strict=false" })
    static KeycloakService keycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM, DEFAULT_REALM_BASE_PATH)
            .withProperty("JAVA_OPTS_KC_HEAP", "-Xms512m -Xmx1g -XX:+PrintFlagsFinal");

    @QuarkusApplication(ssl = true, certificates = @Certificate(configureKeystore = true, configureHttpServer = true, useTlsRegistry = false))
    static RestService app = new RestService().withProperty("quarkus.oidc.auth-server-url", keycloak::getRealmUrl);

    @Override
    protected RestService getApp() {
        return app;
    }
}
