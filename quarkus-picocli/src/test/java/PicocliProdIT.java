import static java.util.concurrent.CompletableFuture.runAsync;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.qe.configuration.Config;
import io.quarkus.ts.qe.configuration.CustomConfiguration;

@QuarkusScenario
public class PicocliProdIT {

    @QuarkusApplication(classes = { Config.class, CustomConfiguration.class })
    static final RestService customized = new RestService()
            .withProperty("quarkus.args", "customized-config")
            .withProperty("quarkus.profile", "prod")
            .setAutoStart(false);

    @Test
    public void verifyCustomizedCommandLineBehavior() throws InterruptedException {
        String expectedOutput = "customized-config";
        try {
            runAsync(customized::start);
            Thread.sleep(3500);
            customized.logs().assertContains(expectedOutput);
        } finally {
            customized.stop();
        }
    }
}
