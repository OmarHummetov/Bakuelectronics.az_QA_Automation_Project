package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;

@Feature("Guest related product navigation")
public class RelatedProductsNavigationTest extends UiExtensionSupport {
    @Story("A captured recommendation title link reaches the matching product identity")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void relatedTitleLinkReachesCapturedProduct() {
        var product = new ProductPage(driver()).openGuestPlayStation();
        Assert.assertEquals(product.getProductCode(), "201123");
        String source = driver().getCurrentUrl();
        var related = product.relatedProducts();
        var destination = evidence("Captured related title link", related.captureFirst());
        Assert.assertFalse(destination.name().isBlank());
        Assert.assertEquals(URI.create(destination.href()).getHost(), URI.create(source).getHost());
        Assert.assertNotEquals(URI.create(destination.href()).getPath(), URI.create(source).getPath());
        Assert.assertTrue(URI.create(destination.href()).getPath().startsWith("/mehsul/"));
        related.follow(destination);
        evidence("Settled destination", driver().getCurrentUrl() + " | " + product.getProductName() + " | " + product.getProductCode());
        Assert.assertEquals(URI.create(driver().getCurrentUrl()).getPath(), URI.create(destination.href()).getPath());
        Assert.assertEquals(product.getProductName(), destination.name());
        Assert.assertFalse(product.getProductCode().isBlank());
        Assert.assertNotEquals(product.getProductCode(), "201123");
    }
}
