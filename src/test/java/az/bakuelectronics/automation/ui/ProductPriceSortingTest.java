package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

public class ProductPriceSortingTest extends BaseTest {

    @Test(groups = {"regression"})
    public void sortSmartphonesByAscendingPrice() {
        HomePage homePage = new HomePage(driver());
        CategoryPage categoryPage = new CategoryPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .openSmartphonesCategory();
        categoryPage.waitForSmartphonesCategory();

        Assert.assertTrue(categoryPage.hasProducts(),
                "The initial smartphones list should contain products.");

        categoryPage.selectPriceAscendingSort();
        List<BigDecimal> prices = categoryPage.getProductPrices();

        Assert.assertTrue(prices.size() >= 3,
                "At least three numeric product prices should be available.");
        for (int index = 1; index < prices.size(); index++) {
            Assert.assertTrue(prices.get(index - 1).compareTo(prices.get(index)) <= 0,
                    "Product prices should be sorted from low to high: " + prices);
        }
    }
}
