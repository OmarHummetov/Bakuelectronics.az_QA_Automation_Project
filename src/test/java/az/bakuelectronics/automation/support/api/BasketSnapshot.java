package az.bakuelectronics.automation.support.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BasketSnapshot {
    private final Map<BasketItemKey, Integer> quantities;

    private BasketSnapshot(Map<BasketItemKey, Integer> quantities) {
        this.quantities = Collections.unmodifiableMap(new LinkedHashMap<>(quantities));
    }

    public static BasketSnapshot from(Map<BasketItemKey, Integer> quantities) {
        return new BasketSnapshot(quantities);
    }

    public static BasketSnapshot fromResponse(io.restassured.response.Response response) {
        if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Basket snapshot read failed: HTTP "
                    + (response == null ? "no response" : response.statusCode()));
        }
        String contentType = response.getHeader("Content-Type");
        if (contentType == null || !contentType.toLowerCase().contains("json")) {
            throw new IllegalStateException("Basket snapshot response is not JSON.");
        }
        Object root = response.jsonPath().get("$");
        if (!(root instanceof Map<?, ?> rootObject)) {
            throw new IllegalStateException("Basket snapshot response root is not an object.");
        }
        Object value = rootObject.get("products");
        if (!(value instanceof Iterable<?> products)) {
            throw new IllegalStateException("Basket snapshot response has no products array.");
        }
        Map<BasketItemKey, Integer> result = new LinkedHashMap<>();
        for (Object product : products) {
            if (!(product instanceof Map<?, ?> item)) {
                throw new IllegalStateException("Basket snapshot contains a non-object product.");
            }
            Object quantity = item.get("quantity");
            if (!(quantity instanceof Number)) {
                throw new IllegalStateException("Basket snapshot product quantity is not numeric.");
            }
            int parsedQuantity = parseQuantity((Number) quantity);
            BasketItemKey key;
            try {
                key = BasketItemKey.from(item);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Basket snapshot product has no stable identifier.", exception);
            }
            if (result.putIfAbsent(key, parsedQuantity) != null) {
                throw new IllegalStateException("Basket snapshot contains duplicate product identity: " + key);
            }
        }
        return from(result);
    }

    private static int parseQuantity(Number quantity) {
        long value;
        if (quantity instanceof Byte || quantity instanceof Short
                || quantity instanceof Integer || quantity instanceof Long) {
            value = quantity.longValue();
        } else {
            double decimal = quantity.doubleValue();
            if (!Double.isFinite(decimal) || decimal != Math.rint(decimal)) {
                throw new IllegalStateException("Basket snapshot product quantity must be an integer.");
            }
            value = quantity.longValue();
            if ((double) value != decimal) {
                throw new IllegalStateException("Basket snapshot product quantity is out of range.");
            }
        }
        if (value < 1 || value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Basket snapshot product quantity must be a positive integer.");
        }
        return (int) value;
    }

    public Map<BasketItemKey, Integer> quantities() {
        return quantities;
    }

    public Integer quantity(BasketItemKey key) {
        Integer exact = quantities.get(key);
        if (exact != null || quantities.containsKey(key)) {
            return exact;
        }
        BasketItemKey match = null;
        for (Map.Entry<BasketItemKey, Integer> entry : quantities.entrySet()) {
            if (key.slug() != null && key.slug().equals(entry.getKey().slug())) {
                if (match != null) {
                    throw new IllegalStateException("Basket contains multiple variants for slug " + key.slug()
                            + "; the current mutation contract cannot identify one safely.");
                }
                match = entry.getKey();
            }
        }
        return match == null ? null : quantities.get(match);
    }
}
