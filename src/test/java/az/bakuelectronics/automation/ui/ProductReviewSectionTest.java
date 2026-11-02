package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("Guest product reviews")
public class ProductReviewSectionTest extends UiExtensionSupport {
    @Story("Product-associated review accordion opens and closes without review submission")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void reviewsExpandAndCollapseForSameProduct() {
        var product = new ProductPage(driver()).openGuestPlayStation();
        Assert.assertEquals(product.getProductCode(), "201123");
        product.gallery().loadedActiveImage();
        var reviews = product.reviews();
        var before = evidence("Collapsed product reviews", reviews.state(false));
        Assert.assertFalse(before.expanded());
        Assert.assertFalse(before.panelVisible());
        Assert.assertTrue(before.toggleText().contains("Baxmaq üçün kliklə"));
        Assert.assertTrue(before.count() >= 0, "Displayed main-product review count, including zero, must parse");
        var opened = evidence("Expanded product reviews", reviews.expand());
        Assert.assertTrue(opened.expanded());
        Assert.assertTrue(opened.panelVisible(), "Review panel must become visible");
        Assert.assertTrue(opened.toggleText().contains("Kiçiltmək üçün kliklə"));
        Assert.assertEquals(opened.productCode(), "201123");
        Assert.assertEquals(opened.productName(), before.productName());
        Assert.assertEquals(opened.url(), before.url());
        Assert.assertEquals(opened.count(), before.count(), "Opening must preserve the displayed product review count");
        var closed = evidence("Collapsed again", reviews.collapse());
        Assert.assertFalse(closed.expanded());
        Assert.assertFalse(closed.panelVisible());
        Assert.assertEquals(closed, before, "Collapse must restore the original product-associated review state");
    }
}
