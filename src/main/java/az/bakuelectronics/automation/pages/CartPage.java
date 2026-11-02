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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interactive;

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
    private static final By EMPTY_CART_MESSAGE_LOCATOR = By.xpath(
            "//main//div[normalize-space() = '" + EMPTY_CART_MESSAGE + "']");
    private static final By CART_NAVIGATION = By.xpath(
            "//a[@href='/sebet' or @href='/sebet/']");
    private static final By MODAL_BACKDROP = By.cssSelector(".modal-backdrop");
    private static final By CAMPAIGN_CLOSE = By.cssSelector("button.campaignModalClose");
    private static final By CART_HOVER_CONTROLS = By.cssSelector(
            "[class^='header_basketHover__btn__'], [class*=' header_basketHover__btn__']");

    private final WaitUtils wait;
    private final WebDriver driver;
    private String lastMutation = "No cart mutation recorded";
    private static final By MUTATION_BLOCKERS = By.cssSelector(
            ".modal-backdrop, .campaignModal.modal.show, main [aria-busy='true']");

    private final Map<String, MutationState> pendingRemovals = new HashMap<>();

    private static final String INVENTORY_SCRIPT = """
            const visible = e => !!e.getClientRects().length;
            const result = document.evaluate(arguments[0], document, null,
                XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
            const rows = Array.from({length: result.snapshotLength}, (_, i) => result.snapshotItem(i)).filter(visible);
            const items = rows.map(row => {
                const slugs = [...new Set([...row.querySelectorAll('a[href]')].map(a =>
                    new URL(a.href).pathname.match(/^\\/mehsul\\/([a-zA-Z0-9_-]+)\\/?$/)?.[1]).filter(Boolean))];
                if (slugs.length !== 1) throw Error('Ambiguous cart row identity');
                const minus = [...row.querySelectorAll('button')].filter(e => visible(e) && e.innerText.trim() === '-');
                const plus = [...row.querySelectorAll('button')].filter(e => visible(e) && e.innerText.trim() === '+');
                if (minus.length !== 1 || plus.length !== 1) throw Error('Ambiguous cart row controls');
                const count = minus[0].nextElementSibling;
                if (!count) throw Error('Missing quantity');
                return [slugs[0], count.value || count.querySelector('input')?.value || count.innerText.trim()];
            });
            const empty = [...document.querySelectorAll('main div')].some(e => visible(e)
                && e.innerText.trim() === arguments[1]);
            return [empty, items];
            """;

    /** Complete control-row inventory; empty state requires the observed empty-cart message. */
    public Map<String, Integer> waitForInventory() {
        Map<?, ?>[] previous = {null};
        long[] since = {0};
        return wait.until(active -> {
            Map<String, Integer> current = readInventory();
            if (current == null) { previous[0] = null; return null; }
            if (!current.equals(previous[0])) {
                previous[0] = current;
                since[0] = System.nanoTime();
                return null;
            }
            return System.nanoTime() - since[0] >= 1_200_000_000L ? current : null;
        });
    }

    private Map<String, Integer> readInventory() {
        List<?> raw = (List<?>) ((JavascriptExecutor) driver).executeScript(
                INVENTORY_SCRIPT, CART_CONTROL_ROWS, EMPTY_CART_MESSAGE);
        List<?> items = (List<?>) raw.get(1);
        if (items.isEmpty()) return Boolean.TRUE.equals(raw.get(0)) ? Map.of() : null;
        if (Boolean.TRUE.equals(raw.get(0))) return null;
        Map<String, Integer> result = new HashMap<>();
        for (Object value : items) {
            List<?> row = (List<?>) value;
            int count = Integer.parseInt((String) row.get(1));
            if (count < 1 || result.putIfAbsent((String) row.get(0), count) != null)
                throw new IllegalStateException("Duplicate cart identity or nonpositive quantity");
        }
        return Map.copyOf(result);
    }

    private static final String MULTI_SUMMARY_SCRIPT = """
            const visible = e => !!e.getClientRects().length;
            const all = (root, prefix) => [...root.querySelectorAll(
                `[class^='${prefix}'], [class*=' ${prefix}']`)].filter(visible);
            const one = (root, prefix) => {
                const found = all(root, prefix);
                if (found.length !== 1) throw Error(`Expected one ${prefix}, found ${found.length}`);
                return found[0];
            };
            const text = e => e.innerText.trim();
            const normalize = s => s.replace(/[\\s\\u00a0]+/g, ' ').trim();
            const main = document.querySelector('main');
            if (!main) return null;
            const summary = one(main, 'sebet_BasketRight__');
            const productRows = all(summary, 'sebet_BasketRightBodyItem__');
            const evaluated = document.evaluate(arguments[0], document, null,
                XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
            const controls = Array.from({length: evaluated.snapshotLength}, (_, i) => evaluated.snapshotItem(i)).filter(visible);
            if (!controls.length || productRows.length !== controls.length) return null;
            const fields = [];
            const names = new Set();
            const items = controls.map(row => {
                const links = [...row.querySelectorAll('a[href]')];
                const slugs = [...new Set(links.map(a => new URL(a.href).pathname
                    .match(/^\\/mehsul\\/([a-zA-Z0-9_-]+)\\/?$/)?.[1]).filter(Boolean))];
                const titles = [...new Set(links.filter(visible).map(a => normalize(text(a))).filter(Boolean))];
                if (slugs.length !== 1 || titles.length !== 1) throw Error('Ambiguous control row identity/name');
                const title = titles[0];
                if (names.has(title)) throw Error('Summary names are not unique; cannot map identity safely');
                names.add(title);
                const matches = productRows.filter(r => normalize(text(one(r, 'sebet_ItemName__'))) === title);
                if (matches.length !== 1) throw Error('Cannot map summary to exact product name');
                const target = matches[0];
                const name = one(target, 'sebet_ItemName__');
                const old = one(target, 'sebet_BasketRightBodyItemPriceOld__');
                const current = one(target, 'sebet_BasketRightBodyItemPriceCurrent__');
                const quantity = one(target, 'sebet_ItemCount__');
                fields.push(name, old, current, quantity);
                const minus = [...row.querySelectorAll('button')].filter(b => visible(b) && text(b) === '-');
                const plus = [...row.querySelectorAll('button')].filter(b => visible(b) && text(b) === '+');
                if (minus.length !== 1 || plus.length !== 1) throw Error('Ambiguous quantity controls');
                const count = minus[0].nextElementSibling;
                return [slugs[0], title, text(old), text(current), text(quantity),
                    count.value || count.querySelector('input')?.value || text(count)];
            });
            const discount = one(summary, 'sebet_BasketRightDiscountPrice__');
            const total = one(summary, 'sebet_TotalTopPrice__');
            fields.push(discount, total);
            const copy = summary.cloneNode(true);
            const originals = [summary, ...summary.querySelectorAll('*')];
            const copies = [copy, ...copy.querySelectorAll('*')];
            fields.forEach(e => copies[originals.indexOf(e)].remove());
            originals.filter(e => !visible(e)).forEach(e => copies[originals.indexOf(e)].remove());
            if (/\\d/.test(copy.textContent)) throw Error('Unsupported charge/amount in cart summary');
            return [items, text(discount), text(total)];
            """;

    public CartSnapshot waitForMultiProductSummary(Map<String, String> names, Map<String, Integer> quantities) {
        if (names.isEmpty() || !names.keySet().equals(quantities.keySet()))
            throw new IllegalArgumentException("Expected complete identities and quantities");
        CartSnapshot[] previous = {null};
        String[] last = {"No complete snapshot"};
        try {
            return wait.until(active -> {
                List<?> raw = (List<?>) ((JavascriptExecutor) active).executeScript(MULTI_SUMMARY_SCRIPT, CART_CONTROL_ROWS);
                if (raw == null) { previous[0] = null; return null; }
                CartSnapshot current;
                try { current = CartSnapshot.parse(raw); }
                catch (IllegalArgumentException incomplete) {
                    last[0] = incomplete.getMessage(); previous[0] = null; return null;
                }
                last[0] = current.toString();
                boolean matches = current.rows().keySet().equals(names.keySet())
                        && current.rows().entrySet().stream().allMatch(e ->
                        e.getValue().name().equals(CartSnapshot.normalize(names.get(e.getKey())))
                                && Integer.valueOf(e.getValue().quantity()).equals(quantities.get(e.getKey())))
                        && current.hasExpectedArithmetic();
                boolean settled = matches && current.equals(previous[0]);
                previous[0] = matches ? current : null;
                return settled ? current : null;
            });
        } catch (TimeoutException failure) {
            throw new TimeoutException("Multi-product summary did not settle: " + last[0], failure);
        }
    }

    /** Read the complete displayed basket without assuming that the account starts empty. */
    public CartSnapshot waitForAccountSnapshot() {
        CartSnapshot[] previous = {null};
        return wait.until(active -> {
            List<?> raw = (List<?>) ((JavascriptExecutor) active).executeScript(MULTI_SUMMARY_SCRIPT, CART_CONTROL_ROWS);
            if (raw == null) { previous[0] = null; return null; }
            CartSnapshot current = CartSnapshot.parse(raw);
            boolean settled = current.equals(previous[0]) && current.hasExpectedArithmetic();
            previous[0] = current;
            return settled ? current : null;
        });
    }

    // Expected inputs come exclusively from the main product cards, outside BasketRight.
    private static final String PRODUCT_INPUTS_SCRIPT = """
            const visible = e => !!e.getClientRects().length;
            const one = (root, prefix) => {
                const found = [...root.querySelectorAll(`[class^='${prefix}'], [class*=' ${prefix}']`)].filter(visible);
                if (found.length > 1) throw Error('Ambiguous main-row price');
                return found[0];
            };
            const result = document.evaluate(arguments[0], document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
            const rows = Array.from({length: result.snapshotLength}, (_, i) => result.snapshotItem(i)).filter(visible);
            if (!rows.length) return null;
            const items = [];
            for (const row of rows) {
                const links = [...row.querySelectorAll('a[href]')];
                const slugs = [...new Set(links.map(a => new URL(a.href).pathname
                    .match(/^\\/mehsul\\/([a-zA-Z0-9_-]+)\\/?$/)?.[1]).filter(Boolean))];
                const names = [...new Set(links.filter(visible).map(a => a.innerText.replace(/[\\s\\u00a0]+/g, ' ').trim()).filter(Boolean))];
                if (slugs.length !== 1 || names.length !== 1) throw Error('Ambiguous main-row identity');
                const minus = [...row.querySelectorAll('button')].filter(b => visible(b) && b.innerText.trim() === '-');
                const plus = [...row.querySelectorAll('button')].filter(b => visible(b) && b.innerText.trim() === '+');
                if (minus.length !== 1 || plus.length !== 1) throw Error('Ambiguous main-row controls');
                const count = minus[0].nextElementSibling;
                const old = one(row, 'sebet_BasketProductsItemPriceOld__');
                const current = one(row, 'sebet_BasketProductsItemPriceCurrent__');
                if (!count || !visible(count) || !old || !current) return null;
                items.push([slugs[0], names[0], old.innerText.trim(), current.innerText.trim(),
                    count.value || count.querySelector('input')?.value || count.innerText.trim()]);
            }
            return items;
            """;

    /** Independently scoped expected inputs; this reader never reads summary monetary fields. */
    public Map<String, CartSnapshot.Row> waitForDisplayedProductInputs() {
        Map<?, ?>[] previous = {null};
        return wait.until(active -> {
            List<?> raw = (List<?>) ((JavascriptExecutor) active).executeScript(PRODUCT_INPUTS_SCRIPT, CART_CONTROL_ROWS);
            if (raw == null) { previous[0] = null; return null; }
            Map<String, CartSnapshot.Row> rows = new HashMap<>();
            java.util.Set<String> names = new java.util.HashSet<>();
            for (Object item : raw) {
                if (!(item instanceof List<?> fields) || fields.size() != 5
                        || fields.stream().anyMatch(value -> !(value instanceof String)))
                    throw new IllegalArgumentException("Incomplete main-row input");
                String slug = (String) fields.get(0), name = CartSnapshot.normalize((String) fields.get(1));
                String quantity = ((String) fields.get(4)).trim();
                if (!slug.matches("[a-zA-Z0-9_-]+") || name.isBlank() || !names.add(name)
                        || !quantity.matches("[0-9]+"))
                    throw new IllegalArgumentException("Ambiguous main-row identity or quantity");
                CartSnapshot.Row row = new CartSnapshot.Row(slug, name, PriceUtils.parseAzn((String) fields.get(2)),
                        PriceUtils.parseAzn((String) fields.get(3)), Integer.parseInt(quantity));
                if (row.quantity() < 1 || row.discountedUnitPrice().signum() <= 0
                        || row.originalUnitPrice().compareTo(row.discountedUnitPrice()) < 0
                        || rows.putIfAbsent(slug, row) != null)
                    throw new IllegalArgumentException("Invalid or duplicate main-row input");
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("Expected nonempty main-row inputs");
            Map<String, CartSnapshot.Row> current = Map.copyOf(rows);
            boolean settled = current.equals(previous[0]);
            previous[0] = current;
            return settled ? current : null;
        });
    }

    private static final String VISIBLE_TOTALS_SCRIPT = """
            const visible = e => !!e.getClientRects().length;
            const all = (root, prefix) => [...root.querySelectorAll(`[class^='${prefix}'], [class*=' ${prefix}']`)].filter(visible);
            const one = (root, prefix) => {
                const found = all(root, prefix);
                if (found.length > 1) throw Error('Ambiguous visible summary field');
                return found[0];
            };
            const main = document.querySelector('main');
            if (!main) return null;
            const summary = one(main, 'sebet_BasketRight__');
            if (!summary) return null;
            const products = all(summary, 'sebet_BasketRightBodyItem__');
            const discount = one(summary, 'sebet_BasketRightDiscountPrice__');
            const total = one(summary, 'sebet_TotalTopPrice__');
            if (!products.length || !discount || !total) return null;
            // Do not infer charges as zero. An additional displayed amount requires a reviewed contract.
            const clone = summary.cloneNode(true);
            const originals = [summary, ...summary.querySelectorAll('*')];
            const copies = [clone, ...clone.querySelectorAll('*')];
            [...products, discount, total].forEach(e => copies[originals.indexOf(e)].remove());
            originals.filter(e => !visible(e)).forEach(e => copies[originals.indexOf(e)].remove());
            if (/\\d/.test(clone.textContent)) throw Error('Unsupported displayed summary charge or amount');
            return [discount.innerText.trim(), total.innerText.trim()];
            """;

    public record DisplayedSummaryTotals(BigDecimal discount, BigDecimal total) { }

    /** Raw visible RHS amounts, read separately from the main cards; no calculated acceptance predicate. */
    public DisplayedSummaryTotals waitForDisplayedSummaryTotals() {
        DisplayedSummaryTotals[] previous = {null};
        return wait.until(active -> {
            List<?> raw = (List<?>) ((JavascriptExecutor) active).executeScript(VISIBLE_TOTALS_SCRIPT);
            if (raw == null) { previous[0] = null; return null; }
            if (raw.size() != 2 || raw.stream().anyMatch(value -> !(value instanceof String)))
                throw new IllegalArgumentException("Incomplete visible summary totals");
            var current = new DisplayedSummaryTotals(PriceUtils.parseAzn((String) raw.get(0)),
                    PriceUtils.parseAzn((String) raw.get(1)));
            if (current.total().signum() <= 0) throw new IllegalArgumentException("Invalid nonempty summary total");
            boolean settled = current.equals(previous[0]);
            previous[0] = current;
            return settled ? current : null;
        });
    }

    /** One removal command; confirmation includes every surviving row and quantity. */
    public void removeOwnedProduct(String slug) {
        MutationState pending = pendingRemovals.get(slug);
        Map<String, Integer> remaining;
        if (pending != null) {
            if (pending.expectedRemaining == null)
                throw new IllegalStateException("Cannot change an unconfirmed legacy removal into an owned removal");
            remaining = pending.expectedRemaining;
        } else {
            Map<String, Integer> before = waitForInventory();
            if (!before.containsKey(slug)) throw new IllegalStateException("Owned product is absent before removal");
            remaining = new HashMap<>(before);
            remaining.remove(slug);
        }
        mutate("owned removal", slug, productRow(slug), REMOVE_PRODUCT_BUTTON, 0, Map.copyOf(remaining));
        if (!waitForInventory().equals(remaining))
            throw new IllegalStateException("Removal changed surviving products");
        pendingRemovals.remove(slug);
    }



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
        // The click leaves the pointer over the header and keeps its mini-cart above the first row.
        // Use a real pointer move, then wait for the observed hover controls to disappear.
        if (driver instanceof Interactive) {
            new Actions(driver).moveToLocation(1, 1).perform();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(CART_HOVER_CONTROLS));
        }
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
        Map<String, Integer> expectedRemaining;
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
        mutate(operation, slug, rowLocator, control, delta, null);
    }

    private void mutate(String operation, String slug, By rowLocator, By control, int delta,
                        Map<String, Integer> expectedRemaining) {
        boolean removal = delta == 0;
        MutationState state = removal ? pendingRemovals.getOrDefault(slug, new MutationState(operation, slug))
                : new MutationState(operation, slug);
        if (!state.attempted) state.expectedRemaining = expectedRemaining;
        try {
            wait.until(active -> {
                try {

                    if (state.attempted && (removal || state.before != null && state.before + delta == 0)) {
                        Map<String, Integer> inventory = state.expectedRemaining == null ? null : readInventory();
                        state.present = inventory == null ? !active.findElements(productLink(slug)).isEmpty()
                                : inventory.containsKey(slug);

                        state.empty = active.findElements(EMPTY_CART_MESSAGE_LOCATOR).stream()
                                .anyMatch(WebElement::isDisplayed);
                        state.last = state.present ? null : 0;
                        if (!state.present && (state.expectedRemaining == null ? state.empty
                                : state.expectedRemaining.equals(inventory))) return true;
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
                    WebElement button;
                    if (state.expectedRemaining != null) {
                        List<WebElement> candidates = row.findElements(control).stream()
                                .filter(WebElement::isDisplayed).toList();
                        if (candidates.size() != 1)
                            throw new IllegalStateException("Expected exactly one visible owned-product removal control");
                        button = candidates.get(0);
                    } else {
                        button = row.findElement(control);
                    }
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
            // Owned removal remains pending until the caller also verifies a settled inventory.
            if (removal && state.expectedRemaining == null) pendingRemovals.remove(slug);
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
                + ")='" + route + "'"
                + " or substring(@href, string-length(@href) - " + route.length()
                + ")='" + route + "/'";
    }
}
