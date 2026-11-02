package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.openqa.selenium.chromium.HasCdp;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.util.Map;

@Feature("Guest category filters")
public class ClearAllFiltersTest extends UiExtensionSupport {
    @Override
    @BeforeMethod(alwaysRun = true)
    public void setGuestViewport() {
        ((HasCdp) driver()).executeCdpCommand("Emulation.setDeviceMetricsOverride",
                Map.of("width", 390, "height", 844, "deviceScaleFactor", 1, "mobile", true));
    }

    @Story("Mobile clear restores category controls and products, including default sorting")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void clearRestoresUnfilteredCategory() {
        var category = new CategoryPage(driver()).openMobileGuestSmartphones();
        var baseline = evidence("Unfiltered mobile grid", category.snapshot());
        var bounds = evidence("Captured category price bounds", category.priceBounds());
        Assert.assertEquals(category.selectedFilterCheckboxCount(), 0, "Fresh category checkbox baseline");
        Assert.assertEquals(category.mobileSelectedSortLabel(), "Populyar");
        category.selectSamsungBrand().applyGuestPriceRange(400, 700);
        assertCombinedFilters(category, evidence("Mobile combined filters", category.snapshot()));
        category.closeMobileFilters().selectMobilePriceDescendingSort().openMobileFilters();
        var selected = evidence("Mobile filtered sorted grid", category.snapshot());
        assertCombinedFilters(category, selected);
        assertDescending(selected);
        Assert.assertEquals(category.mobileSelectedSortLabel(), "Bahadan ucuza");
        Assert.assertTrue(category.selectedFilterCheckboxCount() > 0);
        var cleared = evidence("After Seçilənləri sil", category.clearAllMobileFilters());
        Assert.assertEquals(category.selectedFilterCheckboxCount(), 0, "All filter checkboxes must clear");
        Assert.assertFalse(category.isSamsungSelected());
        Assert.assertEquals(category.priceBounds(), bounds, "Reset must restore both captured category bounds");
        for (String parameter : new String[]{"brand", "prices", "priceSort", "page"}) {
            Assert.assertNull(cleared.parameter(parameter), "Reset clears URL parameter " + parameter);
        }
        Assert.assertEquals(category.mobileSelectedSortLabel(), "Populyar", "Observed reset also clears sorting");
        Assert.assertNotEquals(cleared.slugs(), selected.slugs(), "Reset must replace filtered products");
        Assert.assertEquals(cleared.products(), baseline.products(), "Default category products and selling prices must return");
        category.closeMobileFilters();
    }
}
