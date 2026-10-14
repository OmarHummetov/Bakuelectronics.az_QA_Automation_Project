package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import az.bakuelectronics.automation.models.ProductApiModel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class SearchPage {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String SEARCH_RESULTS_PATH = "/axtaris-neticesi";
    private static final String PLAYSTATION_5_SLIM_ROUTE = "/mehsul/playstation-5-slim-blue-ray-201123";
    private static final By NO_RESULTS_MESSAGE = By.xpath(
            "//*[normalize-space() = 'Nəticə tapılmadı']");

    private static final By PLAYSTATION_5_SLIM_PRODUCT =
            By.cssSelector("a[href='" + PLAYSTATION_5_SLIM_ROUTE + "']");

    private final WaitUtils wait;

    public SearchPage(WebDriver driver) {
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }


    public SearchPage waitForSearchResultsPage() {
        wait.until(ExpectedConditions.urlContains(SEARCH_RESULTS_PATH));
        return this;
    }


    public boolean isNoResultsMessageVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(NO_RESULTS_MESSAGE))
                .isDisplayed();
    }


    public boolean isExpectedProductResultVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PLAYSTATION_5_SLIM_PRODUCT))
                .isDisplayed();
    }


    public boolean isProductResultVisible(ProductApiModel product) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(
                    productResultLocator(product.getSlug()))).isDisplayed();
        } catch (TimeoutException ignored) {
            return false;
        }
    }


    public void clickProductResult(ProductApiModel product) {
        wait.until(ExpectedConditions.elementToBeClickable(
                productResultLocator(product.getSlug()))).click();
    }

    private static By productResultLocator(String slug) {
        return By.cssSelector("a[href='/mehsul/" + slug + "']");
    }


    public void clickExpectedProductResult() {
        wait.until(ExpectedConditions.elementToBeClickable(PLAYSTATION_5_SLIM_PRODUCT)).click();
    }
}
