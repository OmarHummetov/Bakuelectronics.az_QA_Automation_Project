package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.PriceUtils;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Atomic reads of the audited results grid, excluding recommendations and recent products. */
public final class ProductGrid {
    private static final String READ = """
            const grid = document.querySelector('.Products_ProductsInnerBodyGrid__Od4SH');
            if (!grid) return null;
            const cards = [...grid.querySelectorAll('a.ProductCard_Product__3nHrM')];
            if (!cards.length) return null;
            return [location.href, cards.map(card => [card.getAttribute('href'),
                card.querySelector('h4.ProductCard_ProductTitle___frc_')?.textContent.trim() || '',
                card.querySelector('.ProductCard_OriginalPrice___qWZ1')?.textContent.trim() || ''])];
            """;
    private final WebDriver driver;
    private String lastRead = "Grid not read yet";

    public ProductGrid(WebDriver driver) { this.driver = driver; }

    /** Out-of-stock search cards can omit a selling price; retain their identity for search parity. */
    public record Product(String slug, String title, BigDecimal sellingPrice) { }

    public record Snapshot(String url, List<Product> products) {
        public Snapshot {
            products = List.copyOf(products);
            if (products.isEmpty()) throw new IllegalArgumentException("Results grid must be nonempty");
            Set<String> identities = new LinkedHashSet<>();
            for (Product product : products) {
                if (product.slug() == null || product.slug().isBlank() || product.title() == null
                        || product.title().isBlank()
                        || (product.sellingPrice() != null && product.sellingPrice().signum() <= 0)
                        || !identities.add(product.slug())) {
                    throw new IllegalArgumentException("Incomplete or duplicate product card: " + product);
                }
            }
        }

        public Set<String> slugs() {
            Set<String> slugs = new LinkedHashSet<>();
            products.forEach(product -> slugs.add(product.slug()));
            return Set.copyOf(slugs);
        }

        public String parameter(String name) { return queryParameter(url, name); }
    }

    public static String queryParameter(String url, String name) {
        String query = URI.create(url).getRawQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(name)) {
                return parts.length == 1 ? "" : URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** Three or more matching polls spanning 1.2s; URL and ordered card data must both settle. */
    public Snapshot settled() { return settledAfter(null); }

    public Snapshot settledAfter(Snapshot previous) {
        Snapshot[] last = {null};
        long[] unchangedSince = {0};
        int[] polls = {0};
        try {
            return new WaitUtils(driver, Duration.ofSeconds(20)).until(active -> {
                Snapshot current = read();
                if (current == null || (previous != null && current.products().equals(previous.products()))) {
                    last[0] = null;
                    polls[0] = 0;
                    return null;
                }
                if (!Objects.equals(current, last[0])) {
                    last[0] = current;
                    unchangedSince[0] = System.nanoTime();
                    polls[0] = 1;
                    return null;
                }
                polls[0]++;
                return polls[0] >= 3 && System.nanoTime() - unchangedSince[0] >= 1_200_000_000L
                        ? current : null;
            });
        } catch (TimeoutException failure) {
            throw new TimeoutException("Results grid did not settle after the requested transition. Last snapshot="
                    + last[0] + "; last DOM read=" + lastRead, failure);
        }
    }

    @SuppressWarnings("unchecked")
    private Snapshot read() {
        List<?> raw = (List<?>) ((JavascriptExecutor) driver).executeScript(READ);
        lastRead = String.valueOf(raw);
        if (raw == null) return null;
        List<List<String>> rows = (List<List<String>>) raw.get(1);
        if (rows.stream().anyMatch(row -> row.get(0) == null || row.get(0).isBlank()
                || row.get(1) == null || row.get(1).isBlank())) {
            return null;
        }
        return parseSnapshot((String) raw.get(0), rows);
    }

    public static Snapshot parseSnapshot(String url, List<List<String>> rows) {
        return new Snapshot(url, rows.stream().map(row -> {
            String path = URI.create(row.get(0)).getPath();
            if (!path.startsWith("/mehsul/") || path.substring(8).contains("/")) {
                throw new IllegalArgumentException("Unexpected product href: " + row.get(0));
            }
            String price = row.get(2);
            return new Product(path.substring(8), row.get(1),
                    price == null || price.isBlank() ? null : PriceUtils.parseAzn(price));
        }).toList());
    }
}
