package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;

@Feature("Guest category filters")
public class IndividualFilterRemovalTest extends UiExtensionSupport {
    @Story("Removing Samsung preserves the active price range")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void removingBrandPreservesPriceRange() {
        var category = combinedCategory();
        assertCombinedFilters(category, evidence("Before removal", category.snapshot()));
        category.removeSamsungBrand();
        var remaining = evidence("After brand removal", category.snapshot());
        Assert.assertNull(remaining.parameter("brand"), "Brand parameter must disappear");
        Assert.assertFalse(category.isSamsungSelected(), "Brand checkbox must clear");
        Assert.assertEquals(remaining.parameter("prices"), "400,700", "Price parameter must survive");
        Assert.assertEquals(category.priceBounds(), List.of("400", "700"), "Both price controls must survive");
        Assert.assertTrue(remaining.parameter("page") == null || "1".equals(remaining.parameter("page")),
                "Filter removal must leave the first page active");
        assertPriceRange(remaining);
    }
}
