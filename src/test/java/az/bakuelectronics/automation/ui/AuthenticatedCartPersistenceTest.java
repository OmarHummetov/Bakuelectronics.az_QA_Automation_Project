package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.support.AuthenticatedCartSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class AuthenticatedCartPersistenceTest extends AuthenticatedCartSupport {
    @Test(groups = {"regression", "sensitive-auth", "ui-mutation", "no-retry"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void productPersistsAndOriginalAccountBasketIsRestored() {
        scenario(() -> {
            at("inspect absent product A");
            Product a = inspect(A);
            at("add test-owned product A");
            add(a);
            at("verify exact identity quantity and pricing");
            var expected = summary(Map.of(a.slug(), a), Map.of(a.slug(), 1));
            inspectOwnedPriceStructure(a.slug());
            at("refresh and verify persistence");
            driver().navigate().refresh();
            Assert.assertEquals(summary(Map.of(a.slug(), a), Map.of(a.slug(), 1)), expected);
            at("navigate away and back and verify persistence");
            driver().get(ConfigReader.getUiBaseUrl() + "/");
            cart.openCart();
            Assert.assertEquals(summary(Map.of(a.slug(), a), Map.of(a.slug(), 1)), expected);
            at("close browser reauthenticate and verify test-owned product persists");
            reopenAndVerify();
            Assert.assertEquals(summary(Map.of(a.slug(), a), Map.of(a.slug(), 1)), expected);
            at("remove only test-owned product A");
            cart.removeOwnedProduct(a.slug());
            Assert.assertEquals(cart.waitForInventory(), ownership.baseline());
        });
    }
}
