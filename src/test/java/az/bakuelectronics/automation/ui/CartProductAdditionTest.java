package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.GuestCartTestSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("UI Automation")
@Feature("Shopping Cart")
public class CartProductAdditionTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final String PRODUCT_SLUG = "playstation-5-slim-blue-ray-201123";

    @Story("Add product to cart from search results")
    @Test(groups = "smoke", retryAnalyzer = NoRetryAnalyzer.class)
    public void addProductToCart() {
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

            Assert.assertTrue(productPage.getCartItemCount() >= 1,
                    "The cart badge should be visible after adding the product.");

            cartPage.openCart();
            cartPage.waitForCartPage();
            Assert.assertTrue(cartPage.isProductVisible(PRODUCT_SLUG),
                    "The added PlayStation 5 Slim product should be present in the cart.");
            Assert.assertEquals(cartPage.waitForProductQuantity(PRODUCT_SLUG, 1), 1,
                    "Adding once to the empty guest cart should produce quantity one.");
        });
    }
}
