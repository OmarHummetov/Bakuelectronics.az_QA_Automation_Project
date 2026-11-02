package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import az.bakuelectronics.automation.utils.PriceUtils;
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

    private final WaitUtils wait;
    private final WebDriver driver;

    public CategoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }

    public CategoryPage openGuestSmartphones() {
        driver.get(az.bakuelectronics.automation.config.ConfigReader.getUiBaseUrl()
                + SMARTPHONES_CATEGORY_PATH);
        az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(driver);
        waitForSmartphonesCategory();
        return this;
    }

    public ProductGrid.Snapshot snapshot() { return new ProductGrid(driver).settled(); }

    /** Mobile drawer controls observed at an exact 390x844 CSS viewport. */
    public CategoryPage openMobileGuestSmartphones() {
        driver.get(az.bakuelectronics.automation.config.ConfigReader.getUiBaseUrl() + SMARTPHONES_CATEGORY_PATH);
        az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(driver);
        snapshot();
        return openMobileFilters();
    }

    public CategoryPage openMobileFilters() {
        if (!isDisplayed(By.cssSelector(".ProductsSidebar_ProductsSidebarHeaderBack__iBvfZ"))) {
            wait.clickable(FILTERS_BUTTON).click();
        }
        wait.visible(By.cssSelector(".ProductsSidebar_show__HhefO .ProductsSidebar_ProductsSidebarHeaderBack__iBvfZ"));
        return this;
    }

    public CategoryPage closeMobileFilters() {
        wait.clickable(By.cssSelector(".ProductsSidebar_ProductsSidebarHeaderBack__iBvfZ")).click();
        wait.gone(By.cssSelector(".ProductsSidebar_show__HhefO"));
        return this;
    }

    public CategoryPage selectMobilePriceDescendingSort() {
        ProductGrid.Snapshot before = snapshot();
        wait.clickable(SORTING_BUTTON).click();
        wait.clickable(By.xpath("//button[contains(@class,'style_DropdownBtn__')"
                + " and normalize-space()='Bahadan ucuza']")).click();
        wait.until(active -> "desc".equals(ProductGrid.queryParameter(active.getCurrentUrl(), "priceSort")));
        new ProductGrid(driver).settledAfter(before);
        return this;
    }

    public String mobileSelectedSortLabel() {
        // Selection survives while the dropdown itself is closed/hidden.
        return driver.findElement(By.cssSelector("button.style_DropdownBtn__DWEeg.style_Active__8VHgI"))
                .getDomProperty("textContent").trim();
    }

    public int selectedFilterCheckboxCount() {
        return (int) driver.findElements(By.cssSelector(".ProductsSidebar_ProductsSidebar__iOCZH input[type='checkbox']"))
                .stream().filter(WebElement::isSelected).count();
    }

    public ProductGrid.Snapshot clearAllMobileFilters() {
        openMobileFilters();
        ProductGrid.Snapshot before = snapshot();
        wait.clickable(By.cssSelector(".ProductsSidebar_show__HhefO button.ProductsSidebar_ProductsSidebarHeaderResetBtn__pK7S_"))
                .click();
        wait.until(active -> ProductGrid.queryParameter(active.getCurrentUrl(), "brand") == null
                && ProductGrid.queryParameter(active.getCurrentUrl(), "prices") == null
                && ProductGrid.queryParameter(active.getCurrentUrl(), "priceSort") == null);
        return new ProductGrid(driver).settledAfter(before);
    }

    /** Desktop filter controls are already visible; never toggle a visible panel closed. */
    public CategoryPage applyGuestPriceRange(int minimum, int maximum) {
        ProductGrid.Snapshot before = snapshot();
        if (!isDisplayed(MIN_PRICE_INPUT)) wait.clickable(FILTERS_BUTTON).click();
        WebElement min = wait.visible(MIN_PRICE_INPUT);
        min.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"),
                String.valueOf(minimum));
        WebElement max = wait.visible(MAX_PRICE_INPUT);
        max.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"),
                String.valueOf(maximum), org.openqa.selenium.Keys.ENTER);
        wait.until(active -> (minimum + "," + maximum).equals(
                ProductGrid.queryParameter(active.getCurrentUrl(), "prices")));
        new ProductGrid(driver).settledAfter(before);
        return this;
    }

    public CategoryPage selectPriceDescendingSort() {
        ProductGrid.Snapshot before = snapshot();
        // Revalidated at the extension's 1440px viewport: desktop list, not the hidden mobile dropdown.
        wait.clickable(By.xpath("//ul[contains(@class,'style_FilterLeftDesktop__')]"
                + "//button[normalize-space()='Bahadan ucuza']")).click();
        wait.until(active -> "desc".equals(ProductGrid.queryParameter(active.getCurrentUrl(), "priceSort")));
        new ProductGrid(driver).settledAfter(before);
        return this;
    }

    public String selectedSortLabel() {
        return wait.visible(By.cssSelector("ul.style_FilterLeftDesktop__P86Zt li.style_ActiveLink__iaVxt button"))
                .getText().trim();
    }

    public CategoryPage removeSamsungBrand() {
        if (!isDisplayed(samsungLabel())) wait.clickable(FILTERS_BUTTON).click();
        if (!driver.findElement(samsungCheckbox()).isSelected()) {
            throw new IllegalStateException("Samsung must be selected before individual removal");
        }
        wait.clickable(samsungLabel()).click();
        wait.until(active -> !active.findElement(samsungCheckbox()).isSelected()
                && ProductGrid.queryParameter(active.getCurrentUrl(), "brand") == null);
        snapshot();
        return this;
    }

    // The inspected label contains a separate Samsung span and a product-count span (NBSP after name).
    private static By samsungLabel() {
        return By.xpath("//label[.//input[@type='checkbox'] and span[normalize-space(translate(., '\u00a0', ' '))='Samsung']]");
    }

    private static By samsungCheckbox() {
        return By.xpath("//label[span[normalize-space(translate(., '\u00a0', ' '))='Samsung']]//input[@type='checkbox']");
    }

    public CategoryPage selectSamsungBrand() {
        ProductGrid.Snapshot before = snapshot();
        if (!isDisplayed(samsungLabel())) wait.clickable(FILTERS_BUTTON).click();
        if (driver.findElement(samsungCheckbox()).isSelected()) {
            throw new IllegalStateException("Fresh category must start without Samsung selected");
        }
        wait.clickable(samsungLabel()).click();
        wait.until(active -> active.findElement(samsungCheckbox()).isSelected()
                && "samsung".equals(ProductGrid.queryParameter(active.getCurrentUrl(), "brand")));
        new ProductGrid(driver).settledAfter(before);
        return this;
    }

    public boolean isSamsungSelected() { return driver.findElement(samsungCheckbox()).isSelected(); }

    public List<String> priceBounds() {
        return List.of(driver.findElement(MIN_PRICE_INPUT).getDomProperty("value"),
                driver.findElement(MAX_PRICE_INPUT).getDomProperty("value"));
    }

    public ProductGrid.Snapshot nextGuestPage() {
        ProductGrid.Snapshot before = snapshot();
        wait.clickable(NEXT_PAGE_BUTTON).click();
        wait.visible(SECOND_PAGE_BUTTON);
        wait.until(active -> "2".equals(ProductGrid.queryParameter(active.getCurrentUrl(), "page")));
        return new ProductGrid(driver).settledAfter(before);
    }

    public boolean isSecondPageSelected() {
        return driver.findElements(SECOND_PAGE_BUTTON).stream().anyMatch(WebElement::isDisplayed);
    }

    public String waitForCategoryDestination(String destination) {
        wait.until(active -> java.net.URI.create(active.getCurrentUrl()).getPath()
                .equals(java.net.URI.create(destination).getPath()));
        new ProductGrid(driver).settled();
        return wait.visible(CATEGORY_TITLE).getText().trim();
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
            prices.add(PriceUtils.parseAzn(priceElement.getText()));
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
