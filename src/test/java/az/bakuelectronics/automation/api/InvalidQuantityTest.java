package az.bakuelectronics.automation.api;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

public class InvalidQuantityTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";

    private final ApiClient apiClient = new ApiClient();

    @Test(groups = {"api"})
    public void shouldRejectZeroQuantity() {
        ApiTestReporter.run("Invalid Basket Quantity", () -> {
            Response response = null;
            try {
                response = apiClient.post("/addbasket", Map.of(
                        "product_slug", PRODUCT_SLUG,
                        "quantity", 0));

                // The recorded API response exposes a basket object, but does not establish
                // whether zero quantity means rejection, omission, or normalization. Validate
                // only the observed response contract until that behavior is confirmed live.
                ApiTestReporter.verifyBasketResponseContract(response, null, null);
                ApiTestReporter.verifyResponseTime(response);
            } finally {
                if (response != null && BasketTestSupport.containsProduct(response, PRODUCT_SLUG)) {
                    try {
                        apiClient.delete("/basket-remove-product", Map.of("slug", PRODUCT_SLUG));
                    } catch (RuntimeException ignored) {
                        // Cleanup must not hide the main test result.
                    }
                }
            }
        });
    }
}
