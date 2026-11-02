package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.support.api.ApiTestReporter;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FastSearchExpandedContractTest {

    private static final String QUERY = "samsung";
    private final ApiClient apiClient = ApiClient.publicClient();

    @Test(groups = {"api", "public-api", "read-only"})
    public void shouldReturnRelevantProductsWithValidSellingPrices() {
        ApiTestReporter.run("Fast Search Expanded Contract", () -> {
            Response response = apiClient.get("/fast-search-expanded", Map.of("query", QUERY));

            ApiTestReporter.verifyStatus(response, 200);
            ApiTestReporter.verifyJsonResponse(response);
            ApiTestReporter.verifyJsonRoot(response);
            // Priced products are in data; most_searched contains unpriced suggestions.
            for (String collection : List.of("data", "most_searched", "related_keywords", "related_categories")) {
                ApiTestReporter.verifyFieldType(response, collection, List.class);
            }
            List<?> products = response.jsonPath().getList("data");
            ApiTestReporter.verifyCondition("Data Structure", "Search returns priced products",
                    "nonempty data array", products.size(), !products.isEmpty(),
                    "Expanded search must return products for the verified Samsung query.");

            Set<String> slugs = new HashSet<>();
            for (int index = 0; index < products.size(); index++) {
                String path = "data[" + index + "]";
                ApiTestReporter.verifyFieldType(response, path, Map.class);
                for (String field : List.of("slug", "title", "image", "category_name")) {
                    ApiTestReporter.verifyNonBlankString(response, path + "." + field);
                }
                String slug = response.jsonPath().getString(path + ".slug");
                String title = response.jsonPath().getString(path + ".title");
                ApiTestReporter.verifyCondition("Product Information", path + " has a valid unique product slug",
                        "unique route slug", slug,
                        slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") && slugs.add(slug),
                        "Every search product must have a valid, unique route identity.");
                ApiTestReporter.verifyCondition("Product Information", path + " matches query in slug and title",
                        QUERY, slug + " / " + title,
                        Arrays.asList(slug.split("-")).contains(QUERY)
                                && Arrays.asList(title.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                                .contains(QUERY),
                        "Every priced product must match the Samsung search query.");

                ApiTestReporter.verifyFieldType(response, path + ".price", Number.class);
                BigDecimal price = new BigDecimal(response.jsonPath().get(path + ".price").toString());
                ApiTestReporter.verifyNonBlankString(response, path + ".discount");
                String rawDiscount = response.jsonPath().getString(path + ".discount");
                ApiTestReporter.verifyCondition("Product Information", path + " has a numeric discount",
                        "nonnegative decimal string", rawDiscount, rawDiscount.matches("\\d+(\\.\\d+)?"),
                        "Product discount must be a nonnegative decimal string.");
                BigDecimal discount = new BigDecimal(rawDiscount);
                BigDecimal sellingPrice = price.subtract(discount);
                ApiTestReporter.verifyCondition("Product Information", path + " has a valid selling price",
                        "price > 0 and 0 <= discount < price", sellingPrice,
                        price.signum() > 0 && discount.signum() >= 0 && sellingPrice.signum() > 0,
                        "Price minus discount must be a positive selling price.");
            }
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
