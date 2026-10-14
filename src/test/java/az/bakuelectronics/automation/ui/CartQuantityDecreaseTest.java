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

public class CartQuantityDecreaseTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";

    @Test(groups = "regression")
    public void decreaseProductQuantity() {
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

        cartPage.increaseQuantity();

        WebDriverWait wait = new WebDriverWait(driver(), Duration.ofSeconds(10));
        wait.until(webDriver -> cartPage.getProductQuantity() == initialQuantity + 1);

        int increasedQuantity = cartPage.getProductQuantity();
        Assert.assertEquals(increasedQuantity, initialQuantity + 1,
                "The product quantity should increase by one.");
        String increasedCartTotal = cartPage.getCartTotal();

        cartPage.decreaseQuantity();
        wait.until(webDriver -> cartPage.getProductQuantity() == initialQuantity);

        int finalQuantity = cartPage.getProductQuantity();
        Assert.assertEquals(finalQuantity, initialQuantity,
                "The product quantity should return to the original quantity.");

        String finalCartTotal = cartPage.getCartTotal();
        Assert.assertNotEquals(finalCartTotal, increasedCartTotal,
                "The cart total should change after decreasing the product quantity.");
    }
}
