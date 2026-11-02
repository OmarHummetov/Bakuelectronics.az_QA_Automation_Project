package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("Guest category sorting")
public class ProductPriceDescendingSortTest extends UiExtensionSupport {
    @Story("Actual selling prices decrease across the complete results grid")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void sellingPricesAreDescending() {
        var category = new CategoryPage(driver()).openGuestSmartphones().selectPriceDescendingSort();
        var sorted = evidence("Descending selling prices", category.snapshot());
        assertDescending(sorted);
        Assert.assertEquals(category.selectedSortLabel(), "Bahadan ucuza", "Selected desktop sort control");
        Assert.assertTrue(sorted.products().stream().map(product -> product.sellingPrice())
                .distinct().count() >= 2, "Equal prices alone cannot demonstrate sort direction");
    }
}
