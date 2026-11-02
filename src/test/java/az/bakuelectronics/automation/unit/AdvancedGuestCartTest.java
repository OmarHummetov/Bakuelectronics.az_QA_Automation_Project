package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.CartSnapshot;
import az.bakuelectronics.automation.support.OwnedGuestCart;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvancedGuestCartTest {
    private static List<String> row(String slug, String name, String old, String price, String qty, String control) {
        return List.of(slug, name, old, price, qty, control);
    }
    private static List<?> raw() {
        return List.of(List.of(row("a", "Product A", "25.99", "20.99", "2 ədəd", "2"),
                row("b", "Product B", "10.00", "9.50", "1", "1")), "-10.50", "51.48");
    }

    @Test public void parsesTwoRowsAndExactCentArithmetic() {
        CartSnapshot snapshot = CartSnapshot.parse(raw());
        Assert.assertTrue(snapshot.hasExpectedArithmetic());
        Assert.assertEquals(snapshot.rows().get("a").calculatedTotal(), new BigDecimal("41.98"));
        Assert.assertEquals(snapshot.rows().get("b").calculatedTotal(), new BigDecimal("9.50"));
        Assert.expectThrows(UnsupportedOperationException.class, () -> snapshot.rows().clear());
    }

    @Test public void discountIsNotSubtractedTwiceAndNoFeeIsAssumed() {
        CartSnapshot snapshot = CartSnapshot.parse(raw());
        Assert.assertFalse(new CartSnapshot(snapshot.rows(), snapshot.discount(), new BigDecimal("40.98")).hasExpectedArithmetic());
        Assert.assertFalse(new CartSnapshot(snapshot.rows(), snapshot.discount(), new BigDecimal("52.48")).hasExpectedArithmetic());
        Assert.assertFalse(new CartSnapshot(snapshot.rows(), new BigDecimal("10.49"), snapshot.total()).hasExpectedArithmetic());
    }

    @DataProvider public Object[][] invalidRows() {
        return new Object[][] {
            {List.of()},
            {List.of(row("a", "A", "10", "9", "1", "1"), row("a", "B", "10", "9", "1", "1"))},
            {List.of(row("a", "A", "10", "9", "2", "1"))},
            {List.of(row("a", "A", "10", "9", "0", "0"))},
            {List.of(row("a", "A", "8", "9", "1", "1"))},
            {List.of(row("a", "A", "10", "0", "1", "1"))},
            {List.of(row("a", " ", "10", "9", "1", "1"))},
            {List.of(row("a/b", "A", "10", "9", "1", "1"))},
            {List.of(row("a", "A", "10", "9", "1 item", "1"))},
            {List.of(row("a", "A", "10", "9.999", "1", "1"))}
        };
    }
    @Test(dataProvider = "invalidRows") public void rejectsMalformedDuplicateOrMixedRows(List<?> rows) {
        Assert.expectThrows(IllegalArgumentException.class, () -> CartSnapshot.parse(List.of(rows, "1", "9")));
    }

    private static WebDriver scriptDriver(java.util.function.Supplier<Object> callback) {
        return (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeScript")) return callback.get();
                    throw new AssertionError("Unexpected driver call: " + method.getName());
                });
    }

    @Test public void waitsThroughMixedSummaryAndRequiresTwoEqualReads() {
        AtomicInteger polls = new AtomicInteger();
        CartPage page = new CartPage(scriptDriver(() -> {
            if (polls.incrementAndGet() == 1) return List.of(((List<?>) raw().get(0)), "-5.25", "51.48");
            return raw();
        }), Duration.ofSeconds(3));
        Assert.assertTrue(page.waitForMultiProductSummary(Map.of("a", "Product A", "b", "Product B"),
                Map.of("a", 2, "b", 1)).hasExpectedArithmetic());
        Assert.assertEquals(polls.get(), 3);
    }

    @Test public void unexpectedIdentityDoesNotBecomeSuccess() {
        CartPage page = new CartPage(scriptDriver(AdvancedGuestCartTest::raw), Duration.ofMillis(20));
        Assert.expectThrows(TimeoutException.class, () -> page.waitForMultiProductSummary(
                Map.of("a", "Product A"), Map.of("a", 2)));
    }

    @Test public void duplicateInventoryIsRejected() {
        CartPage page = new CartPage(scriptDriver(() -> List.of(false,
                List.of(List.of("a", "1"), List.of("a", "1")))), Duration.ofMillis(20));
        Assert.expectThrows(IllegalStateException.class, page::waitForInventory);
    }

    @Test public void noRowsWithoutEmptyMessageIsNotAnEmptyBaseline() {
        CartPage page = new CartPage(scriptDriver(() -> List.of(false, List.of())), Duration.ofMillis(20));
        Assert.expectThrows(TimeoutException.class, page::waitForInventory);
    }

    @Test public void inventoryXPathSelectsTwoRowsDespiteDuplicateLinksAndSummaryCopies() throws Exception {
        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new org.xml.sax.InputSource(new java.io.StringReader("""
                <html><main><section><div><a href='/mehsul/a'>A</a><a href='/mehsul/a'>image</a>
                <div><button>-</button><span>2</span><button>+</button></div></div>
                <div><a href='/mehsul/b'>B</a><div><button>-</button><span>1</span><button>+</button></div></div></section>
                <aside class='sebet_BasketRight__hash'><div><a href='/mehsul/a'>A</a><button>-</button><button>+</button></div></aside>
                <a href='/mehsul/recommendation'>Recommendation without cart controls</a></main></html>
                """)));
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    Assert.assertEquals(method.getName(), "executeScript");
                    Object[] scriptArgs = (Object[]) args[1];
                    var selected = (org.w3c.dom.NodeList) javax.xml.xpath.XPathFactory.newInstance().newXPath()
                            .evaluate((String) scriptArgs[0], document, javax.xml.xpath.XPathConstants.NODESET);
                    Assert.assertEquals(selected.getLength(), 2);
                    Assert.assertEquals(selected.item(0).getFirstChild().getAttributes().getNamedItem("href").getNodeValue(), "/mehsul/a");
                    Assert.assertEquals(selected.item(1).getFirstChild().getAttributes().getNamedItem("href").getNodeValue(), "/mehsul/b");
                    return List.of(false, List.of(List.of("a", "2"), List.of("b", "1")));
                });
        Assert.assertEquals(new CartPage(driver, Duration.ofSeconds(3)).waitForInventory(), Map.of("a", 2, "b", 1));
    }

    @Test public void cleansTwoOwnedProductsAndVerifiesBaseline() {
        FakeCart cart = new FakeCart();
        OwnedGuestCart.withEmptyCart(cart, owned -> {
            owned.add("a", () -> cart.items.put("a", 1));
            owned.add("b", () -> cart.items.put("b", 1));
            cart.items.put("a", 2);
        });
        Assert.assertEquals(cart.removed, List.of("a", "b"));
        Assert.assertTrue(cart.items.isEmpty());
    }

    @Test public void refusesForeignBaselineWithoutMutation() {
        FakeCart cart = new FakeCart();
        cart.items.put("foreign", 3);
        Assert.expectThrows(IllegalStateException.class, () -> OwnedGuestCart.withEmptyCart(cart, owned -> Assert.fail()));
        Assert.assertEquals(cart.items, Map.of("foreign", 3));
        Assert.assertTrue(cart.removed.isEmpty());
    }

    @Test public void ownsBeforeAmbiguousAddAndDoesNotRepeatIt() {
        FakeCart cart = new FakeCart();
        RuntimeException original = new RuntimeException("Ambiguous dispatch");
        AtomicInteger attempts = new AtomicInteger();
        Assert.assertSame(Assert.expectThrows(RuntimeException.class, () -> OwnedGuestCart.withEmptyCart(cart,
                owned -> owned.add("a", () -> {
                    attempts.incrementAndGet(); cart.items.put("a", 1); throw original;
                }))), original);
        Assert.assertEquals(attempts.get(), 1);
        Assert.assertEquals(cart.removed, List.of("a"));
        Assert.assertTrue(cart.items.isEmpty());
    }

    @Test public void cleanupContinuesAfterOneFailureAndPreservesOriginalError() {
        FakeCart cart = new FakeCart();
        cart.failRemoval = "a";
        AssertionError original = new AssertionError("Primary failure");
        Assert.assertSame(Assert.expectThrows(AssertionError.class, () -> OwnedGuestCart.withEmptyCart(cart, owned -> {
            owned.add("a", () -> cart.items.put("a", 1));
            owned.add("b", () -> cart.items.put("b", 1));
            throw original;
        })), original);
        Assert.assertEquals(cart.removed, List.of("a", "b"));
        Assert.assertEquals(cart.items, Map.of("a", 1));
        Assert.assertEquals(original.getSuppressed().length, 1);
        Assert.assertEquals(original.getSuppressed()[0].getSuppressed().length, 2);
    }

    @Test public void neverRemovesUnownedRowsAndFailsRestoration() {
        FakeCart cart = new FakeCart();
        Assert.expectThrows(AssertionError.class, () -> OwnedGuestCart.withEmptyCart(cart, owned -> {
            owned.add("a", () -> cart.items.put("a", 1));
            cart.items.put("foreign", 4);
        }));
        Assert.assertEquals(cart.removed, List.of("a"));
        Assert.assertEquals(cart.items, Map.of("foreign", 4));
    }

    @Test public void alreadyRemovedOwnedProductIsNotRemovedTwice() {
        FakeCart cart = new FakeCart();
        OwnedGuestCart.withEmptyCart(cart, owned -> {
            owned.add("a", () -> cart.items.put("a", 1));
            owned.add("b", () -> cart.items.put("b", 1));
            cart.removeOwnedProduct("a");
        });
        Assert.assertEquals(cart.removed, List.of("a", "b"));
    }

    @Test public void ownedRemovalConfirmsSurvivorWithoutRequiringEmptyCart() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.page.removeOwnedProduct("a");
        Assert.assertEquals(fixture.clicks, 1);
        Assert.assertEquals(fixture.inventory(), Map.of("b", 1));
    }

    @Test public void ambiguousRemovalIsNotRepeatedEvenByFinallyCleanup() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.noChange = true;
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.assertEquals(fixture.clicks, 1);
    }

    @Test public void survivorChangeCannotConfirmRemoval() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.survivorQuantity = 9;
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.assertEquals(fixture.clicks, 1);
    }

    @Test public void pendingRemovalCannotRebaseExpectedSurvivors() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.noChange = true;
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        fixture.survivorQuantity = 9;
        fixture.disappearAfterInventory = true;
        fixture.initialSurvivorQuantity = 9;
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.assertEquals(fixture.clicks, 1, "Only the original command may dispatch");
        Assert.assertEquals(fixture.inventory(), Map.of("b", 9));
    }

    @Test public void delayedRemovalCanBeConfirmedWithoutTargetOrAnotherClick() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.noChange = true;
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        fixture.present = false;
        fixture.page.removeOwnedProduct("a");
        Assert.assertEquals(fixture.clicks, 1);
        Assert.assertEquals(fixture.inventory(), Map.of("b", 1));
    }

    @Test public void recommendationLinkDoesNotKeepRemovedControlRowPresent() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.recommendation = true;
        fixture.page.removeOwnedProduct("a");
        Assert.assertEquals(fixture.clicks, 1);
        Assert.assertEquals(fixture.inventory(), Map.of("b", 1));
    }

    @Test public void unsettledSurvivorsCannotBeRebasedAfterImmediateConfirmation() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.changeOnSettledRead = true;
        Assert.expectThrows(IllegalStateException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.expectThrows(TimeoutException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.assertEquals(fixture.clicks, 1);
    }

    @Test public void ambiguousRemovalControlsFailBeforeDispatch() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.removalControls = 2;
        Assert.expectThrows(IllegalStateException.class, () -> fixture.page.removeOwnedProduct("a"));
        Assert.assertEquals(fixture.clicks, 0);
        Assert.assertEquals(fixture.inventory(), Map.of("a", 2, "b", 1));
    }

    @Test public void ownedRemovalLocatesAbsoluteProductRouteWithTrailingSlash() {
        RemovalFixture fixture = new RemovalFixture();
        fixture.validateAbsoluteRoute = true;
        fixture.page.removeOwnedProduct("a");
        Assert.assertEquals(fixture.clicks, 1);
        Assert.assertEquals(fixture.inventory(), Map.of("b", 1));
    }

    @Test public void rejectsNamesThatNormalizeToSameIdentity() {
        Assert.expectThrows(IllegalArgumentException.class, () -> CartSnapshot.parse(List.of(List.of(
                row("a", "Product A", "10", "9", "1", "1"),
                row("b", " Product\u00a0A ", "10", "9", "1", "1")), "2", "18")));
    }

    @DataProvider public Object[][] malformedSnapshots() {
        return new Object[][] {
                {null},
                {List.of(List.of(List.of("a", "A", "10", "9", 1, "1")), "1", "9")},
                {List.of(List.of(java.util.Arrays.asList("a", null, "10", "9", "1", "1")), "1", "9")},
                {List.of(raw().get(0), 10, "51.48")},
                {List.of(raw().get(0), "10.50", 51)}
        };
    }

    @Test(dataProvider = "malformedSnapshots") public void rejectsMalformedSnapshotTypes(List<?> raw) {
        Assert.expectThrows(IllegalArgumentException.class, () -> CartSnapshot.parse(raw));
    }

    @Test public void partialRemovalPreservesForeignRowAndStillCleansRemainingOwnedRow() {
        FakeCart cart = new FakeCart();
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> OwnedGuestCart.withEmptyCart(cart, owned -> {
                    owned.add("a", () -> cart.items.put("a", 1));
                    owned.add("b", () -> cart.items.put("b", 1));
                    cart.items.put("foreign", 7);
                    cart.removeOwnedProduct("a");
                }));
        Assert.assertEquals(cart.removed, List.of("a", "b"));
        Assert.assertEquals(cart.items, Map.of("foreign", 7));
        Assert.assertEquals(failure.getSuppressed().length, 1);
    }

    @Test public void cleanupFailureFailsOtherwiseSuccessfulScenario() {
        FakeCart cart = new FakeCart();
        cart.failRemoval = "a";
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> OwnedGuestCart.withEmptyCart(cart, owned -> {
                    owned.add("a", () -> cart.items.put("a", 1));
                    owned.add("b", () -> cart.items.put("b", 1));
                }));
        Assert.assertEquals(cart.removed, List.of("a", "b"));
        Assert.assertEquals(cart.items, Map.of("a", 1));
        Assert.assertEquals(failure.getSuppressed().length, 2);
    }

    @Test public void cleanupNavigationFailurePreservesPrimaryAndDoesNotDeleteBlindly() {
        FakeCart cart = new FakeCart();
        AssertionError primary = new AssertionError("Scenario failed");
        Assert.assertSame(Assert.expectThrows(AssertionError.class,
                () -> OwnedGuestCart.withEmptyCart(cart, owned -> {
                    owned.add("a", () -> cart.items.put("a", 1));
                    cart.failOpen = true;
                    throw primary;
                })), primary);
        Assert.assertEquals(primary.getSuppressed().length, 1);
        Assert.assertTrue(primary.getSuppressed()[0] instanceof IllegalStateException);
        Assert.assertTrue(cart.removed.isEmpty());
        Assert.assertEquals(cart.items, Map.of("a", 1));
    }

    private static final class RemovalFixture {
        boolean present = true, noChange, recommendation, disappearAfterInventory, changeOnSettledRead;
        boolean validateAbsoluteRoute;
        int clicks, survivorQuantity = 1, initialSurvivorQuantity = 1;
        int removalControls = 1;
        Map<String, Integer> inventory() {
            return present ? Map.of("a", 2, "b", initialSurvivorQuantity) : Map.of("b", survivorQuantity);
        }
        org.openqa.selenium.WebElement element(java.lang.reflect.InvocationHandler handler) {
            return (org.openqa.selenium.WebElement) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                    new Class<?>[]{org.openqa.selenium.WebElement.class}, handler);
        }
        org.openqa.selenium.WebElement shown() {
            return element((p, m, a) -> {
                if (m.getName().equals("isDisplayed")) return true;
                throw new AssertionError(m.getName());
            });
        }
        org.openqa.selenium.WebElement row() {
            return element((p, m, a) -> switch (m.getName()) {
                case "isDisplayed" -> true;
                case "getDomAttribute" -> null;
                case "findElements" -> java.util.stream.IntStream.range(0, removalControls)
                        .mapToObj(i -> row().findElement((org.openqa.selenium.By) a[0])).toList();
                case "findElement" -> a[0].toString().contains("following-sibling")
                        ? element((q, n, b) -> switch (n.getName()) {
                            case "isDisplayed" -> true;
                            case "getAttribute" -> "2";
                            default -> throw new AssertionError(n.getName());
                        }) : element((q, n, b) -> switch (n.getName()) {
                            case "isDisplayed", "isEnabled" -> true;
                            case "getDomAttribute" -> null;
                            case "click" -> { clicks++; if (!noChange) present = false; yield null; }
                            default -> throw new AssertionError(n.getName());
                        });
                default -> throw new AssertionError(m.getName());
            });
        }
        final WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (p, m, a) -> {
                    if (m.getName().equals("executeScript")) {
                        if (disappearAfterInventory) present = false;
                        List<List<String>> rows = inventory().entrySet().stream()
                                .map(e -> List.of(e.getKey(), e.getValue().toString())).toList();
                        return List.of(false, rows);
                    }
                    if (m.getName().equals("findElement")) {
                        if (validateAbsoluteRoute) {
                            var xml = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                                    new org.xml.sax.InputSource(new java.io.StringReader("""
                                    <html><main><div><a href='https://www.bakuelectronics.az/mehsul/a/'>A</a>
                                    <button>-</button><span>2</span><button>+</button><button>Remove</button></div>
                                    <div><a href='/mehsul/another'>Other</a><button>-</button><button>+</button></div>
                                    </main></html>
                                    """)));
                            String xpath = a[0].toString().substring("By.xpath: ".length());
                            var nodes = (org.w3c.dom.NodeList) javax.xml.xpath.XPathFactory.newInstance().newXPath()
                                    .evaluate(xpath, xml, javax.xml.xpath.XPathConstants.NODESET);
                            Assert.assertEquals(nodes.getLength(), 1, "The mutation row must match the inventory route");
                        }
                        return row();
                    }
                    if (m.getName().equals("findElements")) {
                        String locator = a[0].toString();
                        if (locator.contains("@href='/mehsul/a'")) return present || recommendation ? List.of(shown()) : List.of();
                        return List.of(); // B remains, so no empty-state message.
                    }
                    throw new AssertionError(m.getName());
                });
        final CartPage page = new CartPage(driver, Duration.ofMillis(1200)) {
            @Override public Map<String, Integer> waitForInventory() {
                if (!present && changeOnSettledRead) survivorQuantity = 9;
                Map<String, Integer> observed = inventory();
                if (disappearAfterInventory) present = false;
                return observed;
            }
        };
    }

    private static final class FakeCart extends CartPage {
        final Map<String, Integer> items = new HashMap<>();
        final List<String> removed = new ArrayList<>();
        String failRemoval;
        boolean failOpen;
        FakeCart() { super(scriptDriver(() -> { throw new AssertionError("Real driver access forbidden"); })); }
        @Override public void openCart() {
            if (failOpen) throw new IllegalStateException("Navigation unavailable");
        }
        @Override public Map<String, Integer> waitForInventory() { return Map.copyOf(items); }
        @Override public void removeOwnedProduct(String slug) {
            removed.add(slug);
            if (slug.equals(failRemoval)) throw new IllegalStateException("Unconfirmed removal");
            items.remove(slug);
        }
    }
}
