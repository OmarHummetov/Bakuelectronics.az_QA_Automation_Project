package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.models.ProductApiModel;
import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ProductApiService {

    private static final String DEFAULT_SEARCH_RESULT_ENDPOINT = "/default-search-result";

    private final ApiClient apiClient;

    public ProductApiService() {
        this(new ApiClient());
    }

    public ProductApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<ProductApiModel> getDefaultSearchProducts() {
        Response response = apiClient.get(DEFAULT_SEARCH_RESULT_ENDPOINT);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Default search API returned HTTP " + response.statusCode());
        }

        Object responseBody = response.jsonPath().get("$");
        return Collections.unmodifiableList(collectProductCandidates(responseBody).stream()
                .map(ProductCandidate::product)
                .toList());
    }

    public ProductApiModel getFirstDefaultSearchProduct() {
        return getDefaultSearchProducts().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Default search API response contains no products."));
    }


    public ProductApiModel getFirstDefaultSearchProductWithCompleteData() {
        Response response = apiClient.get(DEFAULT_SEARCH_RESULT_ENDPOINT);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Default search API returned HTTP " + response.statusCode());
        }

        ProductCandidate selected = collectProductCandidates(response.jsonPath().get("$"))
                .stream()
                .filter(candidate -> hasCompleteIntegrationData(candidate.product()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Default search API response contains no product with complete integration data."));
        printSelectedProductDiagnostics(selected);
        return selected.product();
    }

    private static boolean hasCompleteIntegrationData(ProductApiModel product) {
        return !isBlank(product.getSlug())
                && !isBlank(product.getName())
                && product.getPrice() != null;
    }

    private static void printSelectedProductDiagnostics(ProductCandidate selected) {
        ProductApiModel product = selected.product();
        System.out.println("API product name: " + product.getName());
        System.out.println("API slug: " + product.getSlug());
        System.out.println("API price: " + product.getPrice());

        List<String> priceFields = new ArrayList<>();
        collectPriceLikeFields(selected.rawProduct(), "", priceFields);
        priceFields.sort(Comparator.naturalOrder());
        System.out.println("API price-like fields: "
                + (priceFields.isEmpty() ? "<none>" : String.join(", ", priceFields)));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<ProductCandidate> collectProductCandidates(Object value) {
        List<ProductCandidate> candidates = new ArrayList<>();
        collectProducts(value, candidates);
        return candidates;
    }

    private static void collectProducts(Object value, List<ProductCandidate> products) {
        if (value instanceof Map<?, ?> map) {
            if (isProductObject(map)) {
                products.add(new ProductCandidate(toProduct(map), map));
                return;
            }
            for (Object child : map.values()) {
                collectProducts(child, products);
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object child : iterable) {
                collectProducts(child, products);
            }
        }
    }

    private static boolean isProductObject(Map<?, ?> map) {
        return map.containsKey("slug")
                && (map.containsKey("name") || map.containsKey("title"))
                && map.containsKey("price");
    }

    private static ProductApiModel toProduct(Map<?, ?> product) {
        return new ProductApiModel(
                toLong(product.get("id")),
                firstString(product, "name", "title"),
                firstString(product, "product_code", "productCode"),
                stringValue(product.get("slug")),
                toBigDecimal(product.get("price")),
                toBigDecimal(product.get("discount")));
    }

    private static String firstString(Map<?, ?> map, String firstKey, String secondKey) {
        Object firstValue = map.get(firstKey);
        return firstValue != null ? stringValue(firstValue) : stringValue(map.get(secondKey));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static void collectPriceLikeFields(Object value, String path, List<String> fields) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    continue;
                }
                String childPath = path.isEmpty() ? key : path + "." + key;
                Object child = entry.getValue();
                if (isPriceLikeKey(key)) {
                    collectPriceLikeValue(child, childPath, fields);
                }
            }
        }
    }

    private static void collectPriceLikeValue(Object value, String path, List<String> fields) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!isSensitiveKey(key)) {
                    collectPriceLikeValue(entry.getValue(), path + "." + key, fields);
                }
            }
        } else if (value instanceof Iterable<?> iterable) {
            int index = 0;
            for (Object child : iterable) {
                collectPriceLikeValue(child, path + "[" + index++ + "]", fields);
            }
        } else {
            fields.add(path + "=" + String.valueOf(value));
        }
    }

    private static boolean isPriceLikeKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("price")
                || normalized.contains("discount")
                || normalized.contains("online")
                || normalized.contains("campaign")
                || normalized.contains("promo")
                || normalized.contains("sale")
                || normalized.contains("regular")
                || normalized.contains("current")
                || normalized.contains("original")
                || normalized.contains("previous")
                || normalized.contains("final");
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("cookie")
                || normalized.contains("authorization")
                || normalized.contains("credential")
                || normalized.contains("secret");
    }

    private record ProductCandidate(ProductApiModel product, Map<?, ?> rawProduct) {
    }
}
