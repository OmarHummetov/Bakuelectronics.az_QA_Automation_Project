package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.JsonUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class NegativeSearchTest extends BaseTest {

    @DataProvider(name = "nonexistentSearchQueries")
    public Object[][] nonexistentSearchQueries() {
        String[] searchQueries = JsonUtils.readStringArray(
                "testdata/negative-search-data.json", "searchQueries");
        Object[][] data = new Object[searchQueries.length][1];
        for (int index = 0; index < searchQueries.length; index++) {
            data[index][0] = searchQueries[index];
        }
        return data;
    }

    @Test(groups = "regression", dataProvider = "nonexistentSearchQueries")
    public void searchForNonexistentProductShowsNoResultsState(String searchQuery) {
        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .enterSearchQuery(searchQuery)
                .submitSearch();

        searchPage.waitForSearchResultsPage();

        Assert.assertTrue(searchPage.isNoResultsMessageVisible(),
                "A nonexistent product search should show the site's zero-results state for: " + searchQuery);
    }
}
