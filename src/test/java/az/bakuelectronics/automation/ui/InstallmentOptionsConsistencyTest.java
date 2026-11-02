package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.InstallmentCalculatorComponent;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.math.BigDecimal;
import java.util.List;

@Feature("Guest installment calculator")
public class InstallmentOptionsConsistencyTest extends UiExtensionSupport {
    @Story("Provider selection and 3/6 month updates retain the displayed total")
    @Description("Observed zero-initial-payment 3/6 month options only. Checks nearest-cent consistency against the displayed calculator total; no fee or lending formula is assumed.")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void providerAndTermChangesKeepDisplayedAmountsConsistent() {
        var product = new ProductPage(driver()).openGuestPlayStation();
        String name = product.getProductName(), url = driver().getCurrentUrl();
        Assert.assertEquals(product.getProductCode(), "201123");
        var calculator = product.installments();
        Assert.assertEquals(calculator.providers(), List.of("Nisyə al", "Birbank", "Bolkart", "Tamkart"));
        var initial = evidence("Initial calculator", calculator.settled());
        Assert.assertEquals(initial.provider(), "Nisyə al");
        Assert.assertEquals(initial.months(), 18);
        for (String provider : calculator.providers()) {
            var selected = evidence("Provider " + provider, calculator.selectProvider(provider));
            Assert.assertEquals(selected.provider(), provider);
            Assert.assertEquals(selected.selectedProviders(), 1);
            Assert.assertEquals(selected.terms(), provider.equals("Nisyə al")
                    ? List.of("3 ay", "6 ay", "9 ay", "12 ay", "15 ay", "18 ay", "21 ay", "24 ay")
                    : List.of("3 ay", "6 ay", "9 ay", "12 ay"));
            var three = evidence(provider + " 3 months", calculator.selectTerm(3));
            var six = evidence(provider + " 6 months", calculator.selectTerm(6));
            assertAmount(three, provider, 3);
            assertAmount(six, provider, 6);
            Assert.assertEquals(three.total(), selected.total(), "Term changes retain displayed total");
            Assert.assertEquals(six.total(), three.total());
            Assert.assertTrue(six.monthly().compareTo(three.monthly()) < 0, "Longer verified term must update monthly amount");
            Assert.assertEquals(evidence(provider + " restored 3 months", calculator.selectTerm(3)), three,
                    "Returning to the same option must reproduce its complete stable state");
        }
        Assert.assertEquals(driver().getCurrentUrl(), url);
        Assert.assertEquals(product.getProductName(), name);
        Assert.assertEquals(product.getProductCode(), "201123");
    }

    private void assertAmount(InstallmentCalculatorComponent.State state, String provider, int months) {
        Assert.assertEquals(state.provider(), provider);
        Assert.assertEquals(state.months(), months);
        Assert.assertEquals(state.selectedProviders(), 1);
        Assert.assertEquals(state.selectedTerms(), 1);
        Assert.assertTrue(state.initialPayment().isEmpty() || state.initialPayment().equals("0"), "No initial payment is entered");
        Assert.assertTrue(state.monthly().signum() > 0 && state.total().signum() > 0);
        // Observed 1449.99 / 6 displays 241.66, /18 displays 80.56. Tie direction is not contractual.
        BigDecimal count = BigDecimal.valueOf(months);
        Assert.assertTrue(state.monthly().multiply(count).subtract(state.total()).abs()
                        .compareTo(new BigDecimal("0.005").multiply(count)) <= 0,
                "Displayed monthly must lie within half a cent of displayed total/term: " + state);
    }
}
