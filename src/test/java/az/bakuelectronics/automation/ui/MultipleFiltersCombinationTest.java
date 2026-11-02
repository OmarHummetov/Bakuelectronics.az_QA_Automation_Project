package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

@Feature("Guest category filters")
public class MultipleFiltersCombinationTest extends UiExtensionSupport {
    @Story("Every product satisfies the brand and selling-price intersection")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void brandAndPriceApplyTogether() {
        var category = combinedCategory();
        assertCombinedFilters(category, evidence("Combined grid", category.snapshot()));
    }
}
