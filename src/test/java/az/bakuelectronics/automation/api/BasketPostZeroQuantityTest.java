package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketTestSupport;

import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.Map;

public class BasketPostZeroQuantityTest {

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
                BasketTestSupport.BasketRestoreResult restore = basketSession.restoreAllChanges();
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
    public void shouldExposeZeroQuantityContractMismatchUntilContractIsConfirmed() {
        ApiTestReporter.run(reportIdentity, "Invalid Basket Quantity", () -> {
            Response response = apiClient.post("/addbasket", Map.of(
                        "product_slug", PRODUCT_SLUG,
                        "quantity", 0));
            ApiTestReporter.verifyStatusOneOf(response, 200, 400, 422);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ApiTestReporter.verifyJsonResponse(response);
            }
            try {
                basketSession.verifyUnchanged();
            } catch (AssertionError assertionError) {
                throw new AssertionError("Zero-quantity contract is unconfirmed: the API accepted quantity=0 "
                        + "and changed the basket. Observed behavior must not be treated as approved normalization "
                        + "without an authoritative contract; investigate as a potential backend bug.", assertionError);
            }
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
