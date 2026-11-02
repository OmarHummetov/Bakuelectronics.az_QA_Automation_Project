package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class AuthenticatedLandingWaitTest {
    @Test public void waitsForRedirectAndAuthenticatedProfileWithoutLeakingValues() {
        var polls = new AtomicInteger();
        List<String> events = new ArrayList<>();
        WebDriver driver = driver(() -> polls.getAndIncrement() == 0
                ? "https://sso.redid.az/auth?token=synthetic-private"
                : ConfigReader.getUiBaseUrl() + "/istifadeci?code=synthetic-private",
                () -> polls.get() >= 3, () -> false);
        new LoginPage(driver).waitForAuthenticatedLanding(Duration.ofSeconds(3), events::add);
        Assert.assertEquals(events, List.of(
                "landing: configuredSite=false, authenticatedProfile=false, anonymousAccountLink=false",
                "landing: configuredSite=true, authenticatedProfile=false, anonymousAccountLink=false",
                "landing: configuredSite=true, authenticatedProfile=true, anonymousAccountLink=false"));
        Assert.assertFalse(events.toString().contains("synthetic-private"));
    }

    @Test public void profileCannotAuthorizeAnUnexpectedOrigin() {
        assertRejected("https://example.invalid/?token=synthetic-private", true, false);
        assertRejected(ConfigReader.getUiBaseUrl() + ".example.invalid/", true, false);
    }

    @Test public void expectedUrlCannotAuthorizeAnAnonymousPage() {
        assertRejected(ConfigReader.getUiBaseUrl() + "/", false, true);
        assertRejected(ConfigReader.getUiBaseUrl() + "/", true, true);
    }

    private void assertRejected(String url, boolean profile, boolean anonymous) {
        Assert.expectThrows(TimeoutException.class, () -> new LoginPage(driver(
                () -> url, () -> profile, () -> anonymous))
                .waitForAuthenticatedLanding(Duration.ofMillis(1), ignored -> { }));
    }

    private WebDriver driver(Supplier<String> url, BooleanSupplier profile, BooleanSupplier anonymous) {
        WebElement visible = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isDisplayed", "isEnabled" -> true;
                    default -> throw new AssertionError("Unexpected element operation");
                });
        return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCurrentUrl" -> url.get();
                    case "findElements" -> {
                        boolean present;
                        if (args[0].equals(By.cssSelector("button.header_UserIcon__gOK4_")))
                            present = profile.getAsBoolean();
                        else if (args[0].equals(By.cssSelector("a[href^='https://sso.redid.az/auth/']")))
                            present = anonymous.getAsBoolean();
                        else throw new AssertionError("Unexpected locator");
                        yield present ? List.of(visible) : List.of();
                    }
                    default -> throw new AssertionError("Unexpected browser operation");
                });
    }
}
