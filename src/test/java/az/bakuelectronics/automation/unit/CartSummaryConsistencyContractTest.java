package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.CartSnapshot;
import az.bakuelectronics.automation.ui.AuthenticatedMultiProductCartSummaryConsistencyTest;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CartSummaryConsistencyContractTest {
    private List<?> mainRows() {
        return List.of(List.of("a", "Synthetic A", "25.99", "20.99", "2"),
                List.of("b", "Synthetic B", "10.00", "9.50", "1"));
    }

    private CartPage page(List<?> rows, String discount, String total, AtomicInteger totalReads) {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    Assert.assertEquals(method.getName(), "executeScript", "No browser navigation or mutation is permitted");
                    String script = (String) args[0];
                    if (script.contains("sebet_BasketProductsItemPriceCurrent__")) {
                        Assert.assertFalse(script.contains("sebet_BasketRightBodyItemPrice"));
                        Assert.assertFalse(script.contains("sebet_TotalTopPrice__"));
                        return rows;
                    }
                    Assert.assertTrue(script.contains("sebet_TotalTopPrice__"));
                    Assert.assertTrue(script.contains("sebet_BasketRightDiscountPrice__"));
                    Assert.assertFalse(script.contains("sebet_BasketProductsItemPrice"));
                    Assert.assertFalse(script.contains("XPathResult"), "Summary reader must not read main quantity controls");
                    totalReads.incrementAndGet();
                    return List.of(discount, total);
                });
        return new CartPage(driver, Duration.ofSeconds(3));
    }

    @Test public void independentlyScopedReadsProduceExactCentAssertionsAndImmutableInputs() {
        var reads = new AtomicInteger();
        var page = page(mainRows(), "-10.50", "51.48", reads);
        var inputs = page.waitForDisplayedProductInputs();
        var displayed = page.waitForDisplayedSummaryTotals();
        Assert.assertEquals(reads.get(), 2);
        Assert.assertEquals(inputs.get("a").quantity(), 2);
        Assert.assertEquals(inputs.get("a").discountedUnitPrice(), new BigDecimal("20.99"));
        Assert.expectThrows(UnsupportedOperationException.class, inputs::clear);
        AuthenticatedMultiProductCartSummaryConsistencyTest.assertDisplayedTotalsMatchInputs(inputs, displayed);
    }

    @Test public void incorrectDisplayedTotalIsReadVerbatimAndFailsInsteadOfBeingWaitedAway() {
        var reads = new AtomicInteger();
        var page = page(mainRows(), "-10.50", "51.49", reads);
        var inputs = page.waitForDisplayedProductInputs();
        var displayed = page.waitForDisplayedSummaryTotals();
        Assert.assertEquals(reads.get(), 2);
        Assert.assertEquals(displayed.total(), new BigDecimal("51.49"));
        Assert.expectThrows(AssertionError.class, () ->
                AuthenticatedMultiProductCartSummaryConsistencyTest.assertDisplayedTotalsMatchInputs(inputs, displayed));
    }

    @DataProvider public Object[][] inconsistentTotals() {
        return new Object[][]{{"-10.49", "51.48"}, {"-10.50", "40.98"}, {"-10.50", "52.48"}};
    }
    @Test(dataProvider = "inconsistentTotals")
    public void rejectsIncorrectDiscountDoubleDiscountAndUnexpectedTotalCharge(String discount, String total) {
        var inputs = page(mainRows(), "-10.50", "51.48", new AtomicInteger()).waitForDisplayedProductInputs();
        var wrong = new CartPage.DisplayedSummaryTotals(new BigDecimal(discount), new BigDecimal(total));
        Assert.expectThrows(AssertionError.class, () ->
                AuthenticatedMultiProductCartSummaryConsistencyTest.assertDisplayedTotalsMatchInputs(inputs, wrong));
    }

    @Test public void existingAccountRowsContributeToTheFullExpectedBasket() {
        var inputs = new HashMap<>(page(mainRows(), "-10.50", "51.48", new AtomicInteger()).waitForDisplayedProductInputs());
        inputs.put("existing", new CartSnapshot.Row("existing", "Synthetic existing", new BigDecimal("2.00"),
                new BigDecimal("2.00"), 3));
        AuthenticatedMultiProductCartSummaryConsistencyTest.assertDisplayedTotalsMatchInputs(inputs,
                new CartPage.DisplayedSummaryTotals(new BigDecimal("10.50"), new BigDecimal("57.48")));
        Assert.expectThrows(AssertionError.class, () ->
                AuthenticatedMultiProductCartSummaryConsistencyTest.assertDisplayedTotalsMatchInputs(inputs,
                        new CartPage.DisplayedSummaryTotals(new BigDecimal("10.50"), new BigDecimal("51.48"))));
    }

    @DataProvider public Object[][] invalidInputs() {
        return new Object[][]{
                {List.of(List.of("a", "A", "10", "9", "1"), List.of("a", "B", "10", "9", "1"))},
                {List.of(List.of("a", "Same", "10", "9", "1"), List.of("b", "Same", "10", "9", "1"))},
                {List.of(List.of("a", "A", "10", "9", "0"))},
                {List.of(List.of("a", "A", "8", "9", "1"))}
        };
    }
    @Test(dataProvider = "invalidInputs") public void ambiguousIdentityQuantityOrPriceStopsTheReader(List<?> rows) {
        Assert.expectThrows(IllegalArgumentException.class,
                () -> page(rows, "1", "9", new AtomicInteger()).waitForDisplayedProductInputs());
    }

    @Test public void unsupportedVisibleChargeDoesNotBecomeAnAssumedZero() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    Assert.assertEquals(method.getName(), "executeScript");
                    Assert.assertTrue(((String) args[0]).contains("Unsupported displayed summary charge or amount"));
                    throw new JavascriptException("Synthetic unsupported summary component");
                });
        Assert.expectThrows(JavascriptException.class,
                () -> new CartPage(driver, Duration.ofSeconds(1)).waitForDisplayedSummaryTotals());
    }
}
