package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;


public class ProductPage {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String PRODUCT_PAGE_PATH = "/mehsul/";

    private static final By PRODUCT_NAME = By.cssSelector("h1");
    private static final By PRODUCT_CODE = By.xpath(
            "//p[starts-with(normalize-space(), 'Məhsul kodu:')]");
    private static final By CURRENT_PRICE = By.xpath(
            "//p[starts-with(normalize-space(), 'Məhsul kodu:')]/parent::div"
                    + "/following-sibling::div[1]/div[1]/p");
    private static final By ADD_TO_CART_BUTTON = By.xpath(
            "//button[.//span[normalize-space() = 'Səbətə əlavə et']]");
    private static final By CHARACTERISTICS_SECTION = By.xpath(
            "//button[contains(normalize-space(.), 'Xüsusiyyətlər')]");

    private final WaitUtils wait;
    private final WebDriver driver;

    public ProductPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }

    public ProductPage openGuestPlayStation() {
        driver.get(az.bakuelectronics.automation.config.ConfigReader.getUiBaseUrl()
                + "/mehsul/playstation-5-slim-blue-ray-201123");
        az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(driver);
        waitForProductPage();
        getProductName();
        getProductCode();
        return this;
    }

    public ProductGalleryComponent gallery() { return new ProductGalleryComponent(driver); }

    public ProductReviewsComponent reviews() { return new ProductReviewsComponent(driver); }

    public InstallmentCalculatorComponent installments() { return new InstallmentCalculatorComponent(driver); }

    public RelatedProductsComponent relatedProducts() { return new RelatedProductsComponent(driver); }

    public ProductPage openGuestOutOfStockCandidate() {
        driver.get(az.bakuelectronics.automation.config.ConfigReader.getUiBaseUrl()
                + "/mehsul/sony-playstation-4-slim-500gb-cuh-2216a-215083");
        az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(driver);
        getProductName();
        getProductCode();
        return this;
    }

    public record PurchaseState(String url, String name, String code, String stockText,
                                String notificationText, boolean notificationUsable, int usableCartActions) { }

    /** Observe settled current stock, including available states; never wait for a favorable assertion. */
    public PurchaseState settledPurchaseState() {
        PurchaseState[] last = {null};
        long[] since = {0};
        int[] polls = {0};
        return new WaitUtils(driver, Duration.ofSeconds(20)).until(active -> {
            WebElement details = wait.visible(By.cssSelector("main .ProdDetails_ProductDetailsContentRight__qD_hb"));
            WebElement purchase = details.findElement(By.cssSelector(".ProdDetails_PurchaseContainerSectionPromo__uBvKO"));
            var stock = details.findElements(By.cssSelector(".ProdDetails_NoStockAlertText__1IPrH"))
                    .stream().filter(WebElement::isDisplayed).map(WebElement::getText).toList();
            var notification = purchase.findElements(By.cssSelector("button.ProdDetails_NotificationBtn__etd2h"))
                    .stream().filter(WebElement::isDisplayed).toList();
            int usable = (int) purchase.findElements(By.xpath(".//button[normalize-space()='Səbətə əlavə et'"
                            + " or normalize-space()='Səbətə at'] | .//a[normalize-space()='Səbətə əlavə et'"
                            + " or normalize-space()='Səbətə at']"))
                    .stream().filter(ProductPage::usableAction).count();
            PurchaseState current = new PurchaseState(active.getCurrentUrl(), getProductName(), getProductCode(),
                    String.join(" | ", stock), notification.isEmpty() ? "" : notification.getFirst().getText().trim(),
                    notification.stream().anyMatch(ProductPage::usableAction), usable);
            if (!current.equals(last[0])) { last[0] = current; since[0] = System.nanoTime(); polls[0] = 1; return null; }
            return ++polls[0] >= 3 && System.nanoTime() - since[0] >= 1_200_000_000L ? current : null;
        });
    }

    private static boolean usableAction(WebElement element) {
        return element.isDisplayed() && element.isEnabled() && !"true".equals(element.getDomAttribute("aria-disabled"));
    }

    public record Breadcrumb(String href, String title) { }

    public Breadcrumb captureCategoryBreadcrumb() {
        WebElement crumb = wait.visible(categoryBreadcrumb());
        return new Breadcrumb(crumb.getAttribute("href"), crumb.getText().trim());
    }

    public void followCategoryBreadcrumb(Breadcrumb captured) {
        WebElement crumb = wait.visible(categoryBreadcrumb());
        if (!captured.href().equals(crumb.getAttribute("href"))
                || !captured.title().equals(crumb.getText().trim())) {
            throw new IllegalStateException("Category breadcrumb changed before navigation");
        }
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", crumb);
        wait.until(active -> (Boolean) ((org.openqa.selenium.JavascriptExecutor) active).executeScript(
                "const e=arguments[0],r=e.getBoundingClientRect();"
                        + "return e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2));", crumb));
        crumb.click();
    }

    private static By categoryBreadcrumb() {
        return By.xpath("//main//a[@href='/catalog/oyun-konsollari-oyunlar/oyun-konsollari'"
                + " and normalize-space()='Oyun konsolları']");
    }


    public ProductPage waitForProductPage() {
        wait.until(ExpectedConditions.urlContains(PRODUCT_PAGE_PATH));
        return this;
    }


    public String getProductName() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PRODUCT_NAME))
                .getText()
                .trim();
    }

    public String getProductCode() {
        String productCodeText = wait.until(ExpectedConditions.visibilityOfElementLocated(PRODUCT_CODE))
                .getText()
                .trim();
        return productCodeText.substring(productCodeText.indexOf(':') + 1).trim();
    }


    public String getCurrentPrice() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(CURRENT_PRICE))
                .getText()
                .trim();
    }


    public String getCurrentPriceDomDescription() {
        WebElement priceElement = wait.until(
                ExpectedConditions.visibilityOfElementLocated(CURRENT_PRICE));
        return "locator=" + CURRENT_PRICE + ", tag=" + priceElement.getTagName();
    }


    public void clickAddToCart() {
        WebElement addToCartButton = wait.until(
                driver -> addToCartButtonReadyForClick(driver));
        addToCartButton.click();
    }

    public boolean isCharacteristicsSectionVisible() {
        return wait.until(driver -> driver.findElements(CHARACTERISTICS_SECTION).stream()
                .anyMatch(WebElement::isDisplayed));
    }

    public boolean isAddToCartButtonClickable() {
        return wait.until(driver -> addToCartButtonReadyForClick(driver)) != null;
    }


    private WebElement addToCartButtonReadyForClick(WebDriver driver) {
        try {
            WebElement button = driver.findElement(ADD_TO_CART_BUTTON);
            if (!button.isDisplayed() || !button.isEnabled()) {
                return null;
            }

            Rectangle buttonRect = button.getRect();
            int clickX = buttonRect.getX() + buttonRect.getWidth() / 2;
            int clickY = buttonRect.getY() + buttonRect.getHeight() / 2;

            for (WebElement dropdown : driver.findElements(By.cssSelector("ul"))) {
                if (dropdown.isDisplayed() && containsPoint(dropdown.getRect(), clickX, clickY)) {
                    return null;
                }
            }
            return button;
        } catch (StaleElementReferenceException ignored) {
            return null;
        }
    }

    private boolean containsPoint(Rectangle rectangle, int x, int y) {
        return x >= rectangle.getX()
                && x < rectangle.getX() + rectangle.getWidth()
                && y >= rectangle.getY()
                && y < rectangle.getY() + rectangle.getHeight();
    }


    public int getCartItemCount() {
        By visibleCartBadge = By.xpath(
                "//div[a[@href='/sebet'] and .//div[normalize-space() = 'Səbətim']]"
                        + "/a[@href='/sebet']/span");
        String badgeText = wait.until(ExpectedConditions.visibilityOfElementLocated(visibleCartBadge))
                .getText()
                .trim();
        return Integer.parseInt(badgeText);
    }
}
