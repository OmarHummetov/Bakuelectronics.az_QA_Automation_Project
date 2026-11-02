package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.CategoryPage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;

@Feature("Guest product navigation")
public class ProductBreadcrumbNavigationTest extends UiExtensionSupport {
    @Story("The captured category breadcrumb reaches its exact category with products")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void breadcrumbReachesCapturedCategory() {
        var product = new ProductPage(driver()).openGuestPlayStation();
        Assert.assertEquals(product.getProductCode(), "201123", "Breadcrumb fixture identity");
        var crumb = evidence("Captured category breadcrumb", product.captureCategoryBreadcrumb());
        Assert.assertEquals(URI.create(crumb.href()).getPath(),
                "/catalog/oyun-konsollari-oyunlar/oyun-konsollari");
        product.followCategoryBreadcrumb(crumb);
        var category = new CategoryPage(driver());
        Assert.assertEquals(category.waitForCategoryDestination(crumb.href()), crumb.title(),
                "Category heading must match the captured breadcrumb label");
        Assert.assertEquals(URI.create(driver().getCurrentUrl()).getPath(), URI.create(crumb.href()).getPath());
        Assert.assertFalse(evidence("Destination category grid", category.snapshot()).products().isEmpty());
    }
}
