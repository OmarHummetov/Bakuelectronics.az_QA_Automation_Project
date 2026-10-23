package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class ProductBrandFilterTest extends BaseTest {

    private static final String SELECTED_BRAND = "Samsung";

    @Test(groups = {"regression"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void filterSmartphonesByBrand() {
        HomePage homePage = new HomePage(driver());
        homePage.open().dismissCampaignPopupIfPresent();
        homePage.openSmartphonesCategory();

        CategoryPage categoryPage = new CategoryPage(driver());
        categoryPage.waitForSmartphonesCategory().selectBrand(SELECTED_BRAND);
        List<String> productTitles = categoryPage.getProductTitles();

        Assert.assertFalse(productTitles.isEmpty(),
                "The selected brand filter should return at least one product.");
        for (String productTitle : productTitles) {
            Assert.assertTrue(productTitle.toLowerCase().contains(SELECTED_BRAND.toLowerCase()),
                    "Every filtered product should match the selected brand: " + productTitle);
        }
    }
}
