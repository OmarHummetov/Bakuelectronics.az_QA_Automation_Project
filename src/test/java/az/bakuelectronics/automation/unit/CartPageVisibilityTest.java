package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class CartPageVisibilityTest {
    private static final String SLUG = "api-added-product";

    @Test
    public void doesNotUseElementAfterSuccessfulVisibilityWait() {
        Dom dom = new Dom();
        Assert.assertTrue(dom.page.isProductVisible(SLUG));
        Assert.assertEquals(dom.visibilityReads, 1,
                "A second read on the returned element reproduces the original stale failure.");
        dom.assertScoped();
    }

    @Test
    public void relocatesLinkWhenReactReplacesItDuringVisibilityCheck() {
        Dom dom = new Dom();
        dom.staleLink = true;
        Assert.assertTrue(dom.page.isProductVisible(SLUG));
        Assert.assertTrue(dom.lookups.size() >= 2);
        dom.assertScoped();
    }

    @Test
    public void relocatesRowAndQuantityAndWaitsForExpectedValue() {
        Dom dom = new Dom();
        dom.staleQuantity = true;
        dom.delayedQuantity = true;
        Assert.assertEquals(dom.page.waitForProductQuantity(SLUG, 1), 1);
        Assert.assertTrue(dom.lookups.size() >= 3);
        dom.assertScoped();
    }

    @Test
    public void permanentlyMissingSelectedProductTimesOut() {
        Dom dom = new Dom();
        dom.absent = true;
        Assert.expectThrows(TimeoutException.class, () -> dom.page.isProductVisible(SLUG));
        dom.assertScoped();
    }

    @Test
    public void permanentlyStaleLinkTimesOut() {
        Dom dom = new Dom();
        dom.alwaysStale = true;
        Assert.expectThrows(TimeoutException.class, () -> dom.page.isProductVisible(SLUG));
    }

    @Test
    public void hiddenProductTimesOut() {
        Dom dom = new Dom();
        dom.visible = false;
        Assert.expectThrows(TimeoutException.class, () -> dom.page.isProductVisible(SLUG));
    }

    @Test
    public void wrongQuantityTimesOutInsteadOfPassingOnAnyRenderedValue() {
        Dom dom = new Dom();
        dom.quantity = "2";
        Assert.expectThrows(TimeoutException.class, () -> dom.page.waitForProductQuantity(SLUG, 1));
        dom.assertScoped();
    }

    @Test
    public void malformedQuantityFailsWithoutBeingRetriedAsStale() {
        Dom dom = new Dom();
        dom.quantity = "invalid";
        Assert.expectThrows(NumberFormatException.class, () -> dom.page.waitForProductQuantity(SLUG, 1));
        Assert.assertEquals(dom.lookups.size(), 1);
    }

    @Test
    public void realDriverFailurePropagatesImmediately() {
        Dom dom = new Dom();
        dom.failure = new WebDriverException("browser connection lost");
        Assert.assertSame(Assert.expectThrows(WebDriverException.class,
                () -> dom.page.isProductVisible(SLUG)), dom.failure);
        Assert.assertEquals(dom.lookups.size(), 1);
    }

    @Test
    public void rejectsInvalidProductSlugBeforeLookingUpAnything() {
        Dom dom = new Dom();
        Assert.expectThrows(IllegalArgumentException.class, () -> dom.page.isProductVisible("' or 1=1"));
        Assert.expectThrows(IllegalArgumentException.class, () -> dom.page.waitForProductQuantity("", 1));
        Assert.assertTrue(dom.lookups.isEmpty());
    }

    private static final class Dom {
        private final List<String> lookups = new ArrayList<>();
        private int visibilityReads;
        private int quantityReads;
        private boolean staleLink;
        private boolean staleQuantity;
        private boolean delayedQuantity;
        private boolean alwaysStale;
        private boolean absent;
        private boolean visible = true;
        private String quantity = "1";
        private WebDriverException failure;
        private final CartPage page = new CartPage((WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(), new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return "Offline cart driver";
                    if (!method.getName().equals("findElement"))
                        throw new UnsupportedOperationException(method.getName());
                    String locator = args[0].toString();
                    lookups.add(locator);
                    if (failure != null) throw failure;

                    if (absent && locator.contains(SLUG)) throw new NoSuchElementException("Selected product absent");
                    return locator.contains("ancestor::div") ? row() : link();
                }), Duration.ofMillis(1100));

        private WebElement link() {
            int[] reads = {0};
            return element((proxy, method, args) -> {
                if (!method.getName().equals("isDisplayed"))
                    throw new UnsupportedOperationException(method.getName());
                visibilityReads++;
                if (++reads[0] > 1 || alwaysStale || staleLink) {
                    staleLink = false;
                    throw stale();
                }
                return visible;
            });
        }

        private WebElement row() {
            return element((proxy, method, args) -> switch (method.getName()) {
                case "isDisplayed" -> visible;
                case "findElement" -> {
                    Assert.assertTrue(args[0].toString().contains("following-sibling"));
                    yield element((p, m, a) -> switch (m.getName()) {
                        case "isDisplayed" -> visible;
                        case "getAttribute" -> {
                            if (staleQuantity) {
                                staleQuantity = false;
                                throw stale();
                            }
                            yield delayedQuantity && quantityReads++ == 0 ? "2" : quantity;
                        }
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
        }

        private void assertScoped() {
            Assert.assertFalse(lookups.isEmpty());
            Assert.assertTrue(lookups.stream().allMatch(locator -> locator.contains("/mehsul/" + SLUG)),
                    "Each poll must use the exact selected product route, not the first basket item.");
        }

        private static StaleElementReferenceException stale() {
            return new StaleElementReferenceException("React replaced this DOM node");
        }

        private static WebElement element(InvocationHandler handler) {
            return (WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                    new Class<?>[]{WebElement.class}, handler);
        }
    }
}
