package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PaginationTest extends BaseTest {

    @Test(groups = {"regression"})
    public void navigateToSecondSmartphonesCategoryPage() {
        HomePage homePage = new HomePage(driver());
        CategoryPage categoryPage = new CategoryPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .openSmartphonesCategory();
        categoryPage.waitForSmartphonesCategory();

        String firstPageUrl = driver().getCurrentUrl();
        String firstPageProductTitle = categoryPage.getFirstProductTitle();

        Assert.assertTrue(categoryPage.hasProducts(), "The first page should contain products.");
        Assert.assertTrue(categoryPage.isPaginationVisible(),
                "Pagination should be visible for the Smartfonlar category.");

        categoryPage.goToNextPage();

        Assert.assertTrue(!driver().getCurrentUrl().equals(firstPageUrl)
                        || !categoryPage.getFirstProductTitle().equals(firstPageProductTitle),
                "The URL or product content should change after moving to the next page.");
        Assert.assertTrue(categoryPage.hasProducts(), "The second page should contain products.");
    }
}
