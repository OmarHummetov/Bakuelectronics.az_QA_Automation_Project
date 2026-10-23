package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketTestSupport;

import io.restassured.response.Response;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class BasketProductRemovalTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";

    private final ApiClient apiClient = new ApiClient();
    private BasketTestSupport.BasketRestoreSession basketSession;
    private String reportIdentity;

    @BeforeMethod(alwaysRun = true)
    public void lockBasketMutation() {
        reportIdentity = ApiTestReporter.newInvocationIdentity(getClass().getSimpleName());
        basketSession = BasketTestSupport.beginLocked(apiClient, getClass().getSimpleName());
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupBasket(ITestResult result) {
        try {
            if (basketSession != null) {
                BasketTestSupport.BasketRestoreResult restore = basketSession.restore();
                if (!restore.success()) {
                    BasketTestSupport.reportRestoreFailure(result, restore, reportIdentity,
                            getClass().getSimpleName());
                }
            }
        } finally {
            BasketTestSupport.unlock();
            ApiTestReporter.finish(reportIdentity);
        }
    }

    @Test(groups = {"api", "authenticated-api", "api-mutation"})
    public void shouldRemoveProductFromBasket() {
        ApiTestReporter.run(reportIdentity, "Delete Basket", () -> {
            basketSession.beforeMutation(PRODUCT_SLUG);
            Response addResponse = apiClient.post("/addbasket", Map.of(
                    "product_slug", PRODUCT_SLUG,
                    "quantity", 1));
            ApiTestReporter.verifySetupResponse(addResponse, 200);
            Response response = apiClient.delete(
                    "/basket-remove-product", Map.of("slug", PRODUCT_SLUG));

            ApiTestReporter.verifyBasketResponseContract(response, null, null);
            ApiTestReporter.verifyBasketProductAbsent(response, PRODUCT_SLUG);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
