package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.GuestCartTestSupport;
import az.bakuelectronics.automation.utils.PriceUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;

public class CartQuantityPriceValidationTest extends BaseTest {
    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final String PRODUCT_SLUG = "playstation-5-slim-blue-ray-201123";

    @Test(groups = {"regression", "ui-mutation", "no-retry"})
    public void shouldIncreaseProductQuantityFromOneToTwo() {


        HomePage homePage = new HomePage(driver());
        CartPage cartPage = new CartPage(driver());
        ProductPage productPage = new ProductPage(driver());
        SearchPage searchPage = new SearchPage(driver());
        homePage.open().dismissCampaignPopupIfPresent();
        GuestCartTestSupport.withEmptyCart(cartPage, PRODUCT_SLUG, () -> {
            homePage.open().dismissCampaignPopupIfPresent()
                    .enterSearchQuery(SEARCH_QUERY).submitSearch();
            searchPage.waitForSearchResultsPage();
            Assert.assertTrue(searchPage.isExpectedProductResultVisible());
            searchPage.clickExpectedProductResult();
            productPage.waitForProductPage();
            Assert.assertTrue(driver().getCurrentUrl().contains("/mehsul/" + PRODUCT_SLUG),
                    "The selected product must match the cart row being tested.");
            BigDecimal unitPrice = PriceUtils.parseAzn(productPage.getCurrentPrice());
            Assert.assertTrue(unitPrice.signum() > 0, "The current product price must be positive AZN.");
            String productName = productPage.getProductName();

            productPage.clickAddToCart();
            cartPage.openCart();
            cartPage.waitForCartPage();
            Assert.assertTrue(cartPage.isProductVisible(PRODUCT_SLUG));
            Assert.assertEquals(cartPage.getProductQuantity(PRODUCT_SLUG), 1,
                    "Adding the product once must produce quantity one.");
            CartPage.PriceSummary atOne = cartPage.waitForPriceSummary(PRODUCT_SLUG, productName, 1);
            assertSummary(atOne, 1);
            cartPage.increaseQuantity(PRODUCT_SLUG);
            Assert.assertEquals(cartPage.getProductQuantity(PRODUCT_SLUG), 2,
                    "Clicking this product's plus button once must produce quantity two.");
            CartPage.PriceSummary atTwo = cartPage.waitForPriceSummary(PRODUCT_SLUG, productName, 2);
            Assert.assertEquals(atTwo.originalUnitPrice().compareTo(atOne.originalUnitPrice()), 0,
                    "Original unit price must remain unchanged after increasing quantity.");
            Assert.assertEquals(atTwo.discountedUnitPrice().compareTo(atOne.discountedUnitPrice()), 0,
                    "Discounted unit price must remain unchanged after increasing quantity.");
            assertSummary(atTwo, 2);
        });
    }

    private static void assertSummary(CartPage.PriceSummary summary, int quantity) {
        BigDecimal count = BigDecimal.valueOf(quantity);
        Assert.assertEquals(summary.quantity(), quantity, "Summary quantity must match the cart control.");
        Assert.assertEquals(summary.grandTotal().compareTo(summary.discountedUnitPrice().multiply(count)), 0,
                "Grand total must equal discounted unit price × quantity, with no extra fees.");
        Assert.assertEquals(summary.discount().abs().compareTo(summary.originalUnitPrice()
                .subtract(summary.discountedUnitPrice()).multiply(count)), 0,
                "Absolute discount must equal (original - discounted unit price) × quantity.");
    }
}
