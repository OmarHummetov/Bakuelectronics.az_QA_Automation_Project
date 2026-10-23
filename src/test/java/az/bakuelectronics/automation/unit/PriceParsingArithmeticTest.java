package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.utils.PriceUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;

public class PriceParsingArithmeticTest {
    @DataProvider
    public Object[][] aznPrices() {
        return new Object[][] {
                {"1,299.99 ₼", "1299.99"}, {"1.299,99 AZN", "1299.99"},
                {"1 299,90 ₼", "1299.90"}, {"1\u00a0299.90 ₼", "1299.90"},
                {"1\u202f299,90 AZN", "1299.90"}, {"AZN 1299.9", "1299.90"},
                {"₼ 1299", "1299.00"}, {"1299,99", "1299.99"},
                {"1,299 ₼", "1299.00"}, {"1.299 ₼", "1299.00"},
                {"1,234,567.89 ₼", "1234567.89"}, {"1.234.567,89 ₼", "1234567.89"},
                {"1 234 567,89 azn", "1234567.89"}, {"0,01 ₼", "0.01"},
                {" 0 ₼ ", "0.00"}, {"\t99.90\n₼", "99.90"},
                {"- 150 ₼", "-150.00"}, {"-1.00 ₼", "-1.00"},
                {"-\u00a01 234,50 AZN", "-1234.50"}, {"AZN - 0,02", "-0.02"}
        };
    }

    @Test(dataProvider = "aznPrices")
    public void parsesAznWithoutLosingPrecision(String displayed, String expected) {
        Assert.assertEquals(PriceUtils.parseAzn(displayed), new BigDecimal(expected));
    }

    @DataProvider
    public Object[][] invalidPrices() {
        return new Object[][] {
                {null}, {""}, {"₼"}, {"--1.00 ₼"}, {"12,34,56 ₼"}, {"1 29.99 ₼"},
                {"- ₼"}, {"1-00 ₼"}, {"- 1 23 ₼"},
                {"1.2345 ₼"}, {"1,234.567 ₼"}, {"NaN"}, {"99 USD"},
                {"99 ₼ 89 ₼"}, {"Price: 99 ₼"}, {"1..299 ₼"}, {"99."}, {"1e3"}
        };
    }

    @Test(dataProvider = "invalidPrices", expectedExceptions = IllegalArgumentException.class)
    public void rejectsMalformedOrUnsupportedPrices(String displayed) {
        PriceUtils.parseAzn(displayed);
    }

    @DataProvider
    public Object[][] syntheticLineAmounts() {

        return new Object[][] {
                {"19,95 ₼", "19.95 AZN", "39.90 AZN"},
                {"0.10 AZN", "0,10 ₼", "0,20 ₼"},
                {"0,29 ₼", "0.29 AZN", "0.58 AZN"},
                {"0.01 ₼", "0,01 ₼", "0,02 ₼"},
                {"1.299,99 ₼", "1,299.99 AZN", "2 599,98 ₼"},
                {"1\u202f234,50 AZN", "1 234,50 ₼", "2,469.00 AZN"},
                {"999.99 AZN", "999,99 ₼", "1.999,98 ₼"},
                {"10 ₼", "10,00 AZN", "20.0 ₼"},
                {"0 ₼", "0,00 AZN", "0.00 ₼"}
        };
    }

    @Test(dataProvider = "syntheticLineAmounts")
    public void calculatesExactLineAmountsForQuantityOneThenTwo(
            String displayedUnitPrice, String amountAtOne, String amountAtTwo) {
        BigDecimal unitPrice = PriceUtils.parseAzn(displayedUnitPrice);
        BigDecimal atOne = PriceUtils.parseAzn(amountAtOne);
        BigDecimal atTwo = PriceUtils.parseAzn(amountAtTwo);
        Assert.assertEquals(unitPrice.multiply(BigDecimal.ONE).compareTo(atOne), 0);
        Assert.assertEquals(unitPrice.multiply(BigDecimal.valueOf(2)).compareTo(atTwo), 0);
        Assert.assertEquals(atTwo.subtract(atOne).compareTo(unitPrice), 0,
                "One additional unit must add exactly one unit price in these arithmetic fixtures.");
        Assert.assertEquals(unitPrice.multiply(BigDecimal.valueOf(2))
                .compareTo(atTwo.stripTrailingZeros()), 0,
                "Numeric equality must not depend on BigDecimal scale.");
    }

    @Test
    public void detectsUnchangedAndOneQepikIncorrectAmountsAtQuantityTwo() {
        BigDecimal expected = PriceUtils.parseAzn("19,95 ₼").multiply(BigDecimal.valueOf(2));
        Assert.assertTrue(expected.compareTo(PriceUtils.parseAzn("19,95 ₼")) > 0,
                "A stale quantity-one amount must not match the quantity-two amount.");
        Assert.assertTrue(expected.compareTo(PriceUtils.parseAzn("39,89 ₼")) > 0);
        Assert.assertTrue(expected.compareTo(PriceUtils.parseAzn("39,91 ₼")) < 0);
    }

    @DataProvider
    public Object[][] syntheticDiscountedAmounts() {

        return new Object[][] {
                {"25,99 ₼", "20.99 AZN", "5 ₼", "10 AZN", "20,99 ₼", "41.98 AZN"},
                {"2.01 AZN", "1,99 ₼", "0.02 ₼", "0,04 AZN", "1.99 ₼", "3,98 AZN"},
                {"1 234,50 ₼", "999.95 AZN", "234,55 ₼", "469.10 AZN", "999,95 ₼", "1.999,90 ₼"},
                {"10 AZN", "10.00 ₼", "0 ₼", "0.00 AZN", "10 ₼", "20,00 AZN"}
        };
    }

    @Test(dataProvider = "syntheticDiscountedAmounts")
    public void calculatesDiscountAndNetTotalAtOneAndTwoWithoutDoubleSubtractingDiscount(
            String originalText, String discountedText, String discountAtOne, String discountAtTwo,
            String totalAtOne, String totalAtTwo) {
        BigDecimal originalUnit = PriceUtils.parseAzn(originalText);
        BigDecimal discountedUnit = PriceUtils.parseAzn(discountedText);
        String[] displayedDiscounts = {discountAtOne, discountAtTwo};
        String[] displayedTotals = {totalAtOne, totalAtTwo};
        for (int quantity = 1; quantity <= 2; quantity++) {
            BigDecimal count = BigDecimal.valueOf(quantity);
            BigDecimal expectedDiscount = originalUnit.subtract(discountedUnit).multiply(count);
            BigDecimal expectedTotal = discountedUnit.multiply(count);
            BigDecimal discount = PriceUtils.parseAzn(displayedDiscounts[quantity - 1]);
            BigDecimal total = PriceUtils.parseAzn(displayedTotals[quantity - 1]);
            Assert.assertEquals(discount.compareTo(expectedDiscount), 0,
                    "Discount must be the per-unit difference times quantity " + quantity);
            Assert.assertEquals(total.compareTo(expectedTotal), 0,
                    "Net total must be the already discounted unit price times quantity " + quantity);
            Assert.assertEquals(total.add(discount).compareTo(originalUnit.multiply(count)), 0,
                    "Net total plus discount must reconcile to the original total in these fixtures.");
            if (expectedDiscount.signum() > 0) {
                Assert.assertNotEquals(total.compareTo(expectedTotal.subtract(expectedDiscount)), 0,
                        "Subtracting the discount again must not match the net total.");
            }
        }
    }
}
