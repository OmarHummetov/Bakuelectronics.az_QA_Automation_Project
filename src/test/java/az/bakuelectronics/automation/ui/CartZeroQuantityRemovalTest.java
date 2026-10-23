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

public class CartZeroQuantityRemovalTest extends BaseTest {
    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final String PRODUCT_SLUG = "playstation-5-slim-blue-ray-201123";

    @Test(groups = "regression", retryAnalyzer = NoRetryAnalyzer.class)
    public void shouldRemoveGuestProductWhenQuantityDecreasedFromOneToZero() {
        HomePage home = new HomePage(driver());
        SearchPage search = new SearchPage(driver());
        ProductPage product = new ProductPage(driver());
        CartPage cart = new CartPage(driver());

        home.open().dismissCampaignPopupIfPresent();

        GuestCartTestSupport.withEmptyCart(cart, PRODUCT_SLUG, () -> {
            home.open().dismissCampaignPopupIfPresent().enterSearchQuery(SEARCH_QUERY).submitSearch();
            search.waitForSearchResultsPage();
            Assert.assertTrue(search.isExpectedProductResultVisible(),
                    "The known test product must be available in search results.");
            search.clickExpectedProductResult();
            product.waitForProductPage();
            Assert.assertTrue(driver().getCurrentUrl().contains("/mehsul/" + PRODUCT_SLUG),
                    "The selected product must match the product owned by this test.");
            product.clickAddToCart();

            cart.openCart();
            cart.waitForCartPage();
            Assert.assertTrue(cart.isProductVisible(PRODUCT_SLUG),
                    "The added test product must be visible in the guest cart.");
            Assert.assertEquals(cart.waitForProductQuantity(PRODUCT_SLUG, 1), 1,
                    "A single add to the empty guest cart must result in quantity one.");

            cart.decreaseQuantity(PRODUCT_SLUG);
            cart.waitForProductToDisappear(PRODUCT_SLUG);
            Assert.assertTrue(cart.isCartEmpty(),
                    "Decreasing quantity from one to zero must remove the product and empty the guest cart.");
        });
    }
}
