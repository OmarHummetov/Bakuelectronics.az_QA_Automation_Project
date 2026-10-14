package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CategoryNavigationTest extends BaseTest {

    @Test(groups = "smoke")
    public void navigateFromHomeToSmartphonesCategory() {
        HomePage homePage = new HomePage(driver());
        CategoryPage categoryPage = new CategoryPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .openSmartphonesCategory();
        categoryPage.waitForSmartphonesCategory();

        Assert.assertTrue(categoryPage.isSmartphonesCategoryUrl(driver().getCurrentUrl()),
                "The URL should be the Smartfonlar category route.");
        Assert.assertEquals(categoryPage.getCategoryTitle(), "Smartfonlar",
                "The Smartfonlar category title should be visible.");
        Assert.assertTrue(categoryPage.hasProducts(),
                "The Smartfonlar category should contain products.");
    }
}
