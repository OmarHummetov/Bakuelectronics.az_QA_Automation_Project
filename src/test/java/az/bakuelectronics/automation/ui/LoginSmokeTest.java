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

public class LoginSmokeTest extends BaseTest {

    private static final String HOME_PAGE_URL = ConfigReader.getUiBaseUrl() + "/";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POPUP_WAIT_TIMEOUT = Duration.ofSeconds(3);
    private static final By CAMPAIGN_POPUP_CLOSE = By.cssSelector("button.campaignModalClose");
    private static final By ACCOUNT_LINK = By.cssSelector("a[href^='https://sso.redid.az/auth/']");

    @Test(groups = "smoke")
    public void loginPageOpensInRedIdAndFormIsInteractive() {
        driver().get(HOME_PAGE_URL);

        WebDriverWait wait = new WebDriverWait(driver(), WAIT_TIMEOUT);
        dismissCampaignPopupIfPresent();
        WebElement accountLink = wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_LINK));
        accountLink.click();

        wait.until(ExpectedConditions.urlMatches("https://sso\\.redid\\.az(?:/.*)?"));
        Assert.assertTrue(driver().getCurrentUrl().startsWith("https://sso.redid.az/"),
                "Clicking the homepage account link should navigate to the RED ID SSO page.");

        LoginPage loginPage = new LoginPage(driver());
        loginPage.selectMobileNumberLogin();
        loginPage.selectEmailLogin();
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
