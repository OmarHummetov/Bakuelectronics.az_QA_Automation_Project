package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class HomePage {

    private static final String HOME_PAGE_URL = ConfigReader.getUiBaseUrl() + "/";
    private static final String SMARTPHONES_CATEGORY_URL =
            ConfigReader.getUiBaseUrl() + "/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POPUP_WAIT_TIMEOUT = Duration.ofSeconds(3);

    private static final By CAMPAIGN_POPUP_CLOSE = By.cssSelector("button.campaignModalClose");
    private static final By ACCOUNT_LINK = By.cssSelector("a[href^='https://sso.redid.az/auth/']");
    private static final By SEARCH_INPUT = By.cssSelector("input#input");
    private static final By SEARCH_SUBMIT_BUTTON = By.cssSelector("button#submitBtn[type='submit']");

    private final WebDriver driver;
    private final WaitUtils wait;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }

    public HomePage open() {
        driver.get(HOME_PAGE_URL);
        wait.until(ExpectedConditions.titleContains("Baku Electronics"));
        return this;
    }

    public HomePage waitForHomePage() {
        wait.until(ExpectedConditions.urlToBe(HOME_PAGE_URL));
        wait.until(ExpectedConditions.titleContains("Baku Electronics"));
        return this;
    }

    public boolean isOpen() {
        return HOME_PAGE_URL.equals(driver.getCurrentUrl());
    }

    public HomePage dismissCampaignPopupIfPresent() {
        try {
            new WaitUtils(driver, POPUP_WAIT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(CAMPAIGN_POPUP_CLOSE))
                    .click();
        } catch (TimeoutException ignored) {
            // The campaign popup is optional.
        }
        return this;
    }

    public void clickAccountLink() {
        wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_LINK)).click();
    }

    public HomePage enterSearchQuery(String query) {
        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        wait.until(ExpectedConditions.elementToBeClickable(searchInput)).click();
        searchInput.clear();
        searchInput.sendKeys(query);
        wait.until(ExpectedConditions.attributeContains(SEARCH_INPUT, "value", query));
        return this;
    }

    public void submitSearch() {
        wait.until(ExpectedConditions.elementToBeClickable(SEARCH_SUBMIT_BUTTON)).click();
    }

    public void openSmartphonesCategory() {
        driver.get(SMARTPHONES_CATEGORY_URL);
    }
}
