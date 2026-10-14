package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

public class CartRemoveProductTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";
    private static final By CART_PRODUCT = By.xpath(
            "(//main//a[contains(@href, '/mehsul/')])[1]");

    @Test(groups = "regression")
    public void removeProductFromCart() {
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
        Assert.assertTrue(cartPage.isProductVisible(),
                "The added PlayStation 5 Slim product should be visible in the cart.");

        cartPage.removeProduct();

        WebDriverWait wait = new WebDriverWait(driver(), Duration.ofSeconds(10));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(CART_PRODUCT));

        Assert.assertTrue(cartPage.isCartEmpty(),
                "The cart should be empty after removing the product.");
    }
}
