package az.bakuelectronics.automation.api;

import io.restassured.response.Response;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class UpdateBasketTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";

    private final ApiClient apiClient = new ApiClient();
    private boolean cleanupRequired;

    @BeforeMethod(alwaysRun = true)
    public void lockBasketMutation() {
        cleanupRequired = false;
        BasketTestSupport.lock();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupBasket(ITestResult result) {
        try {
            if (cleanupRequired) {
                boolean cleanupSucceeded = BasketTestSupport.removeProduct(
                        apiClient, PRODUCT_SLUG, getClass().getSimpleName());
                if (!cleanupSucceeded && result.getStatus() == ITestResult.SUCCESS) {
                    ApiTestReporter.recordPostTestFailure(getClass().getSimpleName());
                    result.setStatus(ITestResult.FAILURE);
                    result.setThrowable(new AssertionError(
                            "Basket cleanup failed for the test product."));
                }
            }
        } finally {
            BasketTestSupport.unlock();
        }
    }

    @Test(groups = "api")
    public void shouldUpdateProductQuantityInBasket() {
        ApiTestReporter.run("Update Basket", () -> {
            Response addResponse = apiClient.post("/addbasket", Map.of(
                    "product_slug", PRODUCT_SLUG,
                    "quantity", 1));
            ApiTestReporter.verifySetupResponse(addResponse, 200);
            cleanupRequired = true;

            Response response = apiClient.put("/updateBasket", Map.of(
                    "product_slug", PRODUCT_SLUG,
                    "quantity", 2));

            ApiTestReporter.verifyBasketResponseContract(response, PRODUCT_SLUG, 2);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
