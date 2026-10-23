package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;


public class CartPageNavigationTest {
    @Test
    public void opensCartWithoutModal() {
        Dom dom = new Dom();
        dom.page.openCart();
        Assert.assertEquals(dom.cartClicks, 1);
        Assert.assertEquals(dom.closeClicks, 0);
    }

    @Test
    public void waitsForTransientBackdropEvenWhenTransparent() {
        Dom dom = new Dom();
        dom.backdropPolls = 2;
        dom.page.openCart();
        Assert.assertEquals(dom.cartClicks, 1);
        Assert.assertEquals(dom.closeClicks, 0);
        Assert.assertTrue(dom.backdropReads >= 3);
    }

    @Test
    public void closesCampaignOnceAndWaitsForItsBackdropToLeave() {
        Dom dom = new Dom();
        dom.campaignOpen = true;
        dom.page.openCart();
        Assert.assertEquals(dom.closeClicks, 1);
        Assert.assertEquals(dom.cartClicks, 1);
        Assert.assertTrue(dom.backdropReads >= 3);
    }

    @Test
    public void relocatesCampaignControlAfterDomReplacement() {
        Dom dom = new Dom();
        dom.campaignOpen = true;
        dom.staleCloseRead = true;
        dom.page.openCart();
        Assert.assertTrue(dom.closeLookups >= 2);
        Assert.assertEquals(dom.closeClicks, 1);
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void relocatesHeaderLinkAfterDomReplacement() {
        Dom dom = new Dom();
        dom.staleCartRead = true;
        dom.page.openCart();
        Assert.assertEquals(dom.cartLookups, 2);
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void unknownPersistentBackdropTimesOutWithoutClickingBehindIt() {
        Dom dom = new Dom();
        dom.persistentBackdrop = true;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class, dom.page::openCart);
        Assert.assertTrue(failure.getMessage().contains("modal backdrop still present"));
        Assert.assertEquals(dom.cartLookups, 0);
        Assert.assertEquals(dom.cartClicks, 0);
        Assert.assertEquals(dom.closeClicks, 0);
    }

    @Test
    public void unsuccessfulCampaignCloseIsNotRepeated() {
        Dom dom = new Dom();
        dom.campaignOpen = true;
        dom.closeWorks = false;
        Assert.expectThrows(TimeoutException.class, dom.page::openCart);
        Assert.assertEquals(dom.closeClicks, 1);
        Assert.assertEquals(dom.cartClicks, 0);
    }

    @Test
    public void ambiguousCloseClickIsNotRepeated() {
        Dom dom = new Dom();
        dom.campaignOpen = true;
        dom.staleCloseClick = true;
        dom.page.openCart();
        Assert.assertEquals(dom.closeClicks, 1);
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void cartClickFailurePropagatesWithoutRetry() {
        Dom dom = new Dom();
        dom.cartFailure = new ElementClickInterceptedException("Unexpected new overlay");
        Assert.assertSame(Assert.expectThrows(ElementClickInterceptedException.class,
                dom.page::openCart), dom.cartFailure);
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void cartLocatorSupportsSiblingLabelAndHiddenResponsiveDuplicate() throws Exception {
        Dom dom = new Dom();
        dom.hiddenFirstLink = true;
        dom.page.openCart();
        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new org.xml.sax.InputSource(new java.io.StringReader(
                        "<header><div><a href='/sebet'><span>Icon</span></a><span>Səbətim</span></div></header>")));
        String xpath = dom.cartLocator.substring("By.xpath: ".length());
        Assert.assertEquals(((Number) javax.xml.xpath.XPathFactory.newInstance().newXPath().evaluate(
                "count(" + xpath + ")", document, javax.xml.xpath.XPathConstants.NUMBER)).intValue(), 1);
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void waitsForHeaderAndNavigationTransitionWithoutRepeatingClick() {
        Dom dom = new Dom();
        dom.missingLinkPolls = 1;
        dom.navigationPolls = 1;
        dom.page.openCart();
        Assert.assertEquals(dom.cartLookups, 2);
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void missingCartLinkHasDistinctPreclickDiagnostic() {
        Dom dom = new Dom();
        dom.missingLinkPolls = Integer.MAX_VALUE;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class, dom.page::openCart);
        Assert.assertTrue(failure.getMessage().contains("no /sebet link in DOM"));
        Assert.assertEquals(dom.cartClicks, 0);
    }

    @Test
    public void disabledCartLinkHasDistinctPreclickDiagnostic() {
        Dom dom = new Dom();
        dom.linkEnabled = false;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class, dom.page::openCart);
        Assert.assertTrue(failure.getMessage().contains("hidden or disabled"));
        Assert.assertEquals(dom.cartClicks, 0);
    }

    @Test
    public void verifiesActualRouteRatherThanQueryAndDoesNotLeakUrlOrClickAgain() {
        Dom dom = new Dom();
        dom.destinationUrl = "https://example.invalid/other?return=/sebet&token=synthetic-private";
        TimeoutException failure = Assert.expectThrows(TimeoutException.class, dom.page::openCart);
        Assert.assertTrue(failure.getMessage().contains("did not reach the /sebet route"));
        Assert.assertFalse(failure.getMessage().contains("synthetic-private"));
        Assert.assertNull(failure.getCause());
        Assert.assertEquals(dom.cartClicks, 1);
    }

    @Test
    public void searchWaitsForTransparentFadingBackdropBeforeTyping() {
        SearchDom dom = new SearchDom();
        dom.backdropReads = 2;
        dom.home.enterSearchQuery("offline-query");
        Assert.assertEquals(dom.deliveredClicks, 1);
        Assert.assertEquals(dom.value, "offline-query");
        Assert.assertTrue(dom.overlayReads >= 5);
    }

    @Test
    public void searchRecoversOnlyRejectedClickWhenLateBackdropAppears() {
        SearchDom dom = new SearchDom();
        dom.lateBackdrop = true;
        dom.home.enterSearchQuery("offline-query");
        Assert.assertEquals(dom.clickAttempts, 2);
        Assert.assertEquals(dom.deliveredClicks, 1);
        Assert.assertEquals(dom.value, "offline-query");
    }

    @Test
    public void searchPersistentBackdropPreventsClickAndTyping() {
        SearchDom dom = new SearchDom();
        dom.backdropReads = Integer.MAX_VALUE;
        TimeoutException failure = Assert.expectThrows(TimeoutException.class,
                () -> dom.home.enterSearchQuery("offline-query"));
        Assert.assertTrue(failure.getMessage().contains("modal/backdrop clearance"));
        Assert.assertEquals(dom.clickAttempts, 0);
        Assert.assertEquals(dom.value, "");
    }

    @Test
    public void searchHandlesBackdropAlreadyGoneWhenRejectedClickReturns() {
        SearchDom dom = new SearchDom();
        dom.vanishingBackdrop = true;
        dom.home.enterSearchQuery("offline-query");
        Assert.assertEquals(dom.clickAttempts, 2);
        Assert.assertEquals(dom.deliveredClicks, 1);
    }

    @Test
    public void searchRelocatesStaleReadinessAndSubmitAlsoWaitsForBackdrop() {
        SearchDom dom = new SearchDom();
        dom.staleReadiness = true;
        dom.home.enterSearchQuery("offline-query");
        dom.backdropReads = 1;
        dom.home.submitSearch();
        Assert.assertEquals(dom.deliveredClicks, 2);
        Assert.assertEquals(dom.clickAttempts, 2);
    }

    @Test
    public void searchUnexpectedInterceptionFailsWithSanitizedMessage() {
        SearchDom dom = new SearchDom();
        dom.unexpectedOverlay = true;
        ElementClickInterceptedException failure = Assert.expectThrows(ElementClickInterceptedException.class,
                () -> dom.home.enterSearchQuery("offline-query"));
        Assert.assertFalse(failure.getMessage().contains("synthetic-private"));
        Assert.assertNull(failure.getCause());
        Assert.assertEquals(dom.clickAttempts, 1);
        Assert.assertEquals(dom.deliveredClicks, 0);
    }

    private static final class SearchDom {
        int backdropReads, overlayReads, clickAttempts, deliveredClicks;
        boolean lateBackdrop, vanishingBackdrop, staleReadiness, unexpectedOverlay;
        String value = "";
        final WebElement input = Dom.element((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "isEnabled" -> {
                if (staleReadiness) {
                    staleReadiness = false;
                    throw new StaleElementReferenceException("Offline readiness replacement");
                }
                yield true;
            }
            case "click" -> {
                clickAttempts++;
                if (vanishingBackdrop) {
                    vanishingBackdrop = false;
                    throw new ElementClickInterceptedException("Other element: <div class='fade modal-backdrop'>");
                }
                if (lateBackdrop) {
                    lateBackdrop = false;
                    backdropReads = 2;
                    throw new ElementClickInterceptedException("Late campaign backdrop");
                }
                if (unexpectedOverlay) throw new ElementClickInterceptedException("token=synthetic-private");
                Assert.assertTrue(backdropReads <= 0, "Never click through a known backdrop");
                deliveredClicks++;
                yield null;
            }
            case "clear" -> { value = ""; yield null; }
            case "sendKeys" -> { value = ((CharSequence[]) args[0])[0].toString(); yield null; }
            case "getAttribute" -> value;
            default -> throw new AssertionError("Unexpected search element operation: " + method.getName());
        });
        final WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "findElements" -> {
                        Assert.assertTrue(args[0].toString().contains("modal-backdrop"));
                        overlayReads++;
                        yield backdropReads-- > 0 ? List.of(input) : List.of();
                    }
                    case "findElement" -> input;
                    case "toString" -> "Offline search driver";
                    default -> throw new AssertionError("Unexpected search driver operation: " + method.getName());
                });
        final HomePage home = new HomePage(driver, Duration.ofMillis(2200));
    }

    private static final class Dom {
        private boolean campaignOpen;
        private boolean closeWorks = true;
        private boolean persistentBackdrop;
        private boolean staleCloseRead;
        private boolean staleCloseClick;
        private boolean staleCartRead;
        private int backdropPolls;
        private int backdropReads;
        private int closeLookups;
        private int cartLookups;
        private int closeClicks;
        private int cartClicks;
        private ElementClickInterceptedException cartFailure;
        private boolean hiddenFirstLink;
        private boolean linkEnabled = true;
        private int missingLinkPolls;
        private int navigationPolls;
        private String cartLocator;
        private String destinationUrl = "https://example.invalid/sebet";

        private final WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(), new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return "Offline navigation driver";
                    if (method.getName().equals("getCurrentUrl")) {
                        return cartClicks > 0 && navigationPolls-- <= 0 ? destinationUrl : "https://example.invalid/";
                    }
                    String locator = args == null ? "" : args[0].toString();
                    if (method.getName().equals("findElements")) {
                        if (locator.equals("By.cssSelector: button.campaignModalClose")) {
                            closeLookups++;
                            return campaignOpen ? List.of(closeButton()) : List.of();
                        }
                        if (locator.startsWith("By.xpath:")) {
                            Assert.assertTrue(locator.contains("/sebet"));
                            Assert.assertFalse(campaignOpen || persistentBackdrop || backdropPolls >= 0,
                                    "Header must not be resolved before modal/backdrop removal.");
                            cartLocator = locator;
                            cartLookups++;
                            if (missingLinkPolls-- > 0) return List.of();
                            return hiddenFirstLink ? List.of(element((p, m, a) -> {
                                if (m.getName().equals("isDisplayed")) return false;
                                throw new AssertionError("Hidden link must not be clicked.");
                            }), cartLink()) : List.of(cartLink());
                        }
                        Assert.assertEquals(locator, "By.cssSelector: .modal-backdrop");
                        backdropReads++;
                        boolean present = persistentBackdrop || backdropPolls-- > 0;

                        return present ? List.of(element((p, m, a) -> {
                            if (m.getName().equals("isDisplayed")) return false;
                            throw new UnsupportedOperationException(m.getName());
                        })) : List.of();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        private final CartPage page = new CartPage(driver, Duration.ofMillis(1600));

        private WebElement closeButton() {
            return element((proxy, method, args) -> switch (method.getName()) {
                case "isDisplayed" -> {
                    if (staleCloseRead) {
                        staleCloseRead = false;
                        throw new StaleElementReferenceException("Campaign replaced before click");
                    }
                    yield campaignOpen;
                }
                case "isEnabled" -> true;
                case "click" -> {
                    closeClicks++;
                    if (closeWorks) {
                        campaignOpen = false;
                        backdropPolls = 2;
                    }
                    if (staleCloseClick) throw new StaleElementReferenceException("Close outcome ambiguous");
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
        }

        private WebElement cartLink() {
            return element((proxy, method, args) -> switch (method.getName()) {
                case "isDisplayed" -> true;
                case "isEnabled" -> {
                    if (staleCartRead) {
                        staleCartRead = false;
                        throw new StaleElementReferenceException("Header replaced before click");
                    }
                    yield linkEnabled;
                }
                case "click" -> {
                    cartClicks++;
                    if (cartFailure != null) throw cartFailure;
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
        }

        private static WebElement element(InvocationHandler handler) {
            return (WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                    new Class<?>[]{WebElement.class}, handler);
        }
    }
}
