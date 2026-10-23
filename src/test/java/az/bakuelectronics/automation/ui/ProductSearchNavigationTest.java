package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProductSearchNavigationTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final String EXPECTED_PRODUCT_CODE = "201123";

    @Test(groups = "regression")
    public void openExpectedProduct() {
        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());
        ProductPage productPage = new ProductPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .enterSearchQuery(SEARCH_QUERY)
                .submitSearch();

        searchPage.waitForSearchResultsPage();
        Assert.assertTrue(searchPage.isExpectedProductResultVisible(),
                "The expected PlayStation 5 Slim product should be visible in the search results.");

        searchPage.clickExpectedProductResult();
        productPage.waitForProductPage();

        Assert.assertTrue(driver().getCurrentUrl().contains("/mehsul/"),
                "The expected product page URL should contain /mehsul/.");
        Assert.assertTrue(productPage.getProductName().contains(SEARCH_QUERY),
                "The product name should contain the expected PlayStation 5 Slim text.");
        Assert.assertEquals(productPage.getProductCode(), EXPECTED_PRODUCT_CODE,
                "The product code should match the expected product.");
        Assert.assertFalse(productPage.getCurrentPrice().isBlank(),
                "The current product price should not be blank.");
    }
}
