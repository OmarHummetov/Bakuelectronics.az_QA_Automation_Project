package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

public class ProductPriceFilterTest extends BaseTest {

    private static final int MINIMUM_PRICE = 400;
    private static final int MAXIMUM_PRICE = 700;

    @Test(groups = {"regression"})
    public void filterSmartphonesByPriceRange() {
        HomePage homePage = new HomePage(driver());
        CategoryPage categoryPage = new CategoryPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .openSmartphonesCategory();
        categoryPage.waitForSmartphonesCategory();

        categoryPage.filterByPriceRange(MINIMUM_PRICE, MAXIMUM_PRICE);
        List<BigDecimal> prices = categoryPage.getProductPrices();

        Assert.assertFalse(prices.isEmpty(), "The price filter should return at least one product.");
        for (BigDecimal price : prices) {
            Assert.assertTrue(price.compareTo(BigDecimal.valueOf(MINIMUM_PRICE)) >= 0,
                    "Product price should be at least " + MINIMUM_PRICE + ": " + price);
            Assert.assertTrue(price.compareTo(BigDecimal.valueOf(MAXIMUM_PRICE)) <= 0,
                    "Product price should be at most " + MAXIMUM_PRICE + ": " + price);
        }
    }
}
