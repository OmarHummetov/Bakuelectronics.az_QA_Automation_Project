package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.support.GuestCartTestSupport;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;


public class CartPageQuantityTest {
    private static final String PRODUCT_SLUG = "selected-product-123";

    @Test
    public void increasesExactlyFromOneToTwoWithinSelectedProduct() {
        CartDom dom = new CartDom();
        dom.page.increaseQuantity(PRODUCT_SLUG);
        assertSingleIncrease(dom);
        Assert.assertTrue(dom.rowLocators.stream().allMatch(locator -> locator.contains(PRODUCT_SLUG)),
                "Every row lookup must stay scoped to the selected product.");
    }

    @Test
    public void relocatesAfterQuantityReadBecomesStale() {
        CartDom dom = new CartDom();
        dom.staleBeforeClickRead = true;
        dom.page.increaseQuantity(PRODUCT_SLUG);
        assertSingleIncrease(dom);
    }

    @Test
    public void relocatesAfterButtonBecomesStaleBeforeClick() {
        CartDom dom = new CartDom();
        dom.staleButtonCheck = true;
        dom.page.increaseQuantity(PRODUCT_SLUG);
        assertSingleIncrease(dom);
    }

    @Test
    public void rereadsDelayedQuantityAfterAmbiguousStaleClickWithoutClickingAgain() {
        CartDom dom = new CartDom();
        dom.staleClick = true;
        dom.delayedReads = 2;
        dom.page.increaseQuantity(PRODUCT_SLUG);
        assertSingleIncrease(dom);
        Assert.assertTrue(dom.readsAfterClick >= 3);
    }

    @Test
    public void relocatesQuantityAfterSuccessfulClickAndReactReplacement() {
        CartDom dom = new CartDom();
        dom.staleAfterClickRead = true;
        dom.page.increaseQuantity(PRODUCT_SLUG);
        assertSingleIncrease(dom);
    }

    @Test
    public void ambiguousClickWithoutConfirmedChangeTimesOutWithoutSecondClick() {
        CartDom dom = new CartDom();
        dom.staleClick = true;
        dom.applyClick = false;
        Assert.expectThrows(TimeoutException.class, () -> dom.page.increaseQuantity(PRODUCT_SLUG));
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 1);
        Assert.assertTrue(dom.readsAfterClick > 1, "Recovery must re-read rather than blindly retry.");
    }

    @Test
    public void rejectsOvershootInsteadOfAcceptingAnyQuantityChange() {
        CartDom dom = new CartDom();
        dom.resultQuantity = 3;
        Assert.expectThrows(IllegalStateException.class, () -> dom.page.increaseQuantity(PRODUCT_SLUG));
        Assert.assertEquals(dom.clicks, 1);
    }

    @Test
    public void legacyOverloadAlsoAvoidsDuplicateClickAfterStaleOutcome() {
        CartDom dom = new CartDom();
        dom.staleClick = true;
        dom.page.increaseQuantity();
        assertSingleIncrease(dom);
    }

    @Test
    public void waitsForBusyRowAndStableQuantityBeforeSingleClick() {
        CartDom dom = new CartDom();
        dom.busyReads = 2;
        dom.blockerReads = 1;
        dom.page.increaseQuantity(PRODUCT_SLUG);
        assertSingleIncrease(dom);
        Assert.assertTrue(dom.readinessReads >= 5);
    }

    @Test
    public void persistentBusyStateFailsWithoutAttemptingMutation() {
        CartDom dom = new CartDom();
        dom.busyReads = Integer.MAX_VALUE;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class,
                () -> dom.page.increaseQuantity(PRODUCT_SLUG));
        Assert.assertEquals(dom.clicks, 0);
        Assert.assertTrue(failure.getMessage().contains("clickAttempted=false"));
        Assert.assertTrue(failure.getMessage().contains("control busy/disabled or modal present"));
    }

    @Test
    public void mutationDiagnosticsAreSanitizedAndFrozenBeforeCleanup() {
        CartDom dom = new CartDom();
        dom.staleClick = true;
        dom.applyClick = false;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class,
                () -> dom.page.increaseQuantity(PRODUCT_SLUG));
        String diagnostic = failure.getMessage();
        Assert.assertTrue(diagnostic.contains("operation=increase; target=" + PRODUCT_SLUG));
        Assert.assertTrue(diagnostic.contains("preClickQuantity=1"));
        Assert.assertTrue(diagnostic.contains("clickAttempted=true; clickReturned=false; clickThrewStale=true"));
        Assert.assertTrue(diagnostic.contains("lastQuantity=1; productPresent=true; emptyCart=false"));
        Assert.assertTrue(failure.getCause().getMessage().contains("StaleElementReferenceException"));
        Assert.assertNull(failure.getCause().getCause());
        Assert.assertFalse(failure.getCause().getMessage().contains("synthetic-private"));
        Assert.assertFalse(diagnostic.contains("synthetic-private"));
        dom.quantity = 9;
        Assert.assertEquals(failure.getMessage(), diagnostic);
        Assert.assertTrue(dom.page.failureState(PRODUCT_SLUG).contains("lastQuantity=1"));
    }

    @Test
    public void returnedClickWithoutMutationStillFailsWithExactLastState() {
        CartDom dom = new CartDom();
        dom.applyClick = false;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class,
                () -> dom.page.increaseQuantity(PRODUCT_SLUG));
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertTrue(failure.getMessage().contains("clickReturned=true; clickThrewStale=false"));
        Assert.assertTrue(failure.getMessage().contains("lastQuantity=1"));
    }

    @Test
    public void changingQuantityRestartsPreclickStabilityObservation() {
        CartDom dom = new CartDom();
        dom.changeBeforeClick = true;
        dom.resultQuantity = 3;
        dom.page.increaseQuantity(PRODUCT_SLUG);
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 3);
        Assert.assertTrue(dom.rowLocators.size() >= 4);
    }

    private static void assertSingleIncrease(CartDom dom) {
        Assert.assertEquals(dom.quantity, 2, "Quantity must change exactly from one to two.");
        Assert.assertEquals(dom.clicks, 1, "Only one click command may be sent.");
        Assert.assertTrue(dom.rowLocators.size() >= 3, "The updated row must be freshly located.");
    }

    @Test
    public void decrementRelocatesStaleReadinessBeforeSendingOneClick() {
        for (boolean staleQuantity : new boolean[]{true, false}) {
            DecrementDom dom = new DecrementDom();
            dom.staleQuantity = staleQuantity;
            dom.staleButton = !staleQuantity;
            dom.page.decreaseQuantity(PRODUCT_SLUG);
            Assert.assertEquals(dom.clicks, 1);
            Assert.assertEquals(dom.quantity, 0);
            Assert.assertTrue(dom.rowLookups >= 3, "Readiness must re-locate the selected row.");
        }
    }

    @Test
    public void staleDecrementClickAcceptsDelayedRemovalOnlyWithEmptyCart() {
        DecrementDom dom = new DecrementDom();
        dom.staleClick = true;
        dom.delay = 1;
        dom.staleObservation = true;
        dom.page.decreaseQuantity(PRODUCT_SLUG);
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 0);
        Assert.assertTrue(dom.stateReads >= 3, "Re-read the resulting basket instead of clicking again.");
        Assert.assertTrue(dom.emptyReads > 0);
    }

    @Test
    public void successfulDecrementAlsoConfirmsRemovalWithoutAnotherClick() {
        DecrementDom dom = new DecrementDom();
        dom.page.decreaseQuantity(PRODUCT_SLUG);
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 0);
        Assert.assertTrue(dom.emptyReads > 0);
    }

    @Test
    public void staleClickWithoutMutationFailsWithoutSecondDecrement() {
        DecrementDom dom = new DecrementDom();
        dom.staleClick = true;
        dom.applyClick = false;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class,
                () -> dom.page.decreaseQuantity(PRODUCT_SLUG));
        Assert.assertTrue(failure.getMessage().contains("No additional decrement was sent"));
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 1);
        Assert.assertTrue(dom.stateReads > 1);
    }

    @Test
    public void missingProductWithoutEmptyCartConfirmationIsAmbiguousAndFails() {
        DecrementDom dom = new DecrementDom();
        dom.staleClick = true;
        dom.confirmEmpty = false;
        Assert.expectThrows(TimeoutException.class, () -> dom.page.decreaseQuantity(PRODUCT_SLUG));
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 0);
        Assert.assertTrue(dom.emptyReads > 1);
    }

    @Test
    public void existingTwoToOneDecrementStillRequiresExactResultAfterStaleClick() {
        DecrementDom dom = new DecrementDom();
        dom.quantity = 2;
        dom.result = 1;
        dom.staleClick = true;
        dom.page.decreaseQuantity(PRODUCT_SLUG);
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertEquals(dom.quantity, 1);
        Assert.assertTrue(dom.emptyReads > 0, "Diagnostics include observed empty-cart state.");
    }

    @Test
    public void removalRelocatesStaleReadinessBeforeSendingOneClick() {
        for (boolean staleRow : new boolean[]{true, false}) {
            RemovalDom dom = new RemovalDom();
            dom.staleRow = staleRow;
            dom.staleButton = !staleRow;
            dom.page.removeProduct(PRODUCT_SLUG);
            Assert.assertEquals(dom.clicks, 1);
            Assert.assertTrue(dom.rowLookups >= 2, "Readiness must use a freshly located slug-scoped row.");
            Assert.assertFalse(dom.present);
            Assert.assertTrue(dom.emptyReads > 0);
        }
    }

    @Test
    public void staleRemovalClickConfirmsDelayedAbsenceAndEmptyCart() {
        RemovalDom dom = new RemovalDom();
        dom.staleClick = true;
        dom.staleObservation = true;
        dom.delay = 1;
        dom.page.removeProduct(PRODUCT_SLUG);
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertFalse(dom.present);
        Assert.assertTrue(dom.stateReads >= 3, "Observe delayed state without sending another removal.");
        Assert.assertTrue(dom.emptyReads > 0);
    }

    @Test
    public void successfulRemovalAlsoRequiresConfirmedEmptyState() {
        RemovalDom dom = new RemovalDom();
        dom.page.removeProduct(PRODUCT_SLUG);
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertFalse(dom.present);
        Assert.assertTrue(dom.emptyReads > 0);
    }

    @Test
    public void unchangedRemovalIsNotRepeatedByFinallyCleanup() {
        RemovalDom dom = new RemovalDom();
        dom.present = false;
        dom.applyClick = false;
        dom.staleClick = true;
        TimeoutException[] original = {null};
        TimeoutException failure = Assert.expectThrows(TimeoutException.class, () ->
                GuestCartTestSupport.withEmptyCart(dom.page, PRODUCT_SLUG, () -> {
                    dom.present = true;
                    try {
                        dom.page.removeProduct(PRODUCT_SLUG);
                    } catch (TimeoutException exception) {
                        original[0] = exception;
                        throw exception;
                    }
                }));
        Assert.assertSame(failure, original[0], "Cleanup must preserve the original unconfirmed removal.");
        Assert.assertTrue(failure.getMessage().contains("No additional removal was sent"));
        Assert.assertEquals(failure.getSuppressed().length, 1);
        Assert.assertTrue(failure.getSuppressed()[0].getCause() instanceof TimeoutException);
        Assert.assertEquals(dom.clicks, 1, "Finally cleanup must not issue a second ambiguous mutation.");
        Assert.assertTrue(dom.present);
        Assert.assertTrue(dom.stateReads > 1);
    }

    @Test
    public void missingEmptyStateAfterRemovalFailsWithoutAnotherClick() {
        RemovalDom dom = new RemovalDom();
        dom.staleClick = true;
        dom.confirmEmpty = false;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class,
                () -> dom.page.removeProduct(PRODUCT_SLUG));
        Assert.assertTrue(failure.getMessage().contains("empty cart not confirmed"));
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertFalse(dom.present);
        Assert.assertTrue(dom.emptyReads > 1);
    }

    @Test
    public void hiddenTargetAfterRemovalIsNotDomAbsence() {
        RemovalDom dom = new RemovalDom();
        dom.staleClick = true;
        dom.applyClick = false;
        dom.hiddenLink = true;
        Assert.expectThrows(TimeoutException.class, () -> dom.page.removeProduct(PRODUCT_SLUG));
        Assert.assertEquals(dom.clicks, 1);
        Assert.assertTrue(dom.present);
        Assert.assertTrue(dom.emptyReads > 0, "Diagnostics must also observe the empty-state flag.");
    }

    @Test
    public void cleanupRemovalFailurePreservesOriginalAssertion() {
        RemovalDom dom = new RemovalDom();
        dom.present = false;
        dom.staleClick = true;
        dom.applyClick = false;
        AssertionError original = new AssertionError("The cart total should not be blank.");
        AssertionError actual = Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(dom.page, PRODUCT_SLUG, () -> {
                    dom.present = true;
                    throw original;
                }));
        Assert.assertSame(actual, original);
        Assert.assertEquals(actual.getSuppressed().length, 1);
        Assert.assertEquals(actual.getSuppressed()[0].getMessage(), "Guest cart cleanup failed");
        Assert.assertTrue(actual.getSuppressed()[0].getCause() instanceof TimeoutException);
        Assert.assertEquals(dom.clicks, 1);
    }

    private static final class RemovalDom {
        private boolean present = true;
        private boolean applyClick = true;
        private boolean confirmEmpty = true;
        private boolean hiddenLink;
        private boolean staleRow;
        private boolean staleButton;
        private boolean staleClick;
        private boolean staleObservation;
        private int clicks;
        private int rowLookups;
        private int stateReads;
        private int emptyReads;
        private int delay;

        private final CartPage page = new CartPage((WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(), new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return "Offline removal driver";
                    String locator = args[0].toString();
                    if (method.getName().equals("findElement")) {
                        Assert.assertTrue(locator.contains(PRODUCT_SLUG) && locator.contains("ancestor::div"));
                        rowLookups++;
                        return row();
                    }
                    if (method.getName().equals("findElements")) {
                        if (locator.startsWith("By.cssSelector:")) return List.of();
                        if (locator.contains(PRODUCT_SLUG)) {
                            stateReads++;
                            if (staleObservation) { staleObservation = false; throw CartDom.stale(); }
                            if (clicks > 0 && applyClick && delay-- <= 0) present = false;
                            return present ? List.of(displayed(!hiddenLink)) : List.of();
                        }
                        Assert.assertTrue(locator.contains("//main//div") && locator.contains("Sizin hal-hazırda"));
                        emptyReads++;
                        return !present && confirmEmpty ? List.of(displayed(true)) : List.of();
                    }
                    throw new AssertionError("Unexpected driver operation; no browser or JavaScript allowed.");
                }), java.time.Duration.ofMillis(2200)) {
            @Override public void openCart() { }
            @Override public CartPage waitForCartPage() { return this; }
            @Override public boolean isCartEmpty() { return !present && confirmEmpty; }
            @Override public boolean isProductVisible(String slug) {
                Assert.assertEquals(slug, PRODUCT_SLUG);
                if (!present) throw new TimeoutException("Selected product absent");
                return true;
            }
        };

        private WebElement row() {
            return CartDom.element((proxy, method, args) -> {
                if (method.getName().equals("getDomAttribute")) return null;
                if (method.getName().equals("isDisplayed")) {
                    if (staleRow) { staleRow = false; throw CartDom.stale(); }
                    return true;
                }
                Assert.assertEquals(method.getName(), "findElement");
                if (args[0].toString().contains("following-sibling")) {
                    return CartDom.element((p, m, a) -> switch (m.getName()) {
                        case "isDisplayed" -> true;
                        case "getAttribute" -> "1";
                        default -> throw new AssertionError("Unexpected quantity read.");
                    });
                }
                Assert.assertEquals(args[0].toString(), "By.xpath: .//button[not(normalize-space() = '-')"
                        + " and not(normalize-space() = '+')]");
                return CartDom.element((p, m, a) -> switch (m.getName()) {
                    case "getDomAttribute" -> null;
                    case "isDisplayed" -> true;
                    case "isEnabled" -> {
                        if (staleButton) { staleButton = false; throw CartDom.stale(); }
                        yield true;
                    }
                    case "click" -> {
                        clicks++;
                        if (staleClick) throw CartDom.stale();
                        yield null;
                    }
                    default -> throw new AssertionError("Unexpected removal control operation.");
                });
            });
        }

        private WebElement displayed(boolean visible) {
            return CartDom.element((proxy, method, args) -> {
                Assert.assertEquals(method.getName(), "isDisplayed", "Observation must not mutate the cart.");
                return visible;
            });
        }
    }

    private static final class DecrementDom {
        private int quantity = 1;
        private int result;
        private int clicks;
        private int rowLookups;
        private int stateReads;
        private int emptyReads;
        private int delay;
        private boolean staleQuantity;
        private boolean staleButton;
        private boolean staleClick;
        private boolean staleObservation;
        private boolean applyClick = true;
        private boolean confirmEmpty = true;
        private final CartPage page = new CartPage((WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(), new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    String locator = args == null ? "" : args[0].toString();
                    if (method.getName().equals("findElement")) {
                        Assert.assertTrue(locator.contains(PRODUCT_SLUG), "Row lookup must use the exact slug.");
                        rowLookups++;
                        observe();
                        return row();
                    }
                    if (method.getName().equals("findElements")) {
                        if (locator.startsWith("By.cssSelector:")) return List.of();
                        if (locator.contains(PRODUCT_SLUG)) {
                            observe();
                            return quantity == 0 ? List.of() : List.of(displayed());
                        }
                        Assert.assertTrue(locator.contains("//main//div") && locator.contains("Sizin hal-hazırda"));
                        emptyReads++;
                        return quantity == 0 && confirmEmpty ? List.of(displayed()) : List.of();
                    }
                    if (method.getName().equals("toString")) return "Offline decrement driver";
                    throw new AssertionError("Unexpected driver operation.");
                }), java.time.Duration.ofMillis(2200));

        private void observe() {
            if (clicks == 0) return;
            stateReads++;
            if (staleObservation) {
                staleObservation = false;
                throw CartDom.stale();
            }
            if (applyClick && delay-- <= 0) quantity = result;
        }

        private WebElement row() {
            return CartDom.element((proxy, method, args) -> {
                if (method.getName().equals("getDomAttribute")) return null;
                if (method.getName().equals("isDisplayed")) return true;
                if (method.getName().equals("findElement")) {
                    String locator = args[0].toString();
                    if (locator.contains("following-sibling")) return CartDom.element((p, m, a) -> {
                        if (m.getName().equals("isDisplayed")) return true;
                        if (m.getName().equals("getAttribute")) {
                            if (staleQuantity && rowLookups > 1) { staleQuantity = false; throw CartDom.stale(); }
                            return Integer.toString(quantity);
                        }
                        throw new AssertionError("Unexpected quantity operation.");
                    });
                    Assert.assertEquals(locator, "By.xpath: .//button[normalize-space() = '-']");
                    return CartDom.element((p, m, a) -> switch (m.getName()) {
                        case "getDomAttribute" -> null;
                        case "isDisplayed" -> true;
                        case "isEnabled" -> {
                            if (staleButton) { staleButton = false; throw CartDom.stale(); }
                            yield true;
                        }
                        case "click" -> {
                            clicks++;
                            if (staleClick) throw CartDom.stale();
                            yield null;
                        }
                        default -> throw new AssertionError("Unexpected minus operation.");
                    });
                }
                throw new AssertionError("Unexpected row operation.");
            });
        }

        private WebElement displayed() {
            return CartDom.element((proxy, method, args) -> {
                if (method.getName().equals("isDisplayed")) return true;
                throw new AssertionError("Result observation must not mutate the cart.");
            });
        }
    }

    private static final class CartDom {
        private final List<String> rowLocators = new ArrayList<>();
        private int quantity = 1;
        private int resultQuantity = 2;
        private int clicks;
        private int readsAfterClick;
        private int delayedReads;
        private int busyReads;
        private int blockerReads;
        private int readinessReads;
        private boolean changeBeforeClick;
        private boolean staleBeforeClickRead;
        private boolean staleAfterClickRead;
        private boolean staleButtonCheck;
        private boolean staleClick;
        private boolean applyClick = true;
        private final CartPage page = new CartPage((WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(), new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findElements")) {
                        if (args[0].toString().startsWith("By.cssSelector:")) {
                            return blockerReads-- > 0 ? List.of(button()) : List.of();
                        }
                        if (args[0].toString().contains("Sizin hal-hazırda")) return List.of();
                        return List.of(row());
                    }
                    if (method.getName().equals("findElement")) {
                        rowLocators.add(args[0].toString());
                        return row();
                    }
                    if (method.getName().equals("toString")) {
                        return "Offline cart driver";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }), java.time.Duration.ofMillis(2200));

        private WebElement row() {
            return element((proxy, method, args) -> {
                if (method.getName().equals("isDisplayed")) return true;
                if (method.getName().equals("getDomAttribute")) {
                    readinessReads++;
                    return busyReads-- > 0 ? "true" : null;
                }
                if (method.getName().equals("findElement")) {
                    String locator = ((By) args[0]).toString();
                    if (locator.contains("following-sibling")) {
                        return quantityElement();
                    }
                    Assert.assertEquals(locator, "By.xpath: .//button[normalize-space() = '+']");
                    return button();
                }
                throw new UnsupportedOperationException(method.getName());
            });
        }

        private WebElement quantityElement() {
            return element((proxy, method, args) -> switch (method.getName()) {
                case "getDomAttribute" -> null;
                case "isDisplayed" -> true;
                case "getAttribute" -> {
                    if (clicks == 0 && changeBeforeClick && rowLocators.size() > 1) quantity = 2;
                    if (clicks == 0 && staleBeforeClickRead) {
                        staleBeforeClickRead = false;
                        throw stale();
                    }
                    if (clicks > 0) {
                        readsAfterClick++;
                        if (staleAfterClickRead) {
                            staleAfterClickRead = false;
                            throw stale();
                        }
                        if (applyClick && delayedReads-- <= 0) {
                            quantity = resultQuantity;
                        }
                    }
                    yield Integer.toString(quantity);
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
        }

        private WebElement button() {
            return element((proxy, method, args) -> switch (method.getName()) {
                case "getDomAttribute" -> null;
                case "isDisplayed" -> true;
                case "isEnabled" -> {
                    if (staleButtonCheck) {
                        staleButtonCheck = false;
                        throw stale();
                    }
                    yield true;
                }
                case "click" -> {
                    clicks++;
                    if (staleClick) {
                        throw new StaleElementReferenceException("token=synthetic-private https://example.invalid/private");
                    }
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
        }

        private static WebElement element(java.lang.reflect.InvocationHandler handler) {
            return (WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                    new Class<?>[]{WebElement.class}, handler);
        }

        private static StaleElementReferenceException stale() {
            return new StaleElementReferenceException("Offline React row replacement");
        }
    }
}
