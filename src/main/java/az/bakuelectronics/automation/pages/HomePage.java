package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.utils.CampaignPopup;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class HomePage {

    private static final String HOME_PAGE_URL = ConfigReader.getUiBaseUrl() + "/";
    private static final String SMARTPHONES_CATEGORY_URL =
            ConfigReader.getUiBaseUrl() + "/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final By ACCOUNT_LINK = By.cssSelector("a[href^='https://sso.redid.az/auth/']");
    private static final By SEARCH_INPUT = By.cssSelector("input#input");
    private static final By SEARCH_SUBMIT_BUTTON = By.cssSelector("button#submitBtn[type='submit']");
    private static final By SEARCH_OVERLAY = By.cssSelector(".modal-backdrop, .campaignModal.modal.show");

    private final WebDriver driver;
    private final WaitUtils wait;

    public HomePage(WebDriver driver) {
        this(driver, WAIT_TIMEOUT);
    }

    public HomePage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, timeout);
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
        CampaignPopup.dismissIfPresent(driver);
        return this;
    }

    public void clickAccountLink() {
        wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_LINK)).click();
    }

    public HomePage enterSearchQuery(String query) {
        clickSearchControl(SEARCH_INPUT);
        wait.until(active -> {
            if (!active.findElements(SEARCH_OVERLAY).isEmpty()) return false;
            WebElement input = active.findElement(SEARCH_INPUT);
            input.clear();
            input.sendKeys(query);
            return true;
        });
        wait.until(ExpectedConditions.attributeContains(SEARCH_INPUT, "value", query));
        return this;
    }

    public void submitSearch() {
        clickSearchControl(SEARCH_SUBMIT_BUTTON);
    }

    private void clickSearchControl(By locator) {
        boolean[] clearPreviously = {false};
        try {
            wait.until(active -> {
                if (!active.findElements(SEARCH_OVERLAY).isEmpty()) {
                    clearPreviously[0] = false;
                    return false;
                }
                WebElement control = active.findElement(locator);
                if (!control.isDisplayed() || !control.isEnabled()) {
                    clearPreviously[0] = false;
                    return false;
                }
                if (!clearPreviously[0]) { clearPreviously[0] = true; return false; }
                try {
                    control.click();
                    return true;
                } catch (ElementClickInterceptedException intercepted) {


                    clearPreviously[0] = false;
                    if (active.findElements(SEARCH_OVERLAY).isEmpty()
                            && !intercepted.getRawMessage().contains("modal-backdrop")) {
                        throw new ElementClickInterceptedException("Search control blocked by an unexpected overlay.");
                    }
                    return false;
                }
            });
        } catch (TimeoutException timeout) {
            throw new TimeoutException("Search control not ready after bounded modal/backdrop clearance wait.");
        }
    }

    public void openSmartphonesCategory() {
        driver.get(SMARTPHONES_CATEGORY_URL);
    }
}
