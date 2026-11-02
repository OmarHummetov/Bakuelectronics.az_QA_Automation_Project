package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.driver.DriverManager;
import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.utils.CampaignPopup;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("Guest category shareable URLs")
public class CategoryFilterDeepLinkStateTest extends UiExtensionSupport {
    @Story("A real brand/range/sort URL restores data and controls in a new guest browser")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void freshGuestRestoresFilteredDeepLink() {
        var category = combinedCategory().selectPriceDescendingSort();
        var source = evidence("Canonical UI-generated shareable URL and products", category.snapshot());
        assertCombinedFilters(category, source);
        assertDescending(source);
        Assert.assertEquals(category.selectedSortLabel(), "Bahadan ucuza");
        String canonicalUrl = source.url();

        // A separate Chrome process/profile, rather than refresh, new tab or pagination in the same session.
        DriverManager.quitDriver();
        DriverManager.createDriver();
        setGuestViewport();
        driver().get(canonicalUrl);
        CampaignPopup.dismissIfPresent(driver());
        category = new CategoryPage(driver());
        var restored = evidence("Fresh guest restored grid", category.snapshot());
        evidence("Fresh guest restored controls", "brand=" + category.isSamsungSelected()
                + "; bounds=" + category.priceBounds() + "; sort=" + category.selectedSortLabel());
        Assert.assertEquals(restored.url(), canonicalUrl, "Canonical route and all query parameters survive");
        assertCombinedFilters(category, restored);
        assertDescending(restored);
        Assert.assertEquals(restored.products(), source.products(), "Complete ordered card data survives a fresh browser");
        Assert.assertEquals(category.selectedSortLabel(), "Bahadan ucuza", "Deep link must restore selected sort control");
    }
}
