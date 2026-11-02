package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Collections;

@Feature("Guest category pagination")
public class FilterSortPaginationPersistenceTest extends UiExtensionSupport {
    @Story("Brand, range and descending order survive navigation to page two")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void filtersAndSortPersistAcrossPages() {
        var category = combinedCategory().selectPriceDescendingSort();
        var first = evidence("Sorted filtered page one", category.snapshot());
        assertCombinedFilters(category, first);
        assertDescending(first);
        Assert.assertEquals(category.selectedSortLabel(), "Bahadan ucuza", "Page-one selected sort control");
        var second = evidence("Sorted filtered page two", category.nextGuestPage());
        Assert.assertTrue(category.isSecondPageSelected(), "Page two must have aria-current=true");
        Assert.assertEquals(second.parameter("page"), "2");
        assertCombinedFilters(category, second);
        assertDescending(second);
        Assert.assertEquals(category.selectedSortLabel(), "Bahadan ucuza", "Page-two selected sort control");
        Assert.assertTrue(Collections.disjoint(first.slugs(), second.slugs()), "Pages must contain distinct identities");
        Assert.assertTrue(first.products().getLast().sellingPrice()
                        .compareTo(second.products().getFirst().sellingPrice()) >= 0,
                "Last page-one selling price must be >= first page-two selling price");
    }
}
