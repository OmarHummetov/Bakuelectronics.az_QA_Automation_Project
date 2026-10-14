package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("UI Automation")
@Feature("Shopping Cart")
public class AddToCartTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";

    @Story("Add product to cart from search results")
    @Test(groups = "smoke")
    public void addProductToCart() {
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
        productPage.clickAddToCart();

        Assert.assertTrue(productPage.getCartItemCount() >= 1,
                "The cart badge should be visible and indicate at least one item.");
    }
}
