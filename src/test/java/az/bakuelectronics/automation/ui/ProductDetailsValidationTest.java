package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProductDetailsValidationTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";

    @Test(groups = {"regression"})
    public void validateProductDetails() {
        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());
        ProductPage productPage = new ProductPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .enterSearchQuery(SEARCH_QUERY)
                .submitSearch();

        searchPage.waitForSearchResultsPage();
        Assert.assertTrue(searchPage.isExpectedProductResultVisible(),
                "The stable PlayStation product should be visible in search results.");
        searchPage.clickExpectedProductResult();

        productPage.waitForProductPage();
        Assert.assertTrue(productPage.getProductName().contains(SEARCH_QUERY),
                "The product name should be visible and match the selected product.");
        Assert.assertFalse(productPage.getCurrentPrice().isBlank(),
                "The current product price should not be blank.");
        Assert.assertFalse(productPage.getProductCode().isBlank(),
                "The product code should not be blank.");
        Assert.assertTrue(productPage.isCharacteristicsSectionVisible(),
                "The product characteristics section should be present.");
        Assert.assertTrue(productPage.isAddToCartButtonClickable(),
                "The add-to-cart button should be visible and clickable.");
    }
}
