package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.utils.CampaignPopup;
import az.bakuelectronics.automation.utils.PriceUtils;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/** Strict, scoped selling-price reads. Old prices and installment amounts are never fallbacks. */
public final class ProductPriceComponent {
    private final WebDriver driver;

    public ProductPriceComponent(WebDriver driver) { this.driver = driver; }

    public record Quote(String slug, String name, BigDecimal sellingPrice) {
        public Quote {
            if (slug == null || !slug.matches("[a-zA-Z0-9_-]+") || name == null || name.isBlank()
                    || sellingPrice == null || sellingPrice.signum() <= 0) {
                throw new IllegalArgumentException("A quote requires exact identity, name and positive current price");
            }
            name = normalizeName(name);
        }
    }

    public static String normalizeName(String name) {
        return name.replaceAll("[\\s\\p{Zs}]+", " ").trim();
    }

    public static Quote parseQuote(String href, List<String> names, List<String> currentPrices) {
        String path = URI.create(href).getPath();
        if (!path.matches("/mehsul/[a-zA-Z0-9_-]+") || names.size() != 1 || currentPrices.size() != 1) {
            throw new IllegalArgumentException("Expected one product name and one current selling price");
        }
        return new Quote(path.substring(8), names.getFirst(), PriceUtils.parseAzn(currentPrices.getFirst()));
    }

    /** Capture the complete catalog, requiring exactly one visible name/current price per card. */
    public List<Quote> catalogQuotes() {
        return settled(() -> {
            List<?> rows = (List<?>) ((JavascriptExecutor) driver).executeScript("""
                    const visible=e=>!!e.getClientRects().length;
                    const grids=[...document.querySelectorAll('.Products_ProductsInnerBodyGrid__Od4SH')].filter(visible);
                    if(grids.length!==1)throw Error('Expected one visible catalog grid');
                    const text=(r,s)=>[...r.querySelectorAll(s)].filter(visible).map(e=>e.innerText.trim());
                    return [...grids[0].querySelectorAll('a.ProductCard_Product__3nHrM')].filter(visible)
                        .map(c=>[c.getAttribute('href'),text(c,'h4.ProductCard_ProductTitle___frc_'),
                            text(c,'.ProductCard_OriginalPrice___qWZ1')]);
                    """);
            List<Quote> quotes = rows.stream().map(row -> parseRow((List<?>) row)).toList();
            if (quotes.isEmpty() || quotes.stream().map(Quote::slug).distinct().count() != quotes.size()) {
                throw new IllegalArgumentException("Catalog must contain nonempty, unique priced products");
            }
            return quotes;
        });
    }

    public Quote openDetail(Quote captured) {
        driver.get(ConfigReader.getUiBaseUrl() + "/mehsul/" + captured.slug());
        CampaignPopup.dismissIfPresent(driver);
        new ProductPage(driver).getProductName();
        return settled(() -> parseRow((List<?>) ((JavascriptExecutor) driver).executeScript("""
                const visible=e=>!!e.getClientRects().length;
                const roots=[...document.querySelectorAll('main .ProdDetails_ProductDetailsContentRight__qD_hb')].filter(visible);
                if(roots.length!==1)throw Error('Expected one primary product details region');
                const r=roots[0], text=s=>[...r.querySelectorAll(s)].filter(visible).map(e=>e.innerText.trim());
                return [location.href,text('h1'),
                    text('.ProdDetails_TitleProductPrice__nv7Rw .ProdDetails_PriceLeftCurrentPrice__Wmfo2')];
                """)));
    }

    @SuppressWarnings("unchecked")
    private static Quote parseRow(List<?> row) {
        return parseQuote((String) row.get(0), (List<String>) row.get(1), (List<String>) row.get(2));
    }

    /** Observe stable data, never wait for equality with an expected price or identity. */
    private <T> T settled(Supplier<T> read) {
        Object[] last = {null}; long[] since = {0}; int[] polls = {0};
        return new WaitUtils(driver, Duration.ofSeconds(20)).until(active -> {
            T current = read.get();
            if (!current.equals(last[0])) {
                last[0] = current; since[0] = System.nanoTime(); polls[0] = 1; return null;
            }
            return ++polls[0] >= 3 && System.nanoTime() - since[0] >= 1_200_000_000L ? current : null;
        });
    }
}
