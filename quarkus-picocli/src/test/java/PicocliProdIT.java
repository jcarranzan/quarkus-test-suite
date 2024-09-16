import static java.util.concurrent.CompletableFuture.runAsync;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@Disabled("Requires 'prod' profile. Disabled because the test fails when the application is built and run with the default 'dev' profile. Enable by building with '-Dquarkus.profile=prod'.")
@QuarkusScenario
public class PicocliProdIT {
    @QuarkusApplication
    static final RestService customized = new RestService()
            .withProperty("quarkus.profile", "prod")
            .withProperty("quarkus.args", "start -t 60 -v")
            .setAutoStart(false);

    @Test
    public void verifyCustomizedCommandLineBehavior() {
        String expectedOutput = "Service started with timeout: 60 and verbosity";
        try {
            runAsync(customized::start);
            customized.logs().assertContains(expectedOutput);
        } finally {
            customized.stop();
        }
    }
}
