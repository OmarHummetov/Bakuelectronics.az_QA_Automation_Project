package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

public class CartQuantityIncreaseTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";

    @Test(groups = "regression")
    public void increaseProductQuantity() {
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
        productPage.clickAddToCart();

        cartPage.openCart();
        cartPage.waitForCartPage();

        int initialQuantity = cartPage.getProductQuantity();
        Assert.assertTrue(initialQuantity >= 1,
                "The initial product quantity should be at least one.");
        String initialCartTotal = cartPage.getCartTotal();

        cartPage.increaseQuantity();

        WebDriverWait wait = new WebDriverWait(driver(), Duration.ofSeconds(10));
        wait.until(webDriver -> cartPage.getProductQuantity() != initialQuantity);

        int newQuantity = cartPage.getProductQuantity();
        Assert.assertEquals(newQuantity, initialQuantity + 1,
                "The product quantity should increase by one.");

        String newCartTotal = cartPage.getCartTotal();
        Assert.assertNotEquals(newCartTotal, initialCartTotal,
                "The cart total should change after increasing the product quantity.");
    }
}
