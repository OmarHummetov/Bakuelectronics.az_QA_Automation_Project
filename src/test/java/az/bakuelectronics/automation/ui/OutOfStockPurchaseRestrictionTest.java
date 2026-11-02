package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;

@Feature("Guest product availability")
public class OutOfStockPurchaseRestrictionTest extends UiExtensionSupport {
    @Story("Currently unavailable PS4 exposes notification instead of a usable primary cart action")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void unavailableProductHasNoPrimaryCartAction() {
        var product = new ProductPage(driver()).openGuestOutOfStockCandidate();
        var state = evidence("Current main-product purchase state (UI evidence only)", product.settledPurchaseState());
        Assert.assertEquals(state.code(), "215083", "Revalidate candidate identity");
        Assert.assertEquals(URI.create(state.url()).getPath(), "/mehsul/sony-playstation-4-slim-500gb-cuh-2216a-215083");
        Assert.assertTrue(state.name().contains("PlayStation 4 Slim"));
        Assert.assertTrue(state.stockText().contains("Stokda yoxdur"),
                "Candidate must currently be unavailable; changed inventory fails this fixture explicitly");
        Assert.assertEquals(state.notificationText(), "Gələndə xəbər et");
        Assert.assertTrue(state.notificationUsable(), "Visible notification action must be enabled; never activate it");
        Assert.assertEquals(state.usableCartActions(), 0,
                "Main product purchase region must have no usable add-to-cart action (UI restriction only)");
    }
}
