package az.bakuelectronics.automation.unit.api;

import az.bakuelectronics.automation.support.api.BasketItemKey;
import az.bakuelectronics.automation.support.api.BasketSnapshot;

import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BasketSnapshotContractTest {

    @Test(groups = {"unit", "read-only"})
    public void parsesEmptyBasket() {
        BasketSnapshot snapshot = parse(List.of());
        Assert.assertTrue(snapshot.quantities().isEmpty());
    }

    @Test(groups = {"unit", "read-only"})
    public void parsesOneProductAndQuantityGreaterThanOne() {
        BasketSnapshot snapshot = parse(List.of(product("phone-a", 3)));
        BasketItemKey key = new BasketItemKey(null, "phone-a", null, null);
        Assert.assertEquals(snapshot.quantity(key), Integer.valueOf(3));
    }

    @Test(groups = {"unit", "read-only"})
    public void parsesSeveralProducts() {
        BasketSnapshot snapshot = parse(List.of(product("phone-a", 1), product("phone-b", 2)));
        Assert.assertEquals(snapshot.quantities().size(), 2);
    }

    @Test(groups = {"unit", "read-only"})
    public void keepsVariantsWithTheSameSlugSeparate() {
        BasketSnapshot snapshot = parse(List.of(
                productWithVariant("phone-a", 10L, 1),
                productWithVariant("phone-a", 11L, 2)));

        Assert.assertEquals(snapshot.quantities().get(new BasketItemKey(null, "phone-a", null, 10L)),
                Integer.valueOf(1));
        Assert.assertEquals(snapshot.quantities().get(new BasketItemKey(null, "phone-a", null, 11L)),
                Integer.valueOf(2));
    }

    @Test(groups = {"unit", "read-only"})
    public void rejectsProductWithoutAnyStableIdentifier() {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("quantity", 1);
        assertParseFails(List.of(product), "stable identifier");
    }

    @Test(groups = {"unit", "read-only"})
    public void rejectsWrongQuantityFormat() {
        assertParseFails(List.of(product("phone-a", "2")), "not numeric");
        assertParseFails(List.of(product("phone-a", 1.5)), "must be an integer");
    }

    @Test(groups = {"unit", "read-only"})
    public void rejectsDuplicateIdentityInsteadOfSilentlyOverwriting() {
        assertParseFails(List.of(product("phone-a", 1), product("phone-a", 2)),
                "duplicate product identity");
    }

    private static Map<String, Object> product(String slug, Object quantity) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("slug", slug);
        product.put("quantity", quantity);
        return product;
    }

    private static Map<String, Object> productWithVariant(String slug, Long variantId, int quantity) {
        Map<String, Object> product = product(slug, quantity);
        product.put("variant_id", variantId);
        return product;
    }

    private static BasketSnapshot parse(List<Map<String, Object>> products) {
        return BasketSnapshot.fromResponse(response(Map.of("products", products)));
    }

    private static void assertParseFails(List<Map<String, Object>> products, String message) {
        RuntimeException exception = Assert.expectThrows(RuntimeException.class,
                () -> parse(products));
        Assert.assertTrue(exception.getMessage().contains(message), exception.getMessage());
    }

    private static Response response(Map<String, Object> body) {
        return new ResponseBuilder()
                .setStatusCode(200)
                .setContentType("application/json")
                .setBody(jsonObject(body))
                .build();
    }

    private static String jsonObject(Map<String, Object> object) {
        return "{" + object.entrySet().stream()
                .map(entry -> jsonString(entry.getKey()) + ":" + jsonValue(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(",")) + "}";
    }

    private static String jsonValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, item) -> normalized.put(String.valueOf(key), item));
            return jsonObject(normalized);
        }
        if (value instanceof List<?> list) {
            return "[" + list.stream().map(BasketSnapshotContractTest::jsonValue)
                    .collect(java.util.stream.Collectors.joining(",")) + "]";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return jsonString(String.valueOf(value));
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
