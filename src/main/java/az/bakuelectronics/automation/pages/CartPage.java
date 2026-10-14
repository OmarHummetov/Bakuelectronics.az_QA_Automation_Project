package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class CartPage {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String CART_PAGE_PATH = "/sebet";
    private static final String PRODUCT_ROUTE_FRAGMENT = "/mehsul/";
    private static final String EMPTY_CART_MESSAGE =
            "Sizin hal-hazırda səbətdə məhsulunuz yoxdur.";

    private static final By CART_PRODUCT = By.xpath(
            "(//main//a[contains(@href, '" + PRODUCT_ROUTE_FRAGMENT + "')])[1]");
    private static final By CART_PRODUCT_ROW = By.xpath(
            "(//main//a[contains(@href, '" + PRODUCT_ROUTE_FRAGMENT + "')])[1]"
                    + "/ancestor::div[.//button[normalize-space() = '-']"
                    + " and .//button[normalize-space() = '+']][1]");
    private static final By PRODUCT_QUANTITY = By.xpath(
            ".//button[normalize-space() = '-']/following-sibling::*[1]");
    private static final By INCREASE_QUANTITY_BUTTON = By.xpath(
            ".//button[normalize-space() = '+']");
    private static final By CART_INCREASE_QUANTITY_BUTTON = By.xpath(
            "(//main//a[contains(@href, '" + PRODUCT_ROUTE_FRAGMENT + "')])[1]"
                    + "/ancestor::div[.//button[normalize-space() = '-']"
                    + " and .//button[normalize-space() = '+']][1]"
                    + "//button[normalize-space() = '+']");
    private static final By DECREASE_QUANTITY_BUTTON = By.xpath(
            ".//button[normalize-space() = '-']");
    private static final By REMOVE_PRODUCT_BUTTON = By.xpath(
            ".//button[not(normalize-space() = '-')"
                    + " and not(normalize-space() = '+')]");
    private static final By CART_TOTAL = By.xpath(
            "//main//*[normalize-space() = 'Cəmi məbləğ:']/following-sibling::*[1]");
    private static final By EMPTY_CART_MESSAGE_LOCATOR = By.xpath(
            "//*[normalize-space() = '" + EMPTY_CART_MESSAGE + "']");
    private static final By CART_NAVIGATION = By.xpath(
            "//div[a[@href='/sebet'] and .//div[normalize-space() = 'Səbətim']]"
                    + "/a[@href='/sebet']");

    private final WaitUtils wait;

    public CartPage(WebDriver driver) {
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }


    public CartPage waitForCartPage() {
        wait.until(ExpectedConditions.urlContains(CART_PAGE_PATH));
        return this;
    }


    public void openCart() {
        wait.until(ExpectedConditions.elementToBeClickable(CART_NAVIGATION)).click();
    }


    public boolean isProductVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(CART_PRODUCT))
                .isDisplayed();
    }

    public boolean isProductVisible(String productSlug) {
        By product = productLink(productSlug);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(product)).isDisplayed();
    }


    public int getProductQuantity() {
        return waitForProductQuantity();
    }

    public int getProductQuantity(String productSlug) {
        By productRow = productRow(productSlug);
        return wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement row = driver.findElement(productRow);
            WebElement quantity = row.findElement(PRODUCT_QUANTITY);
            if (!quantity.isDisplayed()) {
                return null;
            }
            return readQuantity(quantity);
        }));
    }


    public void increaseQuantity() {
        int previousQuantity = waitForProductQuantity();

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(
                        CART_INCREASE_QUANTITY_BUTTON)).click();
                break;
            } catch (StaleElementReferenceException exception) {
                if (attempt == 2) {
                    throw exception;
                }
            }
        }

        wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement productRow = driver.findElement(CART_PRODUCT_ROW);
            WebElement quantity = productRow.findElement(PRODUCT_QUANTITY);
            if (!quantity.isDisplayed()) {
                return null;
            }
            String renderedQuantity = quantity.getText().trim();
            if (renderedQuantity.isEmpty()) {
                return null;
            }
            return Integer.parseInt(renderedQuantity) == previousQuantity + 1
                    ? quantity
                    : null;
        }));
    }


    public void decreaseQuantity() {
        waitForCartControl(DECREASE_QUANTITY_BUTTON).click();
    }

    public void removeProduct() {
        waitForCartControl(REMOVE_PRODUCT_BUTTON).click();
    }

    public String getCartTotal() {
        By cartTotal = By.xpath(
                "//main//span[normalize-space() = 'Cəmi məbləğ:']/following-sibling::span[1]");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(cartTotal))
                .getText()
                .trim();
    }

    public boolean isCartEmpty() {
        By emptyCartState = By.xpath(
                "//main//div[normalize-space() = '" + EMPTY_CART_MESSAGE + "']");
        return wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement emptyCartMessage = driver.findElement(emptyCartState);
            return emptyCartMessage.isDisplayed() ? emptyCartMessage : null;
        })).isDisplayed();
    }

    private int waitForProductQuantity() {
        return wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement productRow = driver.findElement(CART_PRODUCT_ROW);
            WebElement quantity = productRow.findElement(PRODUCT_QUANTITY);
            if (!quantity.isDisplayed()) {
                return null;
            }
            return readQuantity(quantity);
        }));
    }

    private WebElement waitForCartControl(By control) {
        return wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement productRow = driver.findElement(CART_PRODUCT_ROW);
            WebElement button = productRow.findElement(control);
            return button.isDisplayed() && button.isEnabled() ? button : null;
        }));
    }

    private Integer readQuantity(WebElement quantityElement) {
        String renderedQuantity = quantityElement.getAttribute("value");
        if (renderedQuantity == null || renderedQuantity.isBlank()) {
            WebElement quantityInput = quantityElement.findElements(By.cssSelector("input"))
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (quantityInput != null) {
                renderedQuantity = quantityInput.getAttribute("value");
            }
        }
        if (renderedQuantity == null || renderedQuantity.isBlank()) {
            renderedQuantity = quantityElement.getText();
        }
        return renderedQuantity == null || renderedQuantity.isBlank()
                ? null
                : Integer.parseInt(renderedQuantity.trim());
    }

    private By productLink(String productSlug) {
        return By.xpath("//main//a[contains(@href, '" + PRODUCT_ROUTE_FRAGMENT + productSlug + "')]");
    }

    private By productRow(String productSlug) {
        return By.xpath("(" + productLinkExpression(productSlug)
                + ")[1]/ancestor::div[.//button[normalize-space() = '-']"
                + " and .//button[normalize-space() = '+']][1]");
    }

    private String productLinkExpression(String productSlug) {
        return "//main//a[contains(@href, '" + PRODUCT_ROUTE_FRAGMENT + productSlug + "')]";
    }
}
