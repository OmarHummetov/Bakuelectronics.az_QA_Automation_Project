package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.ProductPriceComponent;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;

@Feature("Guest catalog price accuracy")
public class ProductCardDetailPriceParityTest extends UiExtensionSupport {
    @Story("Two distinct catalog cards agree with their exact details on identity and current selling price")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void twoDistinctCardsMatchTheirDetails() {
        new CategoryPage(driver()).openGuestSmartphones().selectSamsungBrand().applyGuestPriceRange(400, 700);
        var prices = new ProductPriceComponent(driver());
        var cards = evidence("Strict scoped catalog selling-price quotes", prices.catalogQuotes());
        Assert.assertTrue(cards.size() >= 2, "Need at least two distinct priced cards; no empty comparison PASS");
        var selected = List.of(cards.getFirst(), cards.getLast());
        Assert.assertNotEquals(selected.getFirst().slug(), selected.getLast().slug(), "Compare distinct products");
        int comparisons = 0;
        for (var card : selected) {
            var detail = evidence("Exact detail for " + card.slug(), prices.openDetail(card));
            Assert.assertEquals(detail.slug(), card.slug(), "Exact captured card route; no fuzzy lookup");
            Assert.assertEquals(detail.name(), card.name(), "Exact product identity after whitespace normalization");
            Assert.assertEquals(detail.sellingPrice().compareTo(card.sellingPrice()), 0,
                    "Current selling price must agree in AZN (BigDecimal), excluding crossed-out prices: " + card.slug());
            comparisons++;
        }
        Assert.assertEquals(comparisons, 2, "Both nonempty product comparisons must execute");
    }
}
