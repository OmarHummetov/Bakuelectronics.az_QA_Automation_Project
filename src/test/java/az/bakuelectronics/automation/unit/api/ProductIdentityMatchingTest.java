package az.bakuelectronics.automation.unit.api;

import az.bakuelectronics.automation.api.ProductIdentityMatcher;
import az.bakuelectronics.automation.models.ProductApiModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;

public class ProductIdentityMatchingTest {

    @Test
    public void matchesBySlugBeforeName() {
        Assert.assertTrue(ProductIdentityMatcher.matches(product("a", "Same name", "sku-a"),
                product("a", "Different name", "sku-b")));
    }

    @Test
    public void differentSkuDoesNotCreateFalsePositiveWhenNamesMatch() {
        Assert.assertFalse(ProductIdentityMatcher.matches(product(null, "Same name", "sku-a"),
                product(null, "Same name", "sku-b")));
    }

    @Test
    public void fallsBackToIdAndThenNormalizedName() {
        Assert.assertTrue(ProductIdentityMatcher.matches(new ProductApiModel(42L, "Name", null, null,
                        BigDecimal.ONE, null),
                new ProductApiModel(42L, "Other", null, null, BigDecimal.ONE, null)));
        Assert.assertTrue(ProductIdentityMatcher.matches(product(null, "  Brand   Model ", null),
                product(null, "brand model", null)));
    }

    @Test
    public void blankProductsNeverMatch() {
        Assert.assertFalse(ProductIdentityMatcher.matches(product(null, "", null),
                product(null, "", null)));
    }

    private static ProductApiModel product(String slug, String name, String sku) {
        return new ProductApiModel(null, name, sku, slug, BigDecimal.ONE, null);
    }
}
