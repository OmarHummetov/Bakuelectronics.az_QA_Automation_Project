package az.bakuelectronics.automation.api;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

public class InvalidProductSlugTest {

    private static final String INVALID_PRODUCT_SLUG =
            "__api-test-nonexistent-product-slug__";

    private final ApiClient apiClient = new ApiClient();

    @Test(groups = {"api"})
    public void shouldRejectNonexistentProductSlug() {
        ApiTestReporter.run("Invalid Product Slug", () -> {
            Response response = apiClient.post("/addbasket", Map.of(
                    "product_slug", INVALID_PRODUCT_SLUG,
                    "quantity", 1));

            ApiTestReporter.verifyJsonResponse(response);
            ApiTestReporter.verifyJsonRoot(response);
            Map<String, Object> responseBody = response.jsonPath().getMap("$");
            Object message = firstPresent(responseBody, "detail", "message", "error");
            ApiTestReporter.verifyValuePresent("Invalid slug error message", message);
            ApiTestReporter.verifyNonBlankValue("Invalid slug error message", message);
            ApiTestReporter.verifyResponseTime(response);
        });
    }

    private static Object firstPresent(Map<String, Object> responseBody, String... fields) {
        if (responseBody == null) {
            return null;
        }
        for (String field : fields) {
            if (responseBody.containsKey(field)) {
                return responseBody.get(field);
            }
        }
        return null;
    }
}
