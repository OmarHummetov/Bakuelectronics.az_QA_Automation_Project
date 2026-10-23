package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.GuestCartTestSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import az.bakuelectronics.automation.ui.CartProductAdditionTest;
import az.bakuelectronics.automation.ui.CartContentsTest;
import az.bakuelectronics.automation.ui.CartQuantityIncreaseTest;
import az.bakuelectronics.automation.ui.CartQuantityDecreaseTest;
import az.bakuelectronics.automation.ui.CartProductRemovalTest;
import az.bakuelectronics.automation.ui.CartZeroQuantityRemovalTest;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class GuestCartCleanupTest {
    private static final String SLUG = "owned-test-product";

    @Test
    public void successfulScenarioRestoresEmptyBaseline() {
        FakeCart cart = new FakeCart();
        int[] attempts = {0};
        GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
            attempts[0]++;
            cart.productPresent = true;
        });
        Assert.assertEquals(attempts[0], 1);
        Assert.assertEquals(cart.removed, List.of(SLUG));
        Assert.assertEquals(cart.emptyChecks, 2);
        Assert.assertFalse(cart.productPresent);
    }

    @Test
    public void refusesNonemptyBaselineWithoutRunningScenarioOrCleanup() {
        FakeCart cart = new FakeCart();
        cart.otherProductPresent = true;
        Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> Assert.fail("Must not run")));
        Assert.assertTrue(cart.removed.isEmpty());
        Assert.assertEquals(cart.opens, 1);
        Assert.assertTrue(cart.otherProductPresent);
    }

    @Test
    public void ambiguousAddFailureStillCleansUpWithoutRepeatingScenario() {
        FakeCart cart = new FakeCart();
        RuntimeException original = new RuntimeException("Add command outcome unknown");
        int[] attempts = {0};
        RuntimeException actual = Assert.expectThrows(RuntimeException.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
                    attempts[0]++;
                    cart.productPresent = true;
                    throw original;
                }));
        Assert.assertSame(actual, original);
        Assert.assertEquals(attempts[0], 1);
        Assert.assertEquals(cart.removed, List.of(SLUG));
        Assert.assertEquals(cart.emptyChecks, 2);
    }

    @Test
    public void cleanupFailureDoesNotMaskAmbiguousAddFailure() {
        FakeCart cart = new FakeCart();
        RuntimeException original = new RuntimeException("Add click outcome unknown");
        cart.removalFailure = new RuntimeException("Cleanup failed");
        RuntimeException actual = Assert.expectThrows(RuntimeException.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
                    cart.productPresent = true;
                    throw original;
                }));
        Assert.assertSame(actual, original);
        Assert.assertEquals(actual.getSuppressed().length, 1);
        Assert.assertSame(actual.getSuppressed()[0].getCause(), cart.removalFailure);
        Assert.assertEquals(cart.removed, List.of(SLUG));
    }

    @Test
    public void failedAddWithNoProductVerifiesEmptyStateWithoutRemovingAnything() {
        FakeCart cart = new FakeCart();
        AssertionError original = new AssertionError("Add did not complete");
        Assert.assertSame(Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> { throw original; })), original);
        Assert.assertTrue(cart.removed.isEmpty());
        Assert.assertEquals(cart.emptyChecks, 2);
    }

    @Test
    public void successfulRemoveScenarioDoesNotRemoveTwice() {
        FakeCart cart = new FakeCart();
        GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
            cart.productPresent = true;
            cart.removeProduct(SLUG);
        });
        Assert.assertEquals(cart.removed, List.of(SLUG));
        Assert.assertEquals(cart.emptyChecks, 2);
    }

    @Test
    public void cleanupFailureFailsOtherwiseSuccessfulScenario() {
        FakeCart cart = new FakeCart();
        cart.removalFailure = new RuntimeException("Remove failed");
        AssertionError failure = Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> cart.productPresent = true));
        Assert.assertEquals(failure.getMessage(), "Guest cart cleanup failed");
        Assert.assertSame(failure.getCause(), cart.removalFailure);
        Assert.assertEquals(cart.removed, List.of(SLUG));
    }

    @Test
    public void cleanupFailureIsSuppressedOnOriginalAssertion() {
        FakeCart cart = new FakeCart();
        cart.removalFailure = new RuntimeException("Remove failed");
        AssertionError original = new AssertionError("Quantity wrong");
        Assert.assertSame(Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
                    cart.productPresent = true;
                    throw original;
                })), original);
        Assert.assertEquals(original.getSuppressed().length, 1);
        Assert.assertSame(original.getSuppressed()[0].getCause(), cart.removalFailure);
    }

    @Test
    public void cleanupFailureDoesNotMaskAnUnconfirmedDecrementFailure() {
        FakeCart cart = new FakeCart();
        TimeoutException original = new TimeoutException("Decrement result unconfirmed; no second click sent");
        cart.removalFailure = new StaleElementReferenceException("Cleanup control replaced");
        TimeoutException actual = Assert.expectThrows(TimeoutException.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
                    cart.productPresent = true;
                    throw original;
                }));
        Assert.assertSame(actual, original);
        Assert.assertEquals(actual.getSuppressed().length, 1);
        Assert.assertSame(actual.getSuppressed()[0].getCause(), cart.removalFailure);
        Assert.assertEquals(cart.removed, List.of(SLUG));
    }

    @Test
    public void unrelatedProductIsNeverRemovedAndPreventsSuccessfulRestoration() {
        FakeCart cart = new FakeCart();
        Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
                    cart.productPresent = true;
                    cart.otherProductPresent = true;
                }));
        Assert.assertEquals(cart.removed, List.of(SLUG));
        Assert.assertTrue(cart.otherProductPresent);
        Assert.assertEquals(cart.emptyChecks, 2);
    }

    @Test
    public void guestTestsHaveNoAuthenticatedBaseAndRetainGroupsAndNoRetry() throws Exception {
        Map<Class<?>, String> methods = Map.of(
                CartContentsTest.class, "openCartAndVerifyProduct",
                CartProductAdditionTest.class, "addProductToCart",
                CartQuantityIncreaseTest.class, "increaseProductQuantity",
                CartQuantityDecreaseTest.class, "decreaseProductQuantity",
                CartProductRemovalTest.class, "removeProductFromCart",
                CartZeroQuantityRemovalTest.class,
                "shouldRemoveGuestProductWhenQuantityDecreasedFromOneToZero");
        for (Map.Entry<Class<?>, String> entry : methods.entrySet()) {
            Assert.assertEquals(entry.getKey().getSuperclass(), BaseTest.class);
            Test annotation = entry.getKey().getMethod(entry.getValue()).getAnnotation(Test.class);
            Assert.assertEquals(annotation.retryAnalyzer(), NoRetryAnalyzer.class);
            Assert.assertEquals(annotation.groups(), new String[]{
                    entry.getKey() == CartProductAdditionTest.class ? "smoke" : "regression"});
        }
    }

    @Test
    public void capturesFailureStateBeforeCleanupChangesTheCart() {
        FakeCart cart = new FakeCart();
        AssertionError original = new AssertionError("Scenario failure");
        Assert.assertSame(Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(cart, SLUG, () -> {
                    cart.productPresent = true;
                    throw original;
                })), original);
        Assert.assertEquals(cart.snapshots, List.of("present=true; removed=0"));
        Assert.assertFalse(cart.productPresent);
    }

    @Test
    public void capturesCleanupOnlyFailureAndDiagnosticErrorsNeverPreventCleanup() {
        FakeCart cart = new FakeCart();
        cart.removalFailure = new RuntimeException("Synthetic removal failure");
        Assert.expectThrows(AssertionError.class, () -> GuestCartTestSupport.withEmptyCart(
                cart, SLUG, () -> cart.productPresent = true));
        Assert.assertEquals(cart.snapshots, List.of("present=true; removed=1"));

        FakeCart unavailable = new FakeCart();
        unavailable.diagnosticFailure = true;
        AssertionError original = new AssertionError("Original assertion");
        Assert.assertSame(Assert.expectThrows(AssertionError.class, () ->
                GuestCartTestSupport.withEmptyCart(unavailable, SLUG, () -> {
                    unavailable.productPresent = true;
                    throw original;
                })), original);
        Assert.assertEquals(unavailable.removed, List.of(SLUG));
    }

    private static final class FakeCart extends CartPage {
        private boolean productPresent;
        private boolean otherProductPresent;
        private int opens;
        private int emptyChecks;
        private RuntimeException removalFailure;
        private final List<String> removed = new ArrayList<>();
        private final List<String> snapshots = new ArrayList<>();
        private boolean diagnosticFailure;

        private FakeCart() {
            super((WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                        throw new AssertionError("Unexpected real driver access: " + method.getName());
                    }));
        }

        @Override public void openCart() { opens++; }
        @Override public String failureState(String slug) {
            Assert.assertEquals(slug, SLUG);
            if (diagnosticFailure) throw new IllegalStateException("Synthetic unavailable driver");
            String snapshot = "present=" + productPresent + "; removed=" + removed.size();
            snapshots.add(snapshot);
            return snapshot;
        }
        @Override public CartPage waitForCartPage() { return this; }
        @Override public boolean isCartEmpty() {
            emptyChecks++;
            return !productPresent && !otherProductPresent;
        }
        @Override public boolean isProductVisible(String slug) {
            Assert.assertEquals(slug, SLUG);
            if (!productPresent) throw new TimeoutException("Selected product absent");
            return true;
        }
        @Override public void removeProduct(String slug) {
            Assert.assertEquals(slug, SLUG);
            removed.add(slug);
            if (removalFailure != null) throw removalFailure;
            productPresent = false;
        }
    }
}
