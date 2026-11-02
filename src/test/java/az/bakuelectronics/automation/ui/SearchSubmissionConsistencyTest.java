package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("Guest search")
public class SearchSubmissionConsistencyTest extends UiExtensionSupport {
    @Story("Button and Enter submit the same query and scoped product identities")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void buttonAndEnterReturnSameProducts() {
        HomePage home = new HomePage(driver());
        SearchPage search = new SearchPage(driver());
        home.open().dismissCampaignPopupIfPresent().enterSearchQuery(PS5_QUERY).submitSearch();
        var button = evidence("Button result grid", search.waitForQuery(PS5_QUERY));
        home.open().dismissCampaignPopupIfPresent().enterSearchQuery(PS5_QUERY).submitSearchWithEnter();
        var enter = evidence("Enter result grid", search.waitForQuery(PS5_QUERY));
        Assert.assertTrue(button.slugs().contains(PS5_SLUG), "Button search must contain the known product");
        Assert.assertTrue(enter.slugs().contains(PS5_SLUG), "Enter search must contain the known product");
        Assert.assertEquals(enter.parameter("name"), button.parameter("name"), "Decoded queries must agree");
        Assert.assertEquals(enter.slugs(), button.slugs(), "Nonempty scoped product-slug sets must agree");
    }
}
