import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import io.quarkus.test.bootstrap.RestService;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.cookie.CookieFilter;

public abstract class AbstractWebAuthnSelenium {
    private static final Boolean DEFAULT_HEADLESS = false;

    protected abstract RestService getApp();

    private static WebDriver webDriver;

    private static WebDriverWait wait;

    private String userName = "Roosvelt";

    private static Filter cookieFilter;

    static VirtualAuthenticator authenticator;

    @BeforeAll
    public static void setup() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setHeadless(DEFAULT_HEADLESS);
        chromeOptions.addArguments("--remote-allow-origins=*");
        webDriver = new ChromeDriver(chromeOptions);
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        VirtualAuthenticatorOptions options = new VirtualAuthenticatorOptions()
                .setIsUserVerified(true)
                .setHasUserVerification(true)
                .setIsUserConsenting(true)
                .setTransport(VirtualAuthenticatorOptions.Transport.USB)
                .setProtocol(VirtualAuthenticatorOptions.Protocol.U2F)
                .setHasResidentKey(false);

        authenticator = ((HasVirtualAuthenticator) webDriver).addVirtualAuthenticator(options);

        cookieFilter = new CookieFilter();

    }

    @AfterAll
    public static void tearDown() {
        webDriver.close();
    }

    @Test
    @Order(1)
    public void registerAUser() {

        webDriver.get("http://localhost:1101");

        WebElement userNameInput = webDriver.findElement(By.id("userNameRegister"));
        WebElement firstNameInput = webDriver.findElement(By.id("firstName"));
        WebElement lastNameInput = webDriver.findElement(By.id("lastName"));

        //fill the inputs
        userNameInput.sendKeys(userName);
        firstNameInput.sendKeys(userName);
        lastNameInput.sendKeys(userName);

        // press register button
        webDriver.findElement(By.id("register")).click();

        verifyLoggedIn(cookieFilter, userName);
    }

    private void verifyLoggedIn(Filter cookieFilter, String userName) {

        // public API still good
        RestAssured.given().filter(cookieFilter)
                .get("http://localhost:1101/api/public")
                .then()
                .statusCode(200)
                .body(Matchers.is("public"));
        // public API user name
        RestAssured.given().filter(cookieFilter)
                .get("http://localhost:1101/api/public/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

        // user API accessible
        RestAssured.given().filter(cookieFilter)
                .get("http://localhost:1101/api/users/me")
                .then()
                .statusCode(200)
                .body(Matchers.is(userName));

    }

}
