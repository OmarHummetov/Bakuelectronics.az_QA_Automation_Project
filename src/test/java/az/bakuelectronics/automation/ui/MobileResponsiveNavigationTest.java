package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.CatalogNavigationComponent;
import az.bakuelectronics.automation.support.UiExtensionSupport;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.openqa.selenium.chromium.HasCdp;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;
import java.util.Map;

@Feature("Guest responsive catalog navigation")
public class MobileResponsiveNavigationTest extends UiExtensionSupport {
    @Story("390px and 414px mobile catalog opens, closes and reaches smartphones through both levels")
    @Test(groups = {"regression", "read-only"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void mobileCatalogReachesSmartphonesAtBothWidths() {
        for (int width : new int[]{390, 414}) {
            ((HasCdp) driver()).executeCdpCommand("Emulation.setDeviceMetricsOverride",
                    Map.of("width", width, "height", 844, "deviceScaleFactor", 1, "mobile", true));
            var catalog = new HomePage(driver()).open().dismissCampaignPopupIfPresent().catalog();
            var closed = evidence(width + " initial menu", catalog.settledMenu());
            Assert.assertEquals(closed.viewportWidth(), (long) width);
            Assert.assertEquals(closed.viewportHeight(), 844L);
            Assert.assertFalse(closed.open());
            Assert.assertTrue(evidence(width + " open", catalog.openMobile()).open());
            Assert.assertFalse(evidence(width + " closed", catalog.closeMobile()).open());
            Assert.assertTrue(evidence(width + " reopened", catalog.openMobile()).open());
            var grid = evidence(width + " smartphone destination", catalog.navigateMobileSmartphones());
            Assert.assertEquals(URI.create(grid.url()).getPath(), CatalogNavigationComponent.SMARTPHONES);
            Assert.assertFalse(grid.products().isEmpty());
            Assert.assertFalse(evidence(width + " destination menu", catalog.settledMenu()).open(), "Navigation must dismiss catalog overlay");
        }
    }
}
