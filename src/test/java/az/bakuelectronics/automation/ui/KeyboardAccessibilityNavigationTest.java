package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;
import org.testng.annotations.Test;

@Feature("Guest keyboard accessibility")
public class KeyboardAccessibilityNavigationTest extends UiExtensionSupport {
    @Story("Tab and reverse Tab reach catalog; Enter opens and Escape dismisses with visible focus")
    @Description("Bounded header-to-catalog journey, at most 25 initial Tabs. Only catalog receives Enter; no purchase, authentication or submission control is activated. Known focus/Escape defects remain failures.")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void keyboardCatalogHasVisibleFocusAndEscapeDismissal() {
        var catalog = new HomePage(driver()).open().dismissCampaignPopupIfPresent().catalog();
        var unfocused = evidence("Unfocused catalog styles", catalog.catalogFocus());
        evidence("Bounded Tab trace", catalog.tabToCatalog());
        var focused = evidence("Keyboard focused catalog", catalog.activeFocus());
        Assert.assertEquals(focused.name(), "Kataloq");
        Assert.assertTrue(focused.visible() && focused.focusVisible(), "Catalog must be in viewport and keyboard focused");
        var reverse = evidence("Shift+Tab previous public link", catalog.tab(true));
        Assert.assertTrue(reverse.classes().contains("header_nav__logo__"), "Reverse Tab reaches the preceding logo");
        Assert.assertEquals(evidence("Tab returns to catalog", catalog.tab(false)).name(), focused.name());
        // Some sessions restore an open catalog. Normalize through its real keyboard toggle before opening.
        if (catalog.settledMenu().open()) Assert.assertFalse(catalog.enterCatalog().open());
        Assert.assertTrue(evidence("Enter opens catalog", catalog.enterCatalog()).open());
        var next = evidence("Tab leaves catalog for public search input", catalog.tab(false));
        Assert.assertEquals(next.id(), "input", "No keyboard trap at catalog");
        Assert.assertEquals(evidence("Shift+Tab returns from search", catalog.tab(true)).name(), "Kataloq");
        var escaped = evidence("Menu after Escape (actual settled state)", catalog.escape());
        var checks = new SoftAssert();
        boolean outline = !focused.outlineStyle().equals("none") && !focused.outlineWidth().equals("0px");
        boolean changedStyle = !focused.shadow().equals(unfocused.shadow())
                || !focused.background().equals(unfocused.background()) || !focused.border().equals(unfocused.border());
        checks.assertTrue(outline || changedStyle, "Catalog must show a visible focus indication; :focus-visible alone is insufficient");
        checks.assertFalse(escaped.open(), "Escape must dismiss the keyboard-opened catalog");
        checks.assertEquals(catalog.activeFocus().name(), "Kataloq", "Escape preserves focus on its public trigger");
        checks.assertAll();
    }
}
