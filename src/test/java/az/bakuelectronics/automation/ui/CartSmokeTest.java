package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CartSmokeTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final String PRODUCT_ROUTE = "/mehsul/playstation-5-slim-blue-ray-201123";
    @Test(groups = "regression")
    public void openCartAndVerifyProduct() {
        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());
        ProductPage productPage = new ProductPage(driver());
        CartPage cartPage = new CartPage(driver());
        homePage.open()
                .dismissCampaignPopupIfPresent()
                .enterSearchQuery(SEARCH_QUERY)
                .submitSearch();

        searchPage.waitForSearchResultsPage();
        Assert.assertTrue(searchPage.isExpectedProductResultVisible(),
                "The expected PlayStation 5 Slim product should be visible in the search results.");

        searchPage.clickExpectedProductResult();
        productPage.waitForProductPage();
        Assert.assertTrue(driver().getCurrentUrl().contains(PRODUCT_ROUTE),
                "The verified PlayStation 5 Slim product page should be opened.");

        productPage.clickAddToCart();
        Assert.assertTrue(productPage.getCartItemCount() >= 1,
                "The cart badge should indicate at least one item.");

        cartPage.openCart();

        cartPage.waitForCartPage();
        Assert.assertTrue(driver().getCurrentUrl().contains("/sebet"),
                "The cart page should be opened.");
        Assert.assertTrue(cartPage.isProductVisible(),
                "The added PlayStation 5 Slim product should be visible in the cart.");
        Assert.assertTrue(cartPage.getProductQuantity() >= 1,
                "The product quantity should be at least one.");
        Assert.assertFalse(cartPage.getCartTotal().isBlank(),
                "The cart total should not be blank.");
    }
}
