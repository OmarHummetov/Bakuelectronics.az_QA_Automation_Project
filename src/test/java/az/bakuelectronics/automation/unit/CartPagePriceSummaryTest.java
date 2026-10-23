package az.bakuelectronics.automation.unit;

import az.bakuelectronics.automation.pages.CartPage;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class CartPagePriceSummaryTest {
    private static final List<String> ONE = List.of("25,99 ₼", "20,99 ₼", "1", "- 5 ₼", "20,99 ₼", "1");
    private static final List<String> TWO = List.of("25.99 AZN", "20.99 AZN", "2 ədəd", "- 10 ₼", "41.98 ₼", "2");

    @Test
    public void readsBothQuantitiesAndWaitsThroughMixedUpdateAndReactReplacement() {
        AtomicInteger polls = new AtomicInteger();
        CartPage page = page((script, arguments) -> {
            int poll = polls.incrementAndGet();
            assertLocatorContract(script, arguments);
            return switch (poll) {
                case 1, 2 -> ONE;
                case 3 -> throw new StaleElementReferenceException("Replaced DOM");
                case 4 -> ONE;
                case 5 -> List.of("25.99 ₼", "20.99 ₼", "2", "- 5 ₼", "20.99 ₼", "2");
                case 6 -> List.of("25.99 ₼", "20.99 ₼", "2", "- 10 ₼", "41.98 ₼", "1");
                default -> TWO;
            };
        });
        CartPage.PriceSummary one = page.waitForPriceSummary("synthetic-product", "Synthetic product", 1);
        CartPage.PriceSummary two = page.waitForPriceSummary("synthetic-product", "Synthetic product", 2);
        Assert.assertTrue(one.hasExpectedArithmetic(1));
        Assert.assertTrue(two.hasExpectedArithmetic(2));
        Assert.assertEquals(two.originalUnitPrice().compareTo(one.originalUnitPrice()), 0);
        Assert.assertEquals(two.discountedUnitPrice().compareTo(one.discountedUnitPrice()), 0);
        Assert.assertEquals(two.discount(), new BigDecimal("-10.00"));
        Assert.assertEquals(polls.get(), 8, "Require two equal, arithmetically consistent post-update snapshots.");
    }

    @Test
    public void waitsForMissingSummaryDuringRendering() {
        AtomicInteger polls = new AtomicInteger();
        CartPage page = page((script, arguments) -> {
            if (polls.incrementAndGet() == 1) {
                throw new JavascriptException("Expected exactly one sebet_BasketRight__; found 0");
            }
            return ONE;
        });
        Assert.assertEquals(page.waitForPriceSummary("synthetic-product", "Synthetic product", 1).quantity(), 1);
        Assert.assertEquals(polls.get(), 3);
    }

    @Test
    public void timesOutWithActualAmountsWhenUnexpectedTotalNeverSettles() {
        List<String> extraFee = List.of("25.99 ₼", "20.99 ₼", "2", "- 10 ₼", "42.98 ₼", "2");
        CartPage page = page((script, arguments) -> extraFee);
        TimeoutException error = Assert.expectThrows(TimeoutException.class,
                () -> page.waitForPriceSummary("synthetic-product", "Synthetic product", 2));
        Assert.assertTrue(error.getMessage().contains("Unexpected charges"));
        Assert.assertTrue(error.getMessage().contains("grandTotal=42.98"));
    }

    @DataProvider
    public Object[][] unsupportedStates() {
        return new Object[][] {
                {"Expected exactly one summary product row; found 2"},
                {"Unexpected additional cart product quantity-control row"},
                {"Expected exactly one selected quantity-control row; found 2"},
                {"Summary product name does not match the selected product"},
                {"Unexpected summary charge/amount outside the verified price fields: Delivery 0 ₼"}
        };
    }

    @Test(dataProvider = "unsupportedStates")
    public void rejectsExtraItemsMismatchedProductAndAdditionalAmounts(String diagnostic) {
        CartPage page = page((script, arguments) -> { throw new JavascriptException(diagnostic); });
        IllegalStateException error = Assert.expectThrows(IllegalStateException.class,
                () -> page.waitForPriceSummary("synthetic-product", "Synthetic product", 1));
        Assert.assertTrue(error.getMessage().contains(diagnostic));
    }

    @DataProvider
    public Object[][] inconsistentAmounts() {
        return new Object[][] {
                {2, "25.99", "20.99", "-10", "20.99"},
                {2, "25.99", "20.99", "-5", "41.98"},
                {2, "25.99", "20.99", "-10", "42.98"},
                {2, "25.99", "20.99", "-10", "31.98"},
                {1, "25.99", "20.99", "-10", "41.98"},
                {2, "19.99", "20.99", "-2", "41.98"},
                {2, "0", "0", "0", "0"}
        };
    }

    @Test(dataProvider = "inconsistentAmounts")
    public void refusesInconsistentSnapshot(int quantity, String original, String current,
                                          String discount, String total) {
        CartPage.PriceSummary summary = new CartPage.PriceSummary(new BigDecimal(original),
                new BigDecimal(current), quantity, new BigDecimal(discount), new BigDecimal(total));
        Assert.assertFalse(summary.hasExpectedArithmetic(2));
    }

    @DataProvider
    public Object[][] cartRows() {
        String selected = cartRow("selected", "synthetic-product");
        String duplicateLinks = selected.replace("<div class='controls'>",
                "<a href='/mehsul/synthetic-product/'>Duplicate image link</a><div class='controls'>");
        String nestedLinks = "<div><a href='/mehsul/synthetic-product'>Image</a>" + selected + "</div>";
        return new Object[][] {
                {selected, 1, 1, false},
                {duplicateLinks, 1, 1, false},
                {nestedLinks, 1, 1, true},
                {"<a href='/mehsul/unrelated-recommendation'>Recommendation</a>" + selected, 1, 1, false},
                {selected + cartRow("other", "different-product"), 2, 1, false},
                {selected + cartRow("duplicate", "synthetic-product"), 2, 2, false},
                {cartRow("other", "synthetic-product-accessory"), 1, 0, false}
        };
    }

    @Test(dataProvider = "cartRows")
    public void identifiesActualControlRowsInsteadOfCountingProductLinkAncestors(
            String markup, int rowCount, int selectedCount, boolean reproducesOldFailure) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(
                new StringReader("<html><body><main><div>" + markup
                        + "<div class='sebet_BasketRight__fixture'><a href='/mehsul/synthetic-product'>Summary</a></div>"
                        + "</div></main></body></html>")));
        AtomicInteger polls = new AtomicInteger();
        CartPage page = page((script, arguments) -> {
            polls.incrementAndGet();
            NodeList rows = evaluate(document, arguments[1].toString());
            NodeList selected = evaluate(document, arguments[2].toString());
            Assert.assertEquals(rows.getLength(), rowCount);
            Assert.assertEquals(selected.getLength(), selectedCount);
            if (reproducesOldFailure) {
                String oldRows = "//main//a[contains(@href, '/mehsul/') and not(ancestor::*["
                        + "contains(concat(' ', normalize-space(@class)), ' sebet_BasketRight__')])]"
                        + "/ancestor::div[.//button[normalize-space() = '-']"
                        + " and .//button[normalize-space() = '+']][1]";
                Assert.assertEquals(evaluate(document, oldRows).getLength(), 2,
                        "One product's image/name links previously produced two nested containers.");
            }
            if (selectedCount != 1) {
                throw new JavascriptException("Expected exactly one selected quantity-control row; found " + selectedCount);
            }
            Assert.assertEquals(selected.item(0).getAttributes().getNamedItem("id").getNodeValue(), "selected");
            if (rowCount > 1) {
                Assert.assertFalse(rows.item(1).isSameNode(selected.item(0)), "The other row is a separate product.");
                throw new JavascriptException("Unexpected additional cart product quantity-control row");
            }
            Assert.assertTrue(rows.item(0).isSameNode(selected.item(0)));
            return ONE;
        });
        if (selectedCount == 0) {
            Assert.expectThrows(TimeoutException.class,
                    () -> page.waitForPriceSummary("synthetic-product", "Synthetic product", 1));
        } else if (selectedCount != 1 || rowCount > 1) {
            Assert.expectThrows(IllegalStateException.class,
                    () -> page.waitForPriceSummary("synthetic-product", "Synthetic product", 1));
        } else {
            Assert.assertTrue(page.waitForPriceSummary("synthetic-product", "Synthetic product", 1)
                    .hasExpectedArithmetic(1));
            Assert.assertEquals(polls.get(), 2);
        }
    }

    private static String cartRow(String id, String slug) {
        return "<div id='" + id + "'><a href='/mehsul/" + slug + "'>Product name</a>"
                + "<div class='controls'><button>-</button><span>1</span><button>+</button></div></div>";
    }

    private static NodeList evaluate(Document document, String xpath) {
        try {
            return (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpath, document, XPathConstants.NODESET);
        } catch (javax.xml.xpath.XPathExpressionException e) {
            throw new AssertionError("Invalid cart row XPath: " + xpath, e);
        }
    }

    private void assertLocatorContract(String script, Object[] arguments) {
        for (String prefix : List.of("sebet_BasketRight__", "sebet_BasketRightBodyItem__", "sebet_ItemName__",
                "sebet_BasketRightBodyItemPriceOld__", "sebet_BasketRightBodyItemPriceCurrent__",
                "sebet_ItemCount__", "sebet_BasketRightDiscountPrice__", "sebet_TotalTopPrice__")) {
            Assert.assertTrue(script.contains("'" + prefix + "'"));
        }
        Assert.assertEquals(arguments[0], "Synthetic product");
        Assert.assertTrue(arguments[2].toString().contains("synthetic-product"));
        Assert.assertTrue(script.contains("cloneNode(true)"), "Check residual amounts outside verified fields.");
    }

    @FunctionalInterface
    private interface SnapshotReader {
        List<String> read(String script, Object[] arguments);
    }

    private CartPage page(SnapshotReader reader) {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeScript")) {
                        return reader.read((String) args[0], (Object[]) args[1]);
                    }
                    if (method.getName().equals("toString")) return "Offline cart snapshot driver";
                    throw new AssertionError("Snapshot reading must not click or use retained elements: " + method.getName());
                });
        return new CartPage(driver);
    }
}
