package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class SearchPage {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String SEARCH_RESULTS_PATH = "/axtaris-neticesi";
    private static final String PLAYSTATION_5_SLIM_ROUTE = "/mehsul/playstation-5-slim-blue-ray-201123";
    private static final By NO_RESULTS_MESSAGE = By.xpath(
            "//*[normalize-space() = 'Nəticə tapılmadı']");
    private static final By PRODUCT_RESULT_LINKS = By.cssSelector("a[href^='/mehsul/']");

    private static final By PLAYSTATION_5_SLIM_PRODUCT =
            By.cssSelector("a[href='" + PLAYSTATION_5_SLIM_ROUTE + "']");

    private final WaitUtils wait;
    private final WebDriver driver;

    public SearchPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }

    public ProductGrid.Snapshot waitForQuery(String query) {
        return waitForQuery(query, null);
    }

    /** Search equivalence concerns products, not the site's spelling of the URL query. */
    public ProductGrid.Snapshot settledSubmittedResults() {
        wait.until(active -> java.net.URI.create(active.getCurrentUrl()).getPath().equals(SEARCH_RESULTS_PATH)
                && ProductGrid.queryParameter(active.getCurrentUrl(), "name") != null);
        return new ProductGrid(driver).settled();
    }

    public ProductGrid.Snapshot waitForQuery(String query, ProductGrid.Snapshot previous) {
        wait.until(active -> java.net.URI.create(active.getCurrentUrl()).getPath().equals(SEARCH_RESULTS_PATH)
                && query.equals(ProductGrid.queryParameter(active.getCurrentUrl(), "name")));
        return new ProductGrid(driver).settledAfter(previous);
    }

    public ProductGrid.Snapshot restoreHistory(boolean forward, String query) {
        ProductGrid.Snapshot previous = new ProductGrid(driver).settled();
        if (forward) driver.navigate().forward();
        else driver.navigate().back();
        return waitForQuery(query, previous);
    }


    public SearchPage waitForSearchResultsPage() {
        wait.until(ExpectedConditions.urlContains(SEARCH_RESULTS_PATH));
        wait.until(driver -> !driver.findElements(PRODUCT_RESULT_LINKS).isEmpty()
                || !driver.findElements(NO_RESULTS_MESSAGE).isEmpty());
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


    public void clickExpectedProductResult() {
        wait.until(ExpectedConditions.elementToBeClickable(PLAYSTATION_5_SLIM_PRODUCT)).click();
    }
}
