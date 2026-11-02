package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.CartSnapshot;
import az.bakuelectronics.automation.support.AuthenticatedCartSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** Separate contract: main-card unit inputs versus independently displayed basket totals. */
@Feature("Authenticated cart summary consistency")
public class AuthenticatedMultiProductCartSummaryConsistencyTest extends AuthenticatedCartSupport {
    private record Observation(Map<String, CartSnapshot.Row> inputs, CartPage.DisplayedSummaryTotals displayed) { }

    @Test(groups = {"regression", "sensitive-auth", "ui-mutation", "no-retry"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void summaryMatchesIndependentProductInputsAndRestoresBasket() {
        scenario(() -> {
            at("capture independent main-card baseline");
            Map<String, CartSnapshot.Row> original = ownership.baseline().isEmpty()
                    ? Map.of() : cart.waitForDisplayedProductInputs();
            Assert.assertEquals(original.keySet(), ownership.baseline().keySet());
            original.forEach((slug, row) -> Assert.assertEquals(row.quantity(), ownership.baseline().get(slug).intValue()));
            var originalTotals = original.isEmpty() ? null : cart.waitForDisplayedSummaryTotals();
            if (originalTotals != null) assertDisplayedTotalsMatchInputs(original, originalTotals);

            at("inspect two distinct absent products");
            Product a = inspect(A), b = inspect(B);
            Assert.assertNotEquals(a.slug(), b.slug());
            Assert.assertNotEquals(a.code(), b.code());
            at("add only test-owned A and B");
            add(a); add(b);
            var products = Map.of(a.slug(), a, b.slug(), b);
            var one = observe(products, Map.of(a.slug(), 1, b.slug(), 1), original);

            at("set A=2 and verify B=1 using independent input and summary reads");
            increase(a);
            var two = observe(products, Map.of(a.slug(), 2, b.slug(), 1), original);
            products.keySet().forEach(slug -> {
                Assert.assertEquals(two.inputs().get(slug).originalUnitPrice(), one.inputs().get(slug).originalUnitPrice());
                Assert.assertEquals(two.inputs().get(slug).discountedUnitPrice(), one.inputs().get(slug).discountedUnitPrice());
            });
            Assert.assertEquals(two.displayed().total().subtract(one.displayed().total()).compareTo(a.price()), 0,
                    "Independently displayed total increases by exactly A's additional captured unit price");
            Allure.attachment("Summary consistency at A=2/B=1", "PASS: exact identities, quantities and main-card unit prices; separately read visible total and discount match BigDecimal expectations; original rows unchanged");

            at("remove A and verify unchanged B and actual displayed summary delta");
            cart.removeOwnedProduct(a.slug());
            var remaining = observe(Map.of(b.slug(), b), Map.of(b.slug(), 1), original);
            Assert.assertEquals(remaining.inputs().get(b.slug()), two.inputs().get(b.slug()), "B's complete main-card row remains unchanged");
            BigDecimal expectedRemoved = a.price().multiply(BigDecimal.valueOf(2));
            BigDecimal displayedDelta = two.displayed().total().subtract(remaining.displayed().total());
            Assert.assertEquals(displayedDelta.compareTo(expectedRemoved), 0, "Actual visible total delta equals two captured A units");
            BigDecimal expectedDiscountRemoved = one.inputs().get(a.slug()).originalUnitPrice().subtract(a.price())
                    .multiply(BigDecimal.valueOf(2));
            BigDecimal displayedDiscountDelta = two.displayed().discount().abs().subtract(remaining.displayed().discount().abs());
            Assert.assertEquals(displayedDiscountDelta.compareTo(expectedDiscountRemoved), 0,
                    "Actual displayed discount delta removes exactly A's two unit discounts");
            Allure.attachment("Remove A / preserve B and visible deltas", "PASS: B and original rows unchanged; expected A amount="
                    + expectedRemoved + "; independently displayed total delta=" + displayedDelta
                    + "; expected discount reduction=" + expectedDiscountRemoved + "; displayed discount delta=" + displayedDiscountDelta);

            at("remove B and verify initial account basket after fresh login");
            cart.removeOwnedProduct(b.slug());
            Assert.assertEquals(cart.waitForInventory(), ownership.baseline());
            reopenAndVerify();
            Assert.assertEquals(cart.waitForInventory(), ownership.baseline());
            if (!original.isEmpty()) {
                Assert.assertEquals(cart.waitForDisplayedProductInputs(), original);
                Assert.assertEquals(cart.waitForDisplayedSummaryTotals(), originalTotals);
            }
            Allure.attachment("Summary-contract scope", "PASS: unit-price/quantity arithmetic versus separately read visible basket total and discount. Independently displayed row totals are outside this contract.");
        });
    }

    private Observation observe(Map<String, Product> products, Map<String, Integer> quantities,
                                Map<String, CartSnapshot.Row> original) {
        Map<String, Integer> expected = new HashMap<>(ownership.baseline());
        expected.putAll(quantities);
        Assert.assertEquals(cart.waitForInventory(), expected, "Complete account identity/quantity inventory");
        var inputs = cart.waitForDisplayedProductInputs();
        var displayed = cart.waitForDisplayedSummaryTotals();
        Assert.assertEquals(cart.waitForDisplayedProductInputs(), inputs, "Main-card inputs stay stable across independent summary read");
        Assert.assertEquals(inputs.keySet(), expected.keySet());
        inputs.forEach((slug, row) -> {
            Assert.assertEquals(row.quantity(), expected.get(slug).intValue());
            if (products.containsKey(slug)) {
                Product product = products.get(slug);
                Assert.assertEquals(row.name(), CartSnapshot.normalize(product.name()));
                Assert.assertEquals(row.discountedUnitPrice().compareTo(product.price()), 0,
                        "Main-card displayed unit price matches independently captured product-page price");
            } else Assert.assertEquals(row, original.get(slug), "Pre-existing account row stays unchanged");
        });
        assertDisplayedTotalsMatchInputs(inputs, displayed);
        return new Observation(inputs, displayed);
    }

    /** Pure assertion entry point also exercised by corruption-focused offline tests. */
    public static void assertDisplayedTotalsMatchInputs(Map<String, CartSnapshot.Row> inputs,
                                                       CartPage.DisplayedSummaryTotals displayed) {
        Assert.assertFalse(inputs.isEmpty());
        BigDecimal expectedTotal = BigDecimal.ZERO, expectedDiscount = BigDecimal.ZERO;
        for (CartSnapshot.Row row : inputs.values()) {
            BigDecimal count = BigDecimal.valueOf(row.quantity());
            expectedTotal = expectedTotal.add(row.discountedUnitPrice().multiply(count));
            expectedDiscount = expectedDiscount.add(row.originalUnitPrice().subtract(row.discountedUnitPrice()).multiply(count));
        }
        Assert.assertEquals(displayed.total().compareTo(expectedTotal), 0,
                "Separately read visible basket total equals main-card unit prices times quantities");
        Assert.assertEquals(displayed.discount().abs().compareTo(expectedDiscount), 0,
                "Separately read visible discount equals main-card original/current unit differences times quantities");
    }
}
