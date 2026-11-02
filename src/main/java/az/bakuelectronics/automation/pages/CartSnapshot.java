package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.utils.PriceUtils;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Immutable displayed unit prices and quantities; line totals are explicitly derived. */
public record CartSnapshot(Map<String, Row> rows, BigDecimal discount, BigDecimal total) {
    public CartSnapshot { rows = Map.copyOf(rows); }

    public record Row(String slug, String name, BigDecimal originalUnitPrice,
                      BigDecimal discountedUnitPrice, int quantity) {
        public BigDecimal calculatedTotal() {
            return discountedUnitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        public BigDecimal calculatedDiscount() {
            return originalUnitPrice.subtract(discountedUnitPrice).multiply(BigDecimal.valueOf(quantity));
        }
    }

    public static CartSnapshot parse(List<?> raw) {
        if (raw == null || raw.size() != 3 || !(raw.get(0) instanceof List<?> items) || items.isEmpty())
            throw new IllegalArgumentException("Expected nonempty cart rows, discount and total");
        if (!(raw.get(1) instanceof String) || !(raw.get(2) instanceof String))
            throw new IllegalArgumentException("Expected displayed discount and total text");
        Map<String, Row> rows = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();
        for (Object item : items) {
            if (!(item instanceof List<?> fields) || fields.size() != 6
                    || fields.stream().anyMatch(field -> !(field instanceof String)))
                throw new IllegalArgumentException("Incomplete cart row");
            String slug = (String) fields.get(0);
            String name = normalize((String) fields.get(1));
            String renderedCount = (String) fields.get(4);
            if (!slug.matches("[a-zA-Z0-9_-]+") || name.isBlank()
                    || !names.add(name)
                    || !renderedCount.matches("\\s*[0-9]+\\s*(?:ədəd)?\\s*"))
                throw new IllegalArgumentException("Malformed row identity/count");
            int count = Integer.parseInt(renderedCount.replace("ədəd", "").trim());
            int control = Integer.parseInt(((String) fields.get(5)).trim());
            Row row = new Row(slug, name, PriceUtils.parseAzn((String) fields.get(2)),
                    PriceUtils.parseAzn((String) fields.get(3)), count);
            if (count < 1 || control != count || row.discountedUnitPrice().signum() <= 0
                    || row.originalUnitPrice().compareTo(row.discountedUnitPrice()) < 0
                    || rows.putIfAbsent(slug, row) != null)
                throw new IllegalArgumentException("Duplicate row, invalid prices or inconsistent quantities");
        }
        return new CartSnapshot(rows, PriceUtils.parseAzn((String) raw.get(1)),
                PriceUtils.parseAzn((String) raw.get(2)));
    }

    public boolean hasExpectedArithmetic() {
        return !rows.isEmpty() && total.compareTo(rows.values().stream().map(Row::calculatedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)) == 0
                && discount.abs().compareTo(rows.values().stream().map(Row::calculatedDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)) == 0;
    }

    public static String normalize(String value) {
        return value.replaceAll("[\\s\\p{Zs}]+", " ").trim();
    }
}
