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
    private static final By CART_BADGE = By.cssSelector("a[href='/sebet'] span");

    private final WaitUtils wait;

    public ProductPage(WebDriver driver) {
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
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
