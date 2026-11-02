package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.support.AccountCartOwnership;
import az.bakuelectronics.automation.support.AuthenticatedCartSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import io.qameta.allure.Allure;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Authentication and complete inventory checks only; no cart mutation or ownership journal. */
public class AuthenticatedCartPreflightTest extends AuthenticatedCartSupport {
    @Test(groups = {"regression", "sensitive-auth", "no-retry"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void freshAuthenticationAndReauthenticationPreserveBasketWithoutMutation() {
        BasketTestSupport.lock();
        try {
            login();
            var baseline = cart.waitForInventory();
            var prices = baseline.isEmpty() ? null : cart.waitForAccountSnapshot();
            ownership = new AccountCartOwnership(baseline);
            reopenAndVerify();
            Assert.assertEquals(cart.waitForInventory(), baseline);
            driver().navigate().refresh();
            Assert.assertEquals(cart.waitForInventory(), baseline);
            if (prices != null) Assert.assertEquals(cart.waitForAccountSnapshot(), prices);
            Allure.attachment("Read-only authenticated preflight", "PASS: fresh login, browser closure, reauthentication and complete original basket verified; no cart mutations");
        } finally { BasketTestSupport.unlock(); }
    }
}
