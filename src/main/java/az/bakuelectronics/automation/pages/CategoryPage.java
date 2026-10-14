package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class CategoryPage {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String SMARTPHONES_CATEGORY_PATH =
            "/catalog/telefonlar-qadcetler/smartfonlar-mobil-telefonlar";

    private static final By CATEGORY_TITLE =
            By.cssSelector("h1.style_ProductsInnerHeaderTitleDesktop__GKuAD");
    private static final By PRODUCT_CARDS = By.cssSelector("a.ProductCard_Product__3nHrM");
    private static final By PRODUCT_LINKS = By.cssSelector(
            "div.Products_ProductsInnerBodyGrid__Od4SH a.ProductCard_Product__3nHrM");
    private static final By PAGINATION = By.cssSelector("div.g-pagination.products-pagination");
    private static final By NEXT_PAGE_BUTTON = By.cssSelector(
            "div.g-pagination.products-pagination button[aria-label='Go to next page']");
    private static final By SECOND_PAGE_BUTTON = By.cssSelector(
            "div.g-pagination.products-pagination button[aria-current='true'][aria-label='page 2']");
    private static final By SORTING_BUTTON = By.xpath("//button[normalize-space()='Sıralama']");
    private static final By FILTERS_BUTTON = By.xpath("//button[normalize-space()='Filterlər']");
    private static final By ASCENDING_SORT_OPTION = By.cssSelector(
            "button.style_DropdownBtn__DWEeg");
    private static final By MIN_PRICE_INPUT = By.cssSelector("input[placeholder='Min.']");
    private static final By MAX_PRICE_INPUT = By.cssSelector("input[placeholder='Max.']");
    private static final By CURRENT_PRODUCT_PRICES = By.cssSelector(
            "div.Products_ProductsInnerBodyGrid__Od4SH > "
                    + "a.ProductCard_Product__3nHrM .ProductCard_OriginalPrice___qWZ1");
    private static final By PRODUCT_TITLES = By.cssSelector(
            "a.ProductCard_Product__3nHrM h4.ProductCard_ProductTitle___frc_");

    private final WaitUtils wait;
    private final WebDriver driver;

    public CategoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }

    public CategoryPage waitForSmartphonesCategory() {
        wait.until(ExpectedConditions.urlContains(SMARTPHONES_CATEGORY_PATH));
        wait.until(ExpectedConditions.visibilityOfElementLocated(CATEGORY_TITLE));
        wait.until(ExpectedConditions.visibilityOfElementLocated(PRODUCT_CARDS));
        return this;
    }

    public boolean isSmartphonesCategoryUrl(String currentUrl) {
        return currentUrl.contains(SMARTPHONES_CATEGORY_PATH);
    }

    public String getCategoryTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(CATEGORY_TITLE)).getText();
    }

    public boolean hasProducts() {
        return !wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(PRODUCT_CARDS)).isEmpty();
    }

    public boolean isPaginationVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PAGINATION)).isDisplayed();
    }

    public String getFirstProductTitle() {
        List<WebElement> products = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(PRODUCT_CARDS));
        return products.get(0).findElement(By.cssSelector("h4.ProductCard_ProductTitle___frc_")).getText();
    }

    public void goToNextPage() {
        wait.until(ExpectedConditions.elementToBeClickable(NEXT_PAGE_BUTTON)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(SECOND_PAGE_BUTTON));
        wait.until(ExpectedConditions.attributeToBe(SECOND_PAGE_BUTTON, "aria-current", "true"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(PRODUCT_CARDS));
    }

    public void selectPriceAscendingSort() {
        wait.until(ExpectedConditions.elementToBeClickable(SORTING_BUTTON)).click();
        wait.until(driver -> driver.findElements(ASCENDING_SORT_OPTION).stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .filter(element -> "Ucuzdan bahaya".equals(element.getText().trim()))
                .findFirst()
                .map(element -> {
                    element.click();
                    return true;
                })
                .orElse(false));
        wait.until(ExpectedConditions.urlContains("priceSort=acs"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(CURRENT_PRODUCT_PRICES));
    }

    public List<BigDecimal> getProductPrices() {
        List<WebElement> priceElements = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(CURRENT_PRODUCT_PRICES));
        List<BigDecimal> prices = new ArrayList<>();
        for (WebElement priceElement : priceElements) {
            String numericPrice = priceElement.getText()
                    .replace("₼", "")
                    .replace(",", "")
                    .trim();
            prices.add(new BigDecimal(numericPrice));
        }
        return prices;
    }

    public CategoryPage selectBrand(String brand) {
        By brandOption = brandFilterOption(brand);
        List<String> previousProductLinks = visibleProductLinks();

        if (!isDisplayed(brandOption)) {
            wait.until(ExpectedConditions.elementToBeClickable(FILTERS_BUTTON)).click();
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(brandOption));
        wait.until(ExpectedConditions.visibilityOfElementLocated(brandOption));
        wait.until(ExpectedConditions.elementToBeClickable(brandOption)).click();
        wait.until(ExpectedConditions.elementToBeSelected(brandCheckbox(brand)));
        wait.until(ExpectedConditions.urlContains("brand=" + brand.toLowerCase()));
        wait.until(driver -> {
            List<String> currentProductLinks = visibleProductLinks();
            return !currentProductLinks.isEmpty()
                    && !currentProductLinks.equals(previousProductLinks);
        });
        final List<String> previousObservedLinks = new ArrayList<>();
        wait.until(driver -> {
            List<String> currentProductLinks = visibleProductLinks();
            boolean stable = !currentProductLinks.isEmpty()
                    && currentProductLinks.equals(previousObservedLinks);
            previousObservedLinks.clear();
            previousObservedLinks.addAll(currentProductLinks);
            return stable;
        });
        return this;
    }

    public List<String> getProductTitles() {
        By productTitleLocator = By.cssSelector(
                "div.Products_ProductsInnerBodyGrid__Od4SH "
                        + "a.ProductCard_Product__3nHrM h4.ProductCard_ProductTitle___frc_");
        List<WebElement> titleElements = wait.until(driver -> {
            List<WebElement> visibleTitles = new ArrayList<>();
            for (WebElement titleElement : driver.findElements(productTitleLocator)) {
                if (titleElement.isDisplayed()) {
                    visibleTitles.add(titleElement);
                }
            }
            return visibleTitles.isEmpty() ? null : visibleTitles;
        });
        List<String> titles = new ArrayList<>();
        for (WebElement titleElement : titleElements) {
            titles.add(titleElement.getText().trim());
        }
        return titles;
    }

    private By brandFilterOption(String brand) {
        return By.xpath("//label[.//input[@type='checkbox'] and .//span[contains(., "
                + xpathLiteral(brand.trim()) + ")]]");
    }

    private By brandCheckbox(String brand) {
        return By.xpath("//label[.//input[@type='checkbox'] and .//span[contains(., "
                + xpathLiteral(brand.trim()) + ")]]//input[@type='checkbox']");
    }

    private boolean isDisplayed(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    private List<String> visibleProductLinks() {
        List<String> links = new ArrayList<>();
        for (WebElement productLink : driver.findElements(PRODUCT_LINKS)) {
            if (productLink.isDisplayed()) {
                links.add(productLink.getAttribute("href"));
            }
        }
        return links;
    }

    private String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "', \"'\", '") + "')";
    }

    public void filterByPriceRange(int minimum, int maximum) {
        List<String> previousProductLinks = visibleProductLinks();

        wait.until(driver -> driver.findElements(FILTERS_BUTTON).stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .findFirst()
                .map(element -> {
                    element.click();
                    return true;
                })
                .orElse(false));

        WebElement minInput = wait.until(ExpectedConditions.visibilityOfElementLocated(MIN_PRICE_INPUT));
        WebElement maxInput = wait.until(ExpectedConditions.visibilityOfElementLocated(MAX_PRICE_INPUT));

        minInput.clear();
        minInput.sendKeys(String.valueOf(minimum));
        maxInput.clear();
        maxInput.sendKeys(String.valueOf(maximum));
        maxInput.sendKeys(org.openqa.selenium.Keys.ENTER);

        wait.until(ExpectedConditions.urlContains("prices=" + minimum));
        wait.until(driver -> {
            List<String> currentProductLinks = visibleProductLinks();
            return !currentProductLinks.isEmpty()
                    && !currentProductLinks.equals(previousProductLinks);
        });
        final List<String> previousObservedLinks = new ArrayList<>();
        wait.until(driver -> {
            List<String> currentProductLinks = visibleProductLinks();
            boolean stable = !currentProductLinks.isEmpty()
                    && currentProductLinks.equals(previousObservedLinks);
            previousObservedLinks.clear();
            previousObservedLinks.addAll(currentProductLinks);
            return stable;
        });
    }
}
