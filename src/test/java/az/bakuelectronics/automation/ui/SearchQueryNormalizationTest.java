package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.pages.ProductGrid;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;
import java.util.Locale;

@Feature("Guest search")
public class SearchQueryNormalizationTest extends UiExtensionSupport {
    @Story("Equivalent query spellings retain nonempty product identity sets")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void equivalentQueriesReturnSameProducts() {
        HomePage home = new HomePage(driver());
        SearchPage search = new SearchPage(driver());
        ProductGrid.Snapshot baseline = null;
        for (String query : List.of(PS5_QUERY, PS5_QUERY.toLowerCase(Locale.ROOT),
                "  " + PS5_QUERY + "  ", PS5_QUERY.replace(" ", "   "))) {
            // Fresh document prevents the equivalent old grid from satisfying a transition wait.
            home.open().dismissCampaignPopupIfPresent().enterSearchQuery(query).submitSearch();
            var result = evidence("Submitted query: [" + query + "]", search.settledSubmittedResults());
            Assert.assertFalse(result.slugs().isEmpty(), "Each variant must return scoped identities");
            Assert.assertTrue(result.slugs().contains(PS5_SLUG), "Every query must retain product code 201123");
            Assert.assertTrue(result.products().stream().filter(p -> p.slug().equals(PS5_SLUG))
                    .allMatch(p -> p.title().contains("PlayStation 5 Slim")), "Expected slug must carry the expected title");
            if (baseline == null) baseline = result;
            else Assert.assertEquals(result.slugs(), baseline.slugs(), "Equivalent queries must return the complete same identity set");
        }
    }
}
