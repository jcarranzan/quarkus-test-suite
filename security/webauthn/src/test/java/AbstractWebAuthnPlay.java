import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import io.quarkus.test.bootstrap.RestService;

public abstract class AbstractWebAuthnPlay {

    protected abstract RestService getApp();

    @Test
    public void testWithPlayWright() {

        String enableWebAuthnScript = "const devTools = await page.context().newCDPSession(page);\n" +
                "  await devTools.send('WebAuthn.enable');\n" +
                "  await devTools.send('WebAuthn.addVirtualAuthenticator', {\n" +
                "    options: {\n" +
                "      protocol: 'ctap2',\n" +
                "      transport: 'internal',\n" +
                "      hasUserVerification: true,\n" +
                "      isUserVerified: true,\n" +
                "      hasResidentKey: true,\n" +
                "    },\n" +
                "  });";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false));
            Page page = browser.newPage();
            page.navigate("http://localhost:1101");

            page.evaluate(enableWebAuthnScript);

            // Expect a title "to contain" a substring.
            assertThat(page).hasTitle(Pattern.compile("Login"));

            // create  locators

            Locator loginButton = page.locator("#login");

            Locator registerButton = page.locator("#register");

            Locator usernameLoginInput = page.locator("#userNameLogin");

            Locator usernameRegisterInput = page.locator("#userNameRegister");

            Locator firstNameInput = page.locator("#firstName");

            Locator lastNameInput = page.locator("#lastName");

            usernameRegisterInput.fill("Roosvelt");
            firstNameInput.fill("Franklin");
            lastNameInput.fill("Roosvelt");

            // Expect an attribute "to be strictly equal" to the value.

            // Click the get started link.
            registerButton.click();

            page.pause();
            // Expects page to have a heading with the name of Installation.

        }

    }

}
