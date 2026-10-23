package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProductSearchResultsTest extends BaseTest {

    private static final String SEARCH_QUERY = "Sony PlayStation 5 Slim";

    @Test(groups = "smoke")
    public void searchForExistingProduct() {
        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .enterSearchQuery(SEARCH_QUERY)
                .submitSearch();

        searchPage.waitForSearchResultsPage();

        Assert.assertTrue(driver().getCurrentUrl().contains("/axtaris-neticesi"),
                "Submitting a search should open the search-results page.");
        Assert.assertTrue(searchPage.isExpectedProductResultVisible(),
                "The expected PlayStation 5 Slim product should be visible in the search results.");
    }
}
