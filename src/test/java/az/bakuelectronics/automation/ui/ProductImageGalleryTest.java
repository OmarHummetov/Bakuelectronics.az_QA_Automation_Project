package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("Guest product details")
public class ProductImageGalleryTest extends UiExtensionSupport {
    @Story("Selecting another thumbnail changes and successfully loads the product image")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void thumbnailChangesLoadedProductImage() {
        var product = new ProductPage(driver()).openGuestPlayStation();
        Assert.assertEquals(product.getProductCode(), "201123", "Gallery fixture identity");
        String route = driver().getCurrentUrl();
        String name = product.getProductName();
        var gallery = product.gallery();
        var before = evidence("Initial gallery image", gallery.loadedActiveImage());
        String selected = gallery.selectDifferentThumbnail(before.source());
        var after = evidence("Selected gallery image", gallery.loadedActiveImage());
        Assert.assertNotEquals(after.source(), before.source(), "Gallery asset must change");
        Assert.assertEquals(after.source(), selected, "Active asset must match selected thumbnail");
        Assert.assertTrue(after.naturalWidth() > 0 && after.naturalHeight() > 0, "Selected image must load");
        Assert.assertEquals(driver().getCurrentUrl(), route, "Gallery must retain the product route");
        Assert.assertEquals(product.getProductName(), name);
        Assert.assertEquals(product.getProductCode(), "201123");
    }
}
