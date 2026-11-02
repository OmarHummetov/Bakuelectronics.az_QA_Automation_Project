package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.ProductGrid;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UiExtensionContractTest {
    @Test
    public void defaultCampaignHandlingUsesRealCloseButton() {
        withCampaignSuppression(null, () -> {
            Assert.assertFalse(az.bakuelectronics.automation.config.ConfigReader.isCampaignPopupSuppressionEnabled(),
                    "Ordinary runs must not install document-start campaign suppression");
            assertCampaignCloseUsesDriver();
        });
    }

    @Test
    public void explicitFalseCampaignHandlingStillUsesRealCloseButton() {
        withCampaignSuppression("false", UiExtensionContractTest::assertCampaignCloseUsesDriver);
    }

    @Test
    public void explicitCampaignSuppressionRemainsOptIn() {
        withCampaignSuppression("true", () -> {
            Assert.assertTrue(az.bakuelectronics.automation.config.ConfigReader.isCampaignPopupSuppressionEnabled());
            az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(campaignDriver(null));
        });
    }

    @Test
    public void campaignCloseDoesNotSwallowClickInterception() {
        withCampaignSuppression(null, () -> {
            var button = (org.openqa.selenium.WebElement) java.lang.reflect.Proxy.newProxyInstance(
                    UiExtensionContractTest.class.getClassLoader(), new Class<?>[]{org.openqa.selenium.WebElement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isDisplayed", "isEnabled" -> true;
                        case "click" -> throw new org.openqa.selenium.ElementClickInterceptedException("Fixture backdrop");
                        default -> throw new AssertionError("Unexpected button operation: " + method.getName());
                    });
            Assert.expectThrows(org.openqa.selenium.ElementClickInterceptedException.class,
                    () -> az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(campaignDriver(button)));
        });
    }

    private static void assertCampaignCloseUsesDriver() {
        var clicks = new java.util.concurrent.atomic.AtomicInteger();
        var button = (org.openqa.selenium.WebElement) java.lang.reflect.Proxy.newProxyInstance(
                UiExtensionContractTest.class.getClassLoader(), new Class<?>[]{org.openqa.selenium.WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isDisplayed", "isEnabled" -> true;
                    case "click" -> { clicks.incrementAndGet(); yield null; }
                    default -> throw new AssertionError("Unexpected button operation: " + method.getName());
                });
        az.bakuelectronics.automation.utils.CampaignPopup.dismissIfPresent(campaignDriver(button));
        Assert.assertEquals(clicks.get(), 1, "Dismiss through the real control exactly once");
    }

    private static org.openqa.selenium.WebDriver campaignDriver(org.openqa.selenium.WebElement button) {
        return (org.openqa.selenium.WebDriver) java.lang.reflect.Proxy.newProxyInstance(
                UiExtensionContractTest.class.getClassLoader(), new Class<?>[]{org.openqa.selenium.WebDriver.class},
                (proxy, method, args) -> {
                    if (button != null && method.getName().equals("findElement")) {
                        Assert.assertEquals(args[0], org.openqa.selenium.By.cssSelector("button.campaignModalClose"));
                        return button;
                    }
                    throw new AssertionError("Unexpected browser operation: " + method.getName());
                });
    }

    private static void withCampaignSuppression(String value, Runnable check) {
        String key = "ui.campaignPopup.suppress";
        String previous = System.getProperty(key);
        try {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
            check.run();
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test
    public void phaseFiveQuoteNormalizesOnlyNameWhitespace() {
        var quote = az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(
                "/mehsul/phone-123", List.of("  Samsung\u00a0 Galaxy  A18\nSilver "), List.of("649.99 ₼"));
        Assert.assertEquals(quote.slug(), "phone-123");
        Assert.assertEquals(quote.name(), "Samsung Galaxy A18 Silver");
        Assert.assertEquals(quote.sellingPrice(), new BigDecimal("649.99"));
    }

    @Test
    public void phaseFiveQuoteKeepsExactCentsAndAllowsScaleIndependentComparison() {
        var a = az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(
                "/mehsul/a", List.of("A"), List.of("649,99 AZN"));
        var b = az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(
                "/mehsul/b", List.of("B"), List.of("650 ₼"));
        Assert.assertEquals(a.sellingPrice().subtract(b.sellingPrice()), new BigDecimal("-0.01"));
        Assert.assertEquals(b.sellingPrice().compareTo(new BigDecimal("650.000")), 0);
    }

    @Test
    public void phaseFiveQuoteRejectsMissingAndNonpositiveCurrentPrices() {
        for (List<String> prices : List.of(List.<String>of(), List.of(""), List.of("0"), List.of("-1"))) {
            Assert.expectThrows(IllegalArgumentException.class, () ->
                    az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote("/mehsul/a", List.of("A"), prices));
        }
    }

    @Test
    public void phaseFiveQuoteRejectsAmbiguousAmountsAndNames() {
        Assert.expectThrows(IllegalArgumentException.class, () ->
                az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(
                        "/mehsul/a", List.of("A"), List.of("699.99", "649.99")));
        Assert.expectThrows(IllegalArgumentException.class, () ->
                az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(
                        "/mehsul/a", List.of("A", "B"), List.of("649.99")));
        Assert.expectThrows(IllegalArgumentException.class, () ->
                az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(
                        "/mehsul/a", List.of("A"), List.of("699.99 649.99")));
    }

    @Test
    public void phaseFiveQuoteRejectsMissingOrNonproductIdentity() {
        for (String href : List.of("/mehsul/", "/catalog/a", "/mehsul/a/b")) {
            Assert.expectThrows(IllegalArgumentException.class, () ->
                    az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote(href, List.of("A"), List.of("1")));
        }
        Assert.expectThrows(IllegalArgumentException.class, () ->
                az.bakuelectronics.automation.pages.ProductPriceComponent.parseQuote("/mehsul/a", List.of(" "), List.of("1")));
    }

    @Test
    public void phaseFiveSelectsExactlyTwoReadOnlyMethodsOutsideAllPreviousSuites() throws Exception {
        var selected = classes("testng-ui-extension-phase5.xml");
        Assert.assertEquals(selected, Set.of("az.bakuelectronics.automation.ui.CategoryFilterDeepLinkStateTest",
                "az.bakuelectronics.automation.ui.ProductCardDetailPriceParityTest"));
        Assert.assertEquals(xml("testng-ui-extension-phase5.xml").getElementsByTagName("package").getLength(), 0);
        for (String name : selected) {
            for (String previous : List.of("testng.xml", "testng-ui.xml", "testng-offline.xml", "testng-api.xml",
                    "testng-api-known-issues.xml", "testng-integration.xml", "testng-safe-full-run-with-integration.xml",
                    "testng-parallel-ui-pilot.xml", "testng-ui-extension.xml", "testng-ui-extension-phase2.xml",
                    "testng-ui-extension-phase3.xml")) {
                Assert.assertFalse(classes(previous).contains(name), "Phase 5 leaked into " + previous);
            }
            var methods = Arrays.stream(Class.forName(name).getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Test.class)).toList();
            Assert.assertEquals(methods.size(), 1);
            var test = methods.getFirst().getAnnotation(Test.class);
            Assert.assertEquals(Set.copyOf(Arrays.asList(test.groups())), Set.of("regression", "read-only"));
            Assert.assertEquals(test.retryAnalyzer(), NoRetryAnalyzer.class);
            Assert.assertEquals(test.invocationCount(), 1);
            Assert.assertTrue(test.enabled() && test.dataProvider().isEmpty() && test.dependsOnMethods().length == 0);
        }
    }

    @Test
    public void parsesOnlyExplicitPositiveInstallmentTerms() {
        Assert.assertEquals(az.bakuelectronics.automation.pages.InstallmentCalculatorComponent.parseMonths("3 ay"), 3);
        Assert.assertEquals(az.bakuelectronics.automation.pages.InstallmentCalculatorComponent.parseMonths("24 ay"), 24);
    }

    @Test
    public void unsupportedInstallmentTermsNeverBecomeDefaultPeriods() {
        for (String text : Arrays.asList(null, "", "0 ay", "-3 ay", "3", "3 ay 6 ay", "3 months", "3.5 ay")) {
            Assert.expectThrows(IllegalArgumentException.class,
                    () -> az.bakuelectronics.automation.pages.InstallmentCalculatorComponent.parseMonths(text));
        }
    }

    @Test
    public void phaseThreeIsExplicitReadOnlyAndDisjointFromPreviousSuites() throws Exception {
        var selected = classes("testng-ui-extension-phase3.xml");
        Assert.assertEquals(selected, Set.of("az.bakuelectronics.automation.ui.InstallmentOptionsConsistencyTest",
                "az.bakuelectronics.automation.ui.RelatedProductsNavigationTest",
                "az.bakuelectronics.automation.ui.MobileResponsiveNavigationTest",
                "az.bakuelectronics.automation.ui.KeyboardAccessibilityNavigationTest"));
        Assert.assertEquals(xml("testng-ui-extension-phase3.xml").getElementsByTagName("package").getLength(), 0);
        for (String name : selected) {
            for (String previous : List.of("testng.xml", "testng-ui.xml", "testng-offline.xml",
                    "testng-ui-extension.xml", "testng-ui-extension-phase2.xml")) {
                Assert.assertFalse(classes(previous).contains(name), "Phase 3 leaked into " + previous);
            }
            int count = 0;
            for (var method : Class.forName(name).getDeclaredMethods()) {
                var test = method.getAnnotation(Test.class);
                if (test == null) continue;
                count++;
                Assert.assertEquals(Set.copyOf(Arrays.asList(test.groups())), Set.of("regression", "read-only"));
                Assert.assertEquals(test.retryAnalyzer(), NoRetryAnalyzer.class);
                Assert.assertEquals(test.invocationCount(), 1);
                Assert.assertTrue(test.enabled() && test.dataProvider().isEmpty() && test.dependsOnMethods().length == 0);
                Assert.assertNotNull(method.getAnnotation(io.qameta.allure.Story.class));
            }
            Assert.assertEquals(count, 1);
            Assert.assertNotNull(Class.forName(name).getAnnotation(io.qameta.allure.Feature.class));
        }
    }

    @Test
    public void parsesOnlyDisplayedProductReviewCounts() {
        Assert.assertEquals(az.bakuelectronics.automation.pages.ProductReviewsComponent.parseReviewCount("0rəy"), 0);
        Assert.assertEquals(az.bakuelectronics.automation.pages.ProductReviewsComponent.parseReviewCount(" 12\u00a0rəy "), 12);
        Assert.assertEquals(az.bakuelectronics.automation.pages.ProductReviewsComponent.parseReviewCount("3 rəy"), 3);
    }

    @Test
    public void missingOrAmbiguousReviewCountsNeverBecomeEmptySuccess() {
        for (String text : Arrays.asList(null, "", "rəy", "-1 rəy", "0 rəy 2 rəy", "Rəylər", "4 stars")) {
            Assert.expectThrows(IllegalArgumentException.class,
                    () -> az.bakuelectronics.automation.pages.ProductReviewsComponent.parseReviewCount(text));
        }
    }

    @Test
    public void searchUrlSpellingDoesNotNormalizeIdentityContracts() {
        var baseline = ProductGrid.parseSnapshot("https://example.test/?name=Sony+PlayStation", List.of(
                List.of("/mehsul/ps5", "PlayStation 5", "100 ₼")));
        var variant = ProductGrid.parseSnapshot("https://example.test/?name=++sony+++playstation++", List.of(
                List.of("/mehsul/ps5", "PlayStation 5", "100 ₼")));
        Assert.assertNotEquals(variant.parameter("name"), baseline.parameter("name"));
        Assert.assertEquals(variant.parameter("name"), "  sony   playstation  ");
        Assert.assertEquals(variant.slugs(), baseline.slugs());
    }

    @Test
    public void phaseTwoHasExactlyFourIsolatedReadOnlyMethods() throws Exception {
        var phaseTwo = classes("testng-ui-extension-phase2.xml");
        Assert.assertEquals(phaseTwo, Set.of("az.bakuelectronics.automation.ui.SearchQueryNormalizationTest",
                "az.bakuelectronics.automation.ui.ClearAllFiltersTest",
                "az.bakuelectronics.automation.ui.OutOfStockPurchaseRestrictionTest",
                "az.bakuelectronics.automation.ui.ProductReviewSectionTest"));
        Assert.assertEquals(xml("testng-ui-extension-phase2.xml").getElementsByTagName("package").getLength(), 0);
        for (String name : phaseTwo) {
            for (String suite : List.of("testng.xml", "testng-ui.xml", "testng-offline.xml", "testng-ui-extension.xml")) {
                Assert.assertFalse(classes(suite).contains(name), "Phase 2 leaked into " + suite);
            }
            int methods = 0;
            for (var method : Class.forName(name).getDeclaredMethods()) {
                var test = method.getAnnotation(Test.class);
                if (test == null) continue;
                methods++;
                Assert.assertEquals(Set.copyOf(Arrays.asList(test.groups())), Set.of("regression", "read-only"));
                Assert.assertEquals(test.retryAnalyzer(), NoRetryAnalyzer.class);
                Assert.assertTrue(test.dataProvider().isEmpty());
                Assert.assertEquals(test.invocationCount(), 1);
                Assert.assertNotNull(method.getAnnotation(io.qameta.allure.Story.class));
            }
            Assert.assertEquals(methods, 1, name);
            Assert.assertNotNull(Class.forName(name).getAnnotation(io.qameta.allure.Feature.class));
        }
        var excludes = xml("testng-ui-extension-phase2.xml").getElementsByTagName("exclude");
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < excludes.getLength(); i++) names.add(((org.w3c.dom.Element) excludes.item(i)).getAttribute("name"));
        Assert.assertEquals(names, Set.of("manual-auth", "sensitive-auth"));
    }

    @Test
    public void sparseSearchPricesDoNotDropOutOfStockIdentities() {
        var snapshot = ProductGrid.parseSnapshot("https://example.test/?name=PlayStation", List.of(
                List.of("/mehsul/available", "Available PlayStation", "1449.99 ₼"),
                List.of("/mehsul/out-of-stock", "Out of stock PlayStation", "")));
        Assert.assertEquals(snapshot.slugs(), Set.of("available", "out-of-stock"));
        Assert.assertEquals(snapshot.products().getFirst().sellingPrice(), new BigDecimal("1449.99"));
        Assert.assertNull(snapshot.products().getLast().sellingPrice(), "Missing price must stay explicit, never zero or a copied price");
        Assert.expectThrows(IllegalArgumentException.class, () -> ProductGrid.parseSnapshot("https://example.test/",
                List.of(List.of("/catalog/wrong", "Not a product", "100 ₼"))));
    }

    @Test
    public void decodesObservedQueryParameters() {
        Assert.assertEquals(ProductGrid.queryParameter("https://example.test/?name=Sony+PlayStation%205&prices=400%2C700", "name"),
                "Sony PlayStation 5");
        Assert.assertEquals(ProductGrid.queryParameter("https://example.test/?prices=400%2C700", "prices"), "400,700");
        Assert.assertNull(ProductGrid.queryParameter("https://example.test/", "brand"));
        Assert.assertNull(ProductGrid.queryParameter("https://example.test/?other=brand", "brand"));
    }

    @Test
    public void rejectsEmptyOrDuplicateResultIdentities() {
        Assert.expectThrows(IllegalArgumentException.class, () -> new ProductGrid.Snapshot("https://example.test/", List.of()));
        Assert.expectThrows(IllegalArgumentException.class, () -> new ProductGrid.Snapshot("https://example.test/",
                List.of(product("same", "100"), product("same", "200"))));
    }

    @Test
    public void rejectsMissingIdentityTitleAndNonpositiveSellingPrices() {
        for (var product : List.of(new ProductGrid.Product("", "Phone", BigDecimal.ONE),
                new ProductGrid.Product("phone", "", BigDecimal.ONE), product("phone", "0"), product("phone", "-1"))) {
            Assert.expectThrows(IllegalArgumentException.class,
                    () -> new ProductGrid.Snapshot("https://example.test/", List.of(product)));
        }
    }

    @Test
    public void snapshotIsImmutableAndSlugParityIgnoresOrdering() {
        var input = new ArrayList<>(List.of(product("one", "649.99"), product("two", "419.99")));
        var first = new ProductGrid.Snapshot("https://example.test/", input);
        input.clear();
        var reversed = new ProductGrid.Snapshot("https://example.test/", List.of(first.products().getLast(), first.products().getFirst()));
        Assert.assertEquals(first.products().size(), 2);
        Assert.assertEquals(first.slugs(), reversed.slugs());
        Assert.assertNotEquals(first.products(), reversed.products(), "Sorting snapshots retain ordered card data");
        Assert.expectThrows(UnsupportedOperationException.class, () -> first.products().clear());
    }

    @Test
    public void fullSuiteRetainsExactStandardUiSelection() throws Exception {
        Document full = xml("testng.xml");
        var tests = full.getElementsByTagName("test");
        Set<String> selected = new LinkedHashSet<>();
        for (int i = 0; i < tests.getLength(); i++) {
            var test = (org.w3c.dom.Element) tests.item(i);
            if (test.getAttribute("name").equals("UI Tests")) {
                Assert.assertEquals(test.getElementsByTagName("package").getLength(), 0, "No UI package scanning");
                var classes = test.getElementsByTagName("class");
                for (int c = 0; c < classes.getLength(); c++) selected.add(((org.w3c.dom.Element) classes.item(c)).getAttribute("name"));
                Assert.assertEquals(test.getElementsByTagName("exclude").getLength(), 2, "Protected auth exclusions remain");
            }
        }
        Assert.assertEquals(selected, classes("testng-ui.xml"));
        Assert.assertEquals(selected.size(), 20);
    }

    @Test
    public void extensionHasExactlyEightReadOnlyMethodsWithoutRetries() throws Exception {
        Set<String> extension = classes("testng-ui-extension.xml");
        Assert.assertEquals(extension.size(), 8);
        int methods = 0;
        for (String name : extension) {
            Assert.assertFalse(classes("testng-ui.xml").contains(name));
            for (var method : Class.forName(name).getDeclaredMethods()) {
                Test annotation = method.getAnnotation(Test.class);
                if (annotation == null) continue;
                methods++;
                Assert.assertEquals(Set.copyOf(Arrays.asList(annotation.groups())), Set.of("regression", "read-only"));
                Assert.assertEquals(annotation.retryAnalyzer(), NoRetryAnalyzer.class);
                Assert.assertTrue(annotation.dataProvider().isEmpty());
            }
        }
        Assert.assertEquals(methods, 8);
    }

    @Test
    public void standardSelectionStillProducesTwentyTwoInvocations() throws Exception {
        int methods = 0;
        int invocations = 0;
        for (String name : classes("testng-ui.xml")) {
            Class<?> type = Class.forName(name);
            for (var method : type.getDeclaredMethods()) {
                Test annotation = method.getAnnotation(Test.class);
                if (annotation == null || Arrays.stream(annotation.groups())
                        .anyMatch(group -> group.equals("manual-auth") || group.equals("sensitive-auth"))) continue;
                methods++;
                if (annotation.dataProvider().isEmpty()) invocations++;
                else {
                    var provider = Arrays.stream(type.getDeclaredMethods()).filter(candidate -> {
                        var data = candidate.getAnnotation(org.testng.annotations.DataProvider.class);
                        return data != null && data.name().equals(annotation.dataProvider());
                    }).findFirst().orElseThrow();
                    invocations += ((Object[][]) provider.invoke(type.getConstructor().newInstance())).length;
                }
            }
        }
        Assert.assertEquals(methods, 20);
        Assert.assertEquals(invocations, 22);
    }

    @Test
    public void protectedAuthenticationGroupsRemainExcluded() throws Exception {
        for (String path : List.of("testng-ui.xml", "testng-ui-extension.xml")) {
            var excludes = xml(path).getElementsByTagName("exclude");
            Set<String> names = new LinkedHashSet<>();
            for (int i = 0; i < excludes.getLength(); i++) {
                names.add(((org.w3c.dom.Element) excludes.item(i)).getAttribute("name"));
            }
            Assert.assertEquals(names, Set.of("manual-auth", "sensitive-auth"), path);
        }
    }

    private static ProductGrid.Product product(String slug, String price) {
        return new ProductGrid.Product(slug, "Phone " + slug, new BigDecimal(price));
    }

    private static Set<String> classes(String path) throws Exception {
        var nodes = xml(path).getElementsByTagName("class");
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) result.add(((org.w3c.dom.Element) nodes.item(i)).getAttribute("name"));
        return result;
    }

    private static Document xml(String path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(Path.of(path).toFile());
    }
}
