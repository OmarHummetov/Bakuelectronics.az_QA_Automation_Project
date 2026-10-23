package az.bakuelectronics.automation.support.api;

import java.util.Map;
import java.util.Objects;


public record BasketItemKey(Long productId, String slug, String sku, Long variantId) {

    public BasketItemKey {
        if (productId == null && isBlank(slug) && isBlank(sku) && variantId == null) {
            throw new IllegalArgumentException("Basket item has no stable identifier.");
        }
    }

    public static BasketItemKey from(Map<?, ?> item) {
        return new BasketItemKey(
                number(item, "id", "product_id", "productId"),
                text(item, "slug"),
                text(item, "sku", "product_code", "productCode"),
                number(item, "variant_id", "variantId"));
    }

    public boolean matches(Map<?, ?> item) {
        return equals(from(item));
    }

    public String mutationSlug() {
        if (isBlank(slug)) {
            throw new IllegalStateException("Basket mutation contract requires a slug for " + this);
        }
        return slug;
    }

    private static Long number(Map<?, ?> item, String... names) {
        for (String name : names) {
            Object value = item.get(name);
            if (value != null) {
                return Long.valueOf(String.valueOf(value));
            }
        }
        return null;
    }

    private static String text(Map<?, ?> item, String... names) {
        for (String name : names) {
            Object value = item.get(name);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
