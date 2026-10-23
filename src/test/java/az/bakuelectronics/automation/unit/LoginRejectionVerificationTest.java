package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.List;


public class LoginRejectionVerificationTest {
    private static final By ERROR = By.cssSelector("[role='alert']");

    @Test
    public void verifiedPasswordRejectionPassesWithBuiltInSelectorAndText() {
        LoginPage page = page("https://sso.redid.az/auth/offline", true, false,
                List.of(element(true, "Daxilolma uğursuz oldu. Məlumatlarınızı yoxlayın.")));
        page.assertIncorrectPasswordRejected();
        Assert.expectThrows(AssertionError.class, page::assertPasswordRejectionAbsent);
        page("https://sso.redid.az/auth/offline", true, false, List.of()).assertPasswordRejectionAbsent();
    }

    @Test
    public void diagnosticAssertionsUseTheSameReadOnlyVerification() {
        page("https://sso.redid.az/auth/offline", true, false,
                List.of(element(true, "Observed non-sensitive rejection")))
                .assertAuthenticationRejected(ERROR, "Observed non-sensitive rejection");
        Assert.expectThrows(AssertionError.class,
                () -> page("https://sso.redid.az/auth/offline", true, false, List.of())
                        .assertAuthenticationRejected(ERROR, " "));
    }

    @Test
    public void browserVerificationFailureSuppressesSensitiveDetailsAndCause() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    throw new TimeoutException("offline-sensitive-browser-details");
                });
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> new LoginPage(driver).assertIncorrectPasswordRejected());
        Assert.assertFalse(failure.getMessage().contains("offline-sensitive-browser-details"));
        Assert.assertNull(failure.getCause());
    }

    @Test
    public void acceptsOnlyOneVisibleErrorWithExactText() {
        LoginPage page = page("https://sso.redid.az/auth/offline", true, false,
                List.of(element(true, "Incorrect password")));
        Assert.assertTrue(page.isAuthenticationErrorVisible(ERROR));
        Assert.assertTrue(page.hasExactAuthenticationError(ERROR, "Incorrect password"));
        Assert.assertFalse(page.hasExactAuthenticationError(ERROR, "incorrect password"));
        Assert.assertFalse(page.hasExactAuthenticationError(ERROR, "Incorrect password "));
        Assert.assertTrue(page.isPasswordInputVisible());
        Assert.assertTrue(page.isUnauthenticatedPasswordStep());
    }

    @Test
    public void missingHiddenOrAmbiguousErrorsCannotPass() {
        for (List<WebElement> errors : List.of(List.<WebElement>of(),
                List.of(element(false, "Incorrect password")),
                List.of(element(true, "Incorrect password"), element(true, "Incorrect password")))) {
            Assert.assertFalse(page("https://sso.redid.az/auth/offline", true, false, errors)
                    .hasExactAuthenticationError(ERROR, "Incorrect password"));
        }
    }

    @Test
    public void applicationReturnMissingPasswordOrAuthenticatedActionCannotPass() {
        Assert.assertFalse(page("https://www.bakuelectronics.az/", true, false, List.of())
                .isUnauthenticatedPasswordStep());
        Assert.assertFalse(page("https://sso.redid.az/auth/offline", false, false, List.of())
                .isUnauthenticatedPasswordStep());
        Assert.assertFalse(page("https://sso.redid.az/auth/offline", true, true, List.of())
                .isUnauthenticatedPasswordStep());
        Assert.assertFalse(page("https://sso.redid.az.example/auth/offline", true, false, List.of())
                .isUnauthenticatedPasswordStep());
    }

    private static LoginPage page(String location, boolean passwordVisible,
                                  boolean authenticatedAction, List<WebElement> errors) {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCurrentUrl" -> location;
                    case "findElements" -> {
                        String locator = args[0].toString();
                        if (ERROR.toString().equals(locator)
                                || "By.cssSelector: form > p.text-red-600".equals(locator)) yield errors;
                        if (locator.contains("password")) yield List.of(element(passwordVisible, ""));
                        if (locator.contains("button[type='submit']")) yield List.of(element(passwordVisible, ""));
                        if (locator.contains("Çıxış")) yield List.of(element(authenticatedAction, ""));
                        throw new AssertionError("Unexpected offline locator.");
                    }
                    case "findElement" -> element(passwordVisible, "");
                    default -> throw new AssertionError("Unexpected browser operation in read-only verification.");
                });
        return new LoginPage(driver);
    }

    private static WebElement element(boolean visible, String text) {
        return (WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isDisplayed" -> visible;
                    case "getText" -> text;
                    default -> throw new AssertionError("Unexpected element operation in read-only verification.");
                });
    }
}
