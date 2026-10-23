package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketTestSupport;

import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

import java.util.Map;

public class BasketInvalidSlugResponseTest {

    private static final String INVALID_PRODUCT_SLUG =
            "__api-test-nonexistent-product-slug__";

    private final ApiClient apiClient;
    private BasketTestSupport.BasketRestoreSession basketSession;
    private String reportIdentity;

    public BasketInvalidSlugResponseTest() {
        this(new ApiClient());
    }

    BasketInvalidSlugResponseTest(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

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
            basketSession = null;
            BasketTestSupport.unlock();
            ApiTestReporter.finish(reportIdentity);
        }
    }

    @Test(groups = {"api", "authenticated-api", "api-mutation"})
    public void shouldRejectNonexistentProductSlug() {
        ApiTestReporter.run(reportIdentity, "Invalid Product Slug", () -> {

            basketSession.verifyUnchanged();
            Response response = apiClient.post("/addbasket", Map.of(
                    "product_slug", INVALID_PRODUCT_SLUG,
                    "quantity", 1));


            basketSession.verifyUnchanged();

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
