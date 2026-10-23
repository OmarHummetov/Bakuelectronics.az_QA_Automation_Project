package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.GuestCartTestSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CartQuantityIncreaseTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final String PRODUCT_SLUG = "playstation-5-slim-blue-ray-201123";

    @Test(groups = "regression", retryAnalyzer = NoRetryAnalyzer.class)
    public void increaseProductQuantity() {
        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());
        ProductPage productPage = new ProductPage(driver());
        CartPage cartPage = new CartPage(driver());

        homePage.open().dismissCampaignPopupIfPresent();
        GuestCartTestSupport.withEmptyCart(cartPage, PRODUCT_SLUG, () -> {
            homePage.open()
                    .dismissCampaignPopupIfPresent()
                    .enterSearchQuery(SEARCH_QUERY)
                    .submitSearch();

            searchPage.waitForSearchResultsPage();
            Assert.assertTrue(searchPage.isExpectedProductResultVisible(),
                    "The expected PlayStation 5 Slim product should be visible in the search results.");

            searchPage.clickExpectedProductResult();
            productPage.waitForProductPage();
            Assert.assertTrue(driver().getCurrentUrl().contains("/mehsul/" + PRODUCT_SLUG),
                    "The selected product must match the product owned by this test.");
            productPage.clickAddToCart();

            cartPage.openCart();
            cartPage.waitForCartPage();

            int initialQuantity = cartPage.waitForProductQuantity(PRODUCT_SLUG, 1);
            Assert.assertEquals(initialQuantity, 1,
                    "Adding once to the empty guest cart should produce quantity one.");
            String initialCartTotal = cartPage.getCartTotal();

            cartPage.increaseQuantity(PRODUCT_SLUG);

            int newQuantity = cartPage.waitForProductQuantity(PRODUCT_SLUG, initialQuantity + 1);
            Assert.assertEquals(newQuantity, initialQuantity + 1,
                    "The product quantity should increase by one.");

            String newCartTotal = cartPage.getCartTotal();
            Assert.assertNotEquals(newCartTotal, initialCartTotal,
                    "The cart total should change after increasing the product quantity.");
        });
    }
}
