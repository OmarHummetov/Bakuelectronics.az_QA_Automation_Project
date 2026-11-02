package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.ProductGrid;
import io.qameta.allure.Allure;
import org.openqa.selenium.Dimension;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class UiExtensionSupport extends BaseTest {
    public static final String PS5_QUERY = "Sony PlayStation 5 Slim";
    public static final String PS5_SLUG = "playstation-5-slim-blue-ray-201123";

    @BeforeMethod(alwaysRun = true)
    public void setGuestViewport() { driver().manage().window().setSize(new Dimension(1440, 1000)); }

    protected CategoryPage combinedCategory() {
        return new CategoryPage(driver()).openGuestSmartphones()
                .selectSamsungBrand().applyGuestPriceRange(400, 700);
    }

    protected <T> T evidence(String name, T value) {
        Allure.attachment(name, "text/plain", new ByteArrayInputStream(
                ("Viewport (CSS pixels): " + ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript(
                        "return innerWidth + 'x' + innerHeight;") + "\n" + value).getBytes(StandardCharsets.UTF_8)),
                io.qameta.allure.AttachmentOptions.withFileExtension(".txt"));
        return value;
    }

    protected void assertPriceRange(ProductGrid.Snapshot snapshot) {
        Assert.assertFalse(snapshot.products().isEmpty(), "Filtered grid must contain products");
        for (ProductGrid.Product product : snapshot.products()) {
            Assert.assertNotNull(product.sellingPrice(), "Price filtering requires a displayed selling price: " + product);
            Assert.assertTrue(product.sellingPrice().compareTo(BigDecimal.valueOf(400)) >= 0
                            && product.sellingPrice().compareTo(BigDecimal.valueOf(700)) <= 0,
                    "Every card must sell within 400–700 AZN: " + product);
        }
    }

    protected void assertCombinedFilters(CategoryPage category, ProductGrid.Snapshot snapshot) {
        assertPriceRange(snapshot);
        Assert.assertEquals(snapshot.parameter("brand"), "samsung", "Brand URL state");
        Assert.assertEquals(snapshot.parameter("prices"), "400,700", "Price URL state");
        Assert.assertTrue(category.isSamsungSelected(), "Samsung checkbox must remain selected");
        Assert.assertEquals(category.priceBounds(), List.of("400", "700"), "Price controls must retain both bounds");
        for (ProductGrid.Product product : snapshot.products()) {
            Assert.assertTrue(product.title().toLowerCase(Locale.ROOT).contains("samsung"),
                    "The same priced card must also match Samsung: " + product);
        }
    }

    protected void assertDescending(ProductGrid.Snapshot snapshot) {
        Assert.assertEquals(snapshot.parameter("priceSort"), "desc", "Descending sort URL state");
        Assert.assertTrue(snapshot.products().size() >= 3, "Need at least three selling prices");
        for (ProductGrid.Product product : snapshot.products()) {
            Assert.assertNotNull(product.sellingPrice(), "Sorting requires a displayed selling price: " + product);
        }
        for (int index = 1; index < snapshot.products().size(); index++) {
            Assert.assertTrue(snapshot.products().get(index - 1).sellingPrice()
                            .compareTo(snapshot.products().get(index).sellingPrice()) >= 0,
                    "Selling prices must be non-increasing at index " + index + ": " + snapshot);
        }
    }
}
