package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.pages.LoginPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

public class LoginTest extends BaseTest {

    private static final String HOME_PAGE_URL = ConfigReader.getUiBaseUrl() + "/";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POST_LOGIN_WAIT_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration POPUP_WAIT_TIMEOUT = Duration.ofSeconds(3);
    private static final By CAMPAIGN_POPUP_CLOSE = By.cssSelector("button.campaignModalClose");
    private static final By ACCOUNT_LINK = By.cssSelector("a[href^='https://sso.redid.az/auth/']");
    private static final By ACCOUNT_PROFILE_BUTTON = By.cssSelector("button.header_UserIcon__gOK4_");
    private static final By MOBILE_NUMBER_INPUT = By.cssSelector("input[placeholder='+994']");
    private static final By EMAIL_INPUT = By.cssSelector("input[placeholder='siz@example.com']");
    private static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
    private static final By LOGOUT_BUTTON = By.xpath("//*[normalize-space()='Çıxış']");
    private static final By ORDERS_LINK = By.xpath("//a[@href='/istifadeci/sifarisler' and normalize-space()='Sifarişlərim']");
    private static final By ACCOUNT_NAME = By.xpath("//*[normalize-space()='Ömər Hümmətov']");

    @Test(groups = "regression")
    public void configuredIdentifierCanBeEnteredInTheCorrectSsoMode() {
        driver().get(HOME_PAGE_URL);
        dismissCampaignPopupIfPresent();

        WebDriverWait wait = new WebDriverWait(driver(), WAIT_TIMEOUT);
        wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_LINK)).click();
        wait.until(ExpectedConditions.urlMatches("https://sso\\.redid\\.az(?:/.*)?"));
        Assert.assertTrue(driver().getCurrentUrl().startsWith("https://sso.redid.az/"),
                "Clicking the homepage account link should open the RED ID SSO page.");

        String login = ConfigReader.getLogin();
        LoginPage loginPage = new LoginPage(driver());
        By identifierInput;
        boolean phoneLogin = false;
        if (looksLikeEmail(login)) {
            loginPage.selectEmailLogin().enterEmail(login);
            identifierInput = EMAIL_INPUT;
        } else {
            loginPage.selectMobileNumberLogin().enterMobileNumber(login);
            identifierInput = MOBILE_NUMBER_INPUT;
            phoneLogin = true;
        }

        WebElement visibleIdentifierInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(identifierInput));
        Assert.assertTrue(visibleIdentifierInput.isDisplayed(),
                "The identifier input for the configured login mode should be visible.");
        String actualIdentifier = visibleIdentifierInput.getAttribute("value");
        String expectedIdentifier = login;
        if (phoneLogin) {
            actualIdentifier = normalizePhoneNumber(actualIdentifier);
            expectedIdentifier = normalizePhoneNumber(expectedIdentifier);
        }
        Assert.assertEquals(actualIdentifier, expectedIdentifier,
                "The configured identifier should be present in the selected SSO input.");

        loginPage.submitLogin();
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));

        String password = ConfigReader.getPassword();
        loginPage.enterPassword(password);
        loginPage.submitPassword();

        WebDriverWait postLoginWait = new WebDriverWait(driver(), POST_LOGIN_WAIT_TIMEOUT);
        postLoginWait.until(ExpectedConditions.and(
                ExpectedConditions.urlToBe(HOME_PAGE_URL),
                ExpectedConditions.titleContains("Baku Electronics")));
        postLoginWait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_PROFILE_BUTTON)).click();

        WebElement logoutButton = postLoginWait.until(
                ExpectedConditions.visibilityOfElementLocated(LOGOUT_BUTTON));
        Assert.assertTrue(logoutButton.isDisplayed(),
                "The authenticated account drawer should display the logout action.");
        Assert.assertEquals(logoutButton.getText().trim(), "Çıxış",
                "The authenticated account drawer should display the expected logout action.");

        WebElement ordersLink = postLoginWait.until(
                ExpectedConditions.visibilityOfElementLocated(ORDERS_LINK));
        Assert.assertTrue(ordersLink.isDisplayed(),
                "The authenticated account drawer should display the orders link.");
        Assert.assertEquals(ordersLink.getText().trim(), "Sifarişlərim",
                "The authenticated account drawer should display the expected orders link.");

        WebElement accountName = postLoginWait.until(
                ExpectedConditions.visibilityOfElementLocated(ACCOUNT_NAME));
        Assert.assertEquals(accountName.getText().trim(), "Ömər Hümmətov",
                "The authenticated account drawer should display the expected account name.");
    }

    private boolean looksLikeEmail(String login) {
        return login.contains("@") && login.indexOf('@') > 0 && login.indexOf('@') < login.length() - 1;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceFirst("^\\+994\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void dismissCampaignPopupIfPresent() {
        try {
            new WebDriverWait(driver(), POPUP_WAIT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(CAMPAIGN_POPUP_CLOSE))
                    .click();
        } catch (TimeoutException ignored) {
            // The campaign popup is optional.
        }
    }
}
