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
public class SearchBrowserHistoryTest extends UiExtensionSupport {
    @Story("Back and Forward restore distinguishable query/result snapshots")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void historyRestoresQueryAndResults() {
        HomePage home = new HomePage(driver());
        SearchPage search = new SearchPage(driver());
        home.open().dismissCampaignPopupIfPresent().enterSearchQuery(PS5_QUERY).submitSearch();
        var first = evidence("First search", search.waitForQuery(PS5_QUERY));
        Assert.assertTrue(first.slugs().contains(PS5_SLUG), "First fixture identity");
        // Revalidated fixture: PS4 and PS5 terms return the same fuzzy-search set; Samsung is distinct.
        String secondQuery = "Samsung";
        home.enterSearchQuery(secondQuery).submitSearch();
        var second = evidence("Second search", search.waitForQuery(secondQuery, first));
        Assert.assertTrue(second.products().stream().allMatch(product -> product.title()
                        .toLowerCase(java.util.Locale.ROOT).contains("samsung")),
                "The distinct second fixture must contain Samsung search results");
        Assert.assertNotEquals(second.slugs(), first.slugs(), "History fixtures must have different identities");
        var back = evidence("Back restored", search.restoreHistory(false, PS5_QUERY));
        Assert.assertEquals(back.parameter("name"), PS5_QUERY);
        Assert.assertEquals(back.slugs(), first.slugs(), "Back must restore matching results");
        var forward = evidence("Forward restored", search.restoreHistory(true, secondQuery));
        Assert.assertEquals(forward.parameter("name"), secondQuery);
        Assert.assertEquals(forward.slugs(), second.slugs(), "Forward must restore matching results");
    }
}
