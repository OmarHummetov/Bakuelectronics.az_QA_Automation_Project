package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.WaitUtils;
import az.bakuelectronics.automation.utils.PriceUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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


    private static final String CART_CONTROL_ROWS =
            "//main//button[normalize-space() = '-' and not(ancestor::*["
                    + "contains(concat(' ', normalize-space(@class)), ' sebet_BasketRight__')])]"
                    + "/ancestor::div[.//a[contains(@href, '/mehsul/')]"
                    + " and .//button[normalize-space() = '+']][1]";
    private static final By PRODUCT_QUANTITY = By.xpath(
            ".//button[normalize-space() = '-']/following-sibling::*[1]");
    private static final By INCREASE_QUANTITY_BUTTON = By.xpath(
            ".//button[normalize-space() = '+']");
    private static final By DECREASE_QUANTITY_BUTTON = By.xpath(
            ".//button[normalize-space() = '-']");
    private static final By REMOVE_PRODUCT_BUTTON = By.xpath(
            ".//button[not(normalize-space() = '-')"
                    + " and not(normalize-space() = '+')]");
    private static final By CART_TOTAL = By.xpath(
            "//main//*[normalize-space() = 'Cəmi məbləğ:']/following-sibling::*[1]");
    private static final By EMPTY_CART_MESSAGE_LOCATOR = By.xpath(
            "//main//div[normalize-space() = '" + EMPTY_CART_MESSAGE + "']");
    private static final By CART_NAVIGATION = By.xpath(
            "//a[@href='/sebet' or @href='/sebet/']");
    private static final By MODAL_BACKDROP = By.cssSelector(".modal-backdrop");
    private static final By CAMPAIGN_CLOSE = By.cssSelector("button.campaignModalClose");

    private final WaitUtils wait;
    private final WebDriver driver;
    private String lastMutation = "No cart mutation recorded";
    private static final By MUTATION_BLOCKERS = By.cssSelector(
            ".modal-backdrop, .campaignModal.modal.show, main [aria-busy='true']");

    private final Map<String, MutationState> pendingRemovals = new HashMap<>();



    private static final String SUMMARY_SNAPSHOT = """
            const visible = e => !!e.getClientRects().length;
            const all = (root, prefix) => [...root.querySelectorAll(
                `[class^='${prefix}'], [class*=' ${prefix}']`)].filter(visible);
            const one = (root, prefix) => {
                const nodes = all(root, prefix);
                if (nodes.length !== 1) throw Error(`Expected exactly one ${prefix}; found ${nodes.length}`);
                return nodes[0];
            };
            const text = e => e.innerText.trim();
            const main = document.querySelector('main');
            if (!main) return null;
            const summary = one(main, 'sebet_BasketRight__');
            const rows = all(summary, 'sebet_BasketRightBodyItem__');
            if (rows.length !== 1) throw Error(`Expected exactly one summary product row; found ${rows.length}`);
            const row = rows[0];
            const name = one(row, 'sebet_ItemName__');
            const normalize = s => s.replace(/\\s+/g, ' ').trim();
            if (normalize(text(name)) !== normalize(arguments[0]))
                throw Error('Summary product name does not match the selected product');
            const original = one(row, 'sebet_BasketRightBodyItemPriceOld__');
            const current = one(row, 'sebet_BasketRightBodyItemPriceCurrent__');
            const quantity = one(row, 'sebet_ItemCount__');
            const discount = one(summary, 'sebet_BasketRightDiscountPrice__');
            const total = one(summary, 'sebet_TotalTopPrice__');
            // Remove only the verified fields from a detached copy. Any remaining
            // displayed amount (including a zero fee) is an unsupported component.
            const copy = summary.cloneNode(true);
            const fields = [name, original, current, quantity, discount, total];
            const originals = [summary, ...summary.querySelectorAll('*')];
            const copies = [copy, ...copy.querySelectorAll('*')];
            fields.forEach(e => copies[originals.indexOf(e)].remove());
            [...summary.querySelectorAll('*')].filter(e => !visible(e))
                .forEach(e => copies[originals.indexOf(e)].remove());
            const remaining = copy.textContent.replace(/\\s+/g, ' ').trim();
            if (/\\d/.test(remaining))
                throw Error('Unexpected summary charge/amount outside the verified price fields: ' + remaining);
            const evaluateRows = xpath => {
                const result = document.evaluate(xpath, document, null,
                    XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
                return Array.from({length: result.snapshotLength}, (_, i) => result.snapshotItem(i))
                    .filter(visible);
            };
            const selectedRows = evaluateRows(arguments[2]);
            if (selectedRows.length !== 1)
                throw Error(`Expected exactly one selected quantity-control row; found ${selectedRows.length}`);
            const selected = selectedRows[0];
            if (evaluateRows(arguments[1]).some(candidate => candidate !== selected))
                throw Error('Unexpected additional cart product quantity-control row');
            for (const sign of ['-', '+']) {
                const buttons = [...selected.querySelectorAll('button')]
                    .filter(button => visible(button) && text(button) === sign);
                if (buttons.length !== 1) throw Error('Ambiguous selected product quantity controls');
            }
            const control = document.evaluate(arguments[3], selected, null,
                XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
            if (!control) return null;
            return [text(original), text(current), text(quantity), text(discount), text(total),
                control.value || control.querySelector('input')?.value || text(control)];
            """;

    public record PriceSummary(BigDecimal originalUnitPrice, BigDecimal discountedUnitPrice,
                               int quantity, BigDecimal discount, BigDecimal grandTotal) {
        public boolean hasExpectedArithmetic(int expectedQuantity) {
            BigDecimal count = BigDecimal.valueOf(expectedQuantity);
            return quantity == expectedQuantity && discountedUnitPrice.signum() > 0
                    && originalUnitPrice.compareTo(discountedUnitPrice) >= 0
                    && grandTotal.compareTo(discountedUnitPrice.multiply(count)) == 0
                    && discount.abs().compareTo(originalUnitPrice.subtract(discountedUnitPrice).multiply(count)) == 0;
        }
    }

    public PriceSummary waitForPriceSummary(String productSlug, String productName, int expectedQuantity) {
        String selectedRow = productRow(productSlug).toString().substring("By.xpath: ".length());
        PriceSummary[] previous = {null};
        String[] lastState = {"Summary not yet available"};
        try {
            return wait.until(driver -> {
                List<?> values;
                try {
                    values = (List<?>) ((JavascriptExecutor) driver).executeScript(
                            SUMMARY_SNAPSHOT, productName, CART_CONTROL_ROWS, selectedRow,
                            ".//button[normalize-space() = '-']/following-sibling::*[1]");
                } catch (org.openqa.selenium.JavascriptException e) {
                    if (e.getRawMessage().contains("found 0")) {
                        lastState[0] = e.getRawMessage();
                        previous[0] = null;
                        return null;
                    }
                    throw new IllegalStateException("Expected a single-product cart with no extra charges: "
                            + e.getRawMessage(), e);
                }
                if (values == null) {
                    previous[0] = null;
                    return null;
                }
                PriceSummary snapshot;
                int controlQuantity;
                try {
                    snapshot = new PriceSummary(PriceUtils.parseAzn((String) values.get(0)),
                            PriceUtils.parseAzn((String) values.get(1)), summaryQuantity((String) values.get(2)),
                            PriceUtils.parseAzn((String) values.get(3)), PriceUtils.parseAzn((String) values.get(4)));
                    controlQuantity = Integer.parseInt((String) values.get(5));
                } catch (IllegalArgumentException e) {
                    lastState[0] = "Incomplete/malformed summary amounts: " + values;
                    previous[0] = null;
                    return null;
                }
                lastState[0] = snapshot + ", control quantity=" + controlQuantity;
                boolean consistent = controlQuantity == expectedQuantity
                        && snapshot.hasExpectedArithmetic(expectedQuantity);
                boolean ready = consistent && snapshot.equals(previous[0]);
                previous[0] = consistent ? snapshot : null;
                return ready ? snapshot : null;
            });
        } catch (TimeoutException e) {
            throw new TimeoutException("Cart summary did not settle at quantity " + expectedQuantity
                    + " with discounted unit × quantity total and (original - discounted) × quantity discount. "
                    + "Unexpected charges or inconsistent prices/quantities: " + lastState[0], e);
        }
    }

    private static int summaryQuantity(String text) {
        if (!text.matches("\\s*[0-9]+\\s*(?:ədəd)?\\s*")) {
            throw new IllegalStateException("Expected a summary unit count, but found: " + text);
        }
        return Integer.parseInt(text.replace("ədəd", "").trim());
    }

    public CartPage(WebDriver driver) {
        this(driver, WAIT_TIMEOUT);
    }

    public CartPage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, timeout);
    }


    public CartPage waitForCartPage() {
        try {
            wait.until(driver -> {
                try {
                    String path = java.net.URI.create(driver.getCurrentUrl()).getPath();
                    return CART_PAGE_PATH.equals(path) || (CART_PAGE_PATH + "/").equals(path);
                } catch (IllegalArgumentException exception) {
                    return false;
                }
            });
        } catch (TimeoutException exception) {
            throw new TimeoutException("Cart navigation did not reach the /sebet route within the bounded wait.");
        }
        return this;
    }


    public void openCart() {
        boolean[] closeRequested = {false};
        String[] state = {"waiting for campaign controls and cart link"};
        WebElement cartLink;
        try {
            cartLink = wait.until(driver -> {
                for (WebElement close : driver.findElements(CAMPAIGN_CLOSE)) {
                    if (close.isDisplayed()) {
                        state[0] = "campaign close control visible; close requested=" + closeRequested[0];
                        if (!closeRequested[0] && close.isEnabled()) {

                            closeRequested[0] = true;
                            close.click();
                        }
                        return null;
                    }
                }


                if (!driver.findElements(MODAL_BACKDROP).isEmpty()) {
                    state[0] = "modal backdrop still present; close requested=" + closeRequested[0];
                    return null;
                }

                List<WebElement> links = driver.findElements(CART_NAVIGATION);
                state[0] = links.isEmpty() ? "no /sebet link in DOM (header not ready or locator changed)"
                        : "cart links present but hidden or disabled";
                for (WebElement link : links) {
                    if (link.isDisplayed() && link.isEnabled()) return link;
                }
                return null;
            });
        } catch (TimeoutException exception) {

            throw new TimeoutException("Cart navigation blocked before click: " + state[0]);
        }
        cartLink.click();
        waitForCartPage();
    }


    public boolean isProductVisible() {
        return waitForProductVisibility(CART_PRODUCT);
    }

    public boolean isProductVisible(String productSlug) {
        return waitForProductVisibility(productLink(productSlug));
    }

    private boolean waitForProductVisibility(By product) {

        return wait.until(driver -> driver.findElement(product).isDisplayed());
    }

    public int waitForProductQuantity(String productSlug, int expectedQuantity) {
        if (expectedQuantity < 1) {
            throw new IllegalArgumentException("Expected a positive cart quantity.");
        }
        By rowLocator = productRow(productSlug);
        return wait.until(driver -> {


            WebElement row = driver.findElement(rowLocator);
            if (!row.isDisplayed()) {
                return null;
            }
            WebElement quantity = row.findElement(PRODUCT_QUANTITY);
            Integer actual = quantity.isDisplayed() ? readQuantity(quantity) : null;
            return Integer.valueOf(expectedQuantity).equals(actual) ? actual : null;
        });
    }


    public int getProductQuantity() {
        return waitForProductQuantity();
    }

    public int getProductQuantity(String productSlug) {
        return waitForProductQuantity(productRow(productSlug));
    }


    public void increaseQuantity() {
        increaseQuantity(CART_PRODUCT_ROW);
    }


    public void decreaseQuantity() {
        waitForCartControl(DECREASE_QUANTITY_BUTTON).click();
    }

    public void increaseQuantity(String productSlug) {
        mutate("increase", productSlug, productRow(productSlug), INCREASE_QUANTITY_BUTTON, 1);
    }

    private void increaseQuantity(By rowLocator) {
        mutate("increase", "legacy-first-product", rowLocator, INCREASE_QUANTITY_BUTTON, 1);
    }

    public void removeProduct(String productSlug) {
        mutate("removal", productSlug, productRow(productSlug), REMOVE_PRODUCT_BUTTON, 0);
    }

    public void decreaseQuantity(String productSlug) {
        mutate("decrement", productSlug, productRow(productSlug), DECREASE_QUANTITY_BUTTON, -1);
    }


    public String failureState(String productSlug) {
        By row = productRow(productSlug);
        String presence = "unknown", empty = "unknown", quantity = "unknown";
        try { presence = Boolean.toString(!driver.findElements(productLink(productSlug)).isEmpty()); }
        catch (WebDriverException ignored) { }
        try { empty = Boolean.toString(driver.findElements(EMPTY_CART_MESSAGE_LOCATOR)
                .stream().anyMatch(WebElement::isDisplayed)); }
        catch (WebDriverException ignored) { }
        try { quantity = String.valueOf(readQuantity(driver.findElement(row).findElement(PRODUCT_QUANTITY))); }
        catch (WebDriverException | NumberFormatException ignored) { }
        return "target=" + productSlug + "; currentQuantity=" + quantity + "; productPresent=" + presence
                + "; emptyCart=" + empty + "\nLast mutation: " + lastMutation;
    }

    private static final class MutationState {
        final String operation, target;
        Integer before, last, stable;
        Boolean present, empty;
        boolean attempted, returned, staleClick;
        String readiness = "not observed";
        RuntimeException underlying;
        MutationState(String operation, String target) {
            this.operation = operation;
            this.target = target;
        }
        String describe() {
            return "operation=" + operation + "; target=" + target + "; preClickQuantity=" + before
                    + "; readiness=" + readiness + "; clickAttempted=" + attempted
                    + "; clickReturned=" + returned + "; clickThrewStale=" + staleClick
                    + "; lastQuantity=" + last + "; productPresent=" + present + "; emptyCart=" + empty
                    + "; underlying=" + (underlying == null ? "none" : underlying.getClass().getSimpleName());
        }
    }


    private static RuntimeException safeCause(RuntimeException original) {
        if (original == null) return null;
        RuntimeException safe = new RuntimeException("Underlying condition: " + original.getClass().getSimpleName());
        safe.setStackTrace(original.getStackTrace());
        return safe;
    }

    private void mutate(String operation, String slug, By rowLocator, By control, int delta) {
        boolean removal = delta == 0;
        MutationState state = removal ? pendingRemovals.getOrDefault(slug, new MutationState(operation, slug))
                : new MutationState(operation, slug);
        try {
            wait.until(active -> {
                try {

                    if (state.attempted && (removal || state.before != null && state.before + delta == 0)) {
                        state.present = !active.findElements(productLink(slug)).isEmpty();

                        state.empty = active.findElements(EMPTY_CART_MESSAGE_LOCATOR).stream()
                                .anyMatch(WebElement::isDisplayed);
                        state.last = state.present ? null : 0;
                        if (!state.present && state.empty) return true;
                        if (!state.present) return false;
                    }
                    WebElement row = active.findElement(rowLocator);
                    state.present = true;
                    state.empty = active.findElements(EMPTY_CART_MESSAGE_LOCATOR).stream()
                            .anyMatch(WebElement::isDisplayed);
                    if (!row.isDisplayed()) { state.readiness = "row hidden"; state.stable = null; return false; }
                    WebElement quantity = row.findElement(PRODUCT_QUANTITY);
                    state.last = quantity.isDisplayed() ? readQuantity(quantity) : null;
                    if (state.last == null || state.last < 1) {
                        state.readiness = "positive quantity unavailable"; state.stable = null; return false;
                    }
                    if (state.attempted) {
                        if (!removal && state.before != null && state.last == state.before + delta) return true;
                        if (!removal && state.before != null && !state.last.equals(state.before)) {
                            throw new IllegalStateException("Unexpected cart quantity; " + state.describe());
                        }
                        return false;
                    }
                    WebElement button = row.findElement(control);
                    boolean ready = button.isDisplayed() && button.isEnabled()
                            && !state.empty
                            && !"true".equals(button.getDomAttribute("aria-disabled"))
                            && !"true".equals(row.getDomAttribute("aria-busy"))
                            && active.findElements(MUTATION_BLOCKERS).isEmpty();
                    state.readiness = ready ? "controls clear; waiting for stable quantity" : "control busy/disabled or modal present";
                    boolean stable = ready && state.last.equals(state.stable);
                    state.stable = ready ? state.last : null;
                    if (!stable) return false;

                    state.before = state.last;
                    state.readiness = "ready; stable quantity across fresh polls";
                    state.attempted = true;
                    if (removal) pendingRemovals.put(slug, state);
                    try {
                        button.click();
                        state.returned = true;
                    } catch (StaleElementReferenceException stale) {
                        state.staleClick = true;
                        state.underlying = stale;
                    }
                    return false;
                } catch (StaleElementReferenceException | NoSuchElementException transientRead) {
                    state.underlying = transientRead;
                    state.stable = null;
                    return false;
                } catch (NumberFormatException malformedQuantity) {
                    state.underlying = malformedQuantity;
                    state.last = null;
                    state.stable = null;
                    state.readiness = "quantity not numeric";
                    return false;
                }
            });
            if (removal) pendingRemovals.remove(slug);
        } catch (TimeoutException timeout) {

            lastMutation = state.describe();
            throw new TimeoutException("Cart " + operation + " was not confirmed; product still present or empty cart not confirmed. "
                    + "No additional " + operation + " was sent. " + lastMutation,
                    safeCause(state.underlying == null ? timeout : state.underlying));
        } catch (WebDriverException failure) {
            state.underlying = failure;
            lastMutation = state.describe();
            throw new WebDriverException("Cart mutation failed. " + lastMutation, safeCause(failure));
        } finally {
            lastMutation = state.describe();
        }
    }

    public void waitForProductToDisappear(String productSlug) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(productLink(productSlug)));
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
        return wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement emptyCartMessage = driver.findElement(EMPTY_CART_MESSAGE_LOCATOR);
            return emptyCartMessage.isDisplayed() ? emptyCartMessage : null;
        })).isDisplayed();
    }

    private int waitForProductQuantity() {
        return waitForProductQuantity(CART_PRODUCT_ROW);
    }

    private int waitForProductQuantity(By rowLocator) {
        return wait.until(ExpectedConditions.refreshed(driver -> {
            WebElement productRow = driver.findElement(rowLocator);
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
        return By.xpath(productLinkExpression(productSlug));
    }

    private By productRow(String productSlug) {
        return By.xpath(CART_CONTROL_ROWS + "[.//a[" + productHrefPredicate(productSlug) + "]]");
    }

    private String productLinkExpression(String productSlug) {
        return "//main//a[" + productHrefPredicate(productSlug) + "]";
    }

    private String productHrefPredicate(String productSlug) {
        if (productSlug == null || !productSlug.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Expected a product slug containing letters, digits, underscores or hyphens.");
        }
        String route = PRODUCT_ROUTE_FRAGMENT + productSlug;
        return "@href='" + route + "' or @href='" + route + "/'"
                + " or substring(@href, string-length(@href) - " + (route.length() - 1)
                + ")='" + route + "'";
    }
}
