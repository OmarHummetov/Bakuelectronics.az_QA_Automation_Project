package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.pages.CartPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CartHoverNavigationTest {
    @Test public void navigationMovesPointerOutOfHeaderAndWaitsForHoverPanelToDisappear() {
        runNavigation(false);
    }

    @Test public void persistentHoverPanelBlocksSubsequentInteractionWithoutRepeatingNavigation() {
        runNavigation(true);
    }

    private void runNavigation(boolean persistent) {
        AtomicInteger clicks = new AtomicInteger(), moves = new AtomicInteger(), hoverReads = new AtomicInteger();
        WebElement link = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isDisplayed", "isEnabled" -> true;
                    case "click" -> { clicks.incrementAndGet(); yield null; }
                    default -> throw new AssertionError("Unexpected link operation");
                });
        WebElement panel = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    Assert.assertEquals(method.getName(), "isDisplayed");
                    return persistent || hoverReads.get() < 2;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, Interactive.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getCurrentUrl")) {
                        Assert.assertEquals(clicks.get(), 1);
                        return "https://example.invalid/sebet";
                    }
                    if (method.getName().equals("perform")) {
                        Assert.assertEquals(clicks.get(), 1);
                        moves.incrementAndGet();
                        var sequences = (java.util.Collection<?>) args[0];
                        Assert.assertEquals(sequences.size(), 1);
                        var sequence = ((Sequence) sequences.iterator().next()).encode();
                        var actions = (List<?>) sequence.get("actions");
                        Map<?, ?> move = (Map<?, ?>) actions.get(actions.size() - 1);
                        Assert.assertEquals(move.get("type"), "pointerMove");
                        Assert.assertEquals(move.get("origin"), "viewport");
                        Assert.assertEquals(move.get("x"), 1);
                        Assert.assertEquals(move.get("y"), 1);
                        return null;
                    }
                    String locator = args[0].toString();
                    if (method.getName().equals("findElements"))
                        return locator.startsWith("By.xpath:") ? List.of(link) : List.of();
                    if (method.getName().equals("findElement") && locator.contains("header_basketHover__btn__")) {
                        Assert.assertEquals(moves.get(), 1);
                        hoverReads.incrementAndGet();
                        return panel;
                    }
                    throw new AssertionError("Unexpected browser operation");
                });
        var cart = new CartPage(driver, Duration.ofMillis(1200));
        if (persistent) Assert.expectThrows(org.openqa.selenium.TimeoutException.class, cart::openCart);
        else cart.openCart();
        Assert.assertEquals(clicks.get(), 1);
        Assert.assertEquals(moves.get(), 1);
        Assert.assertTrue(hoverReads.get() >= 2, "Hover disappearance must be observed, not assumed from pointer dispatch");
    }
}
