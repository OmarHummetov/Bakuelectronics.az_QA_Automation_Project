package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketItemKey;
import az.bakuelectronics.automation.support.api.BasketSnapshot;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

@Epic("API Automation")
@Feature("Basket Quantity Boundary")
public class BasketZeroQuantityRemovalTest {

    private static final String PRODUCT_SLUG = System.getProperty("basket.zero.product.slug",
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768");

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

    @Story("Quantity one becomes zero and removes the product")
    @Test(groups = {"api", "authenticated-api", "api-mutation"})
    public void shouldRemoveProductWhenQuantityIsUpdatedToZero() {
        ApiTestReporter.run(reportIdentity, "Update basket quantity 1 to 0", () -> {
            ApiTestReporter.verifyValuePresent("Test product slug", PRODUCT_SLUG);
            BasketSnapshot initial = BasketSnapshot.fromResponse(apiClient.get("/getBasket"));
            Map<BasketItemKey, Integer> otherProducts = withoutProduct(initial.quantities());
            basketSession.beforeMutation(PRODUCT_SLUG);

            Integer initialQuantity = quantityFor(initial);
            Response setupResponse;
            if (initialQuantity == null) {
                setupResponse = apiClient.post("/addbasket", Map.of(
                        "product_slug", PRODUCT_SLUG,
                        "quantity", 1));
            } else if (initialQuantity != 1) {
                setupResponse = apiClient.put("/updateBasket", Map.of(
                        "product_slug", PRODUCT_SLUG,
                        "quantity", 1));
            } else {
                setupResponse = apiClient.get("/getBasket");
            }
            ApiTestReporter.verifySetupResponse(setupResponse, 200);
            ApiTestReporter.verifyBasketResponseContract(setupResponse, PRODUCT_SLUG, 1);
            ApiTestReporter.verifyEquals("Quantity before mutation", 1, quantityFor(
                    BasketSnapshot.fromResponse(setupResponse)));

            Response response = apiClient.put("/updateBasket", Map.of(
                    "product_slug", PRODUCT_SLUG,
                    "quantity", 0));
            ApiTestReporter.verifyStatus(response, 200);
            ApiTestReporter.verifyBasketResponseContract(response, null, null);
            ApiTestReporter.verifyBasketProductAbsent(response, PRODUCT_SLUG);
            ApiTestReporter.verifyResponseTime(response);

            Response finalBasket = apiClient.get("/getBasket");
            ApiTestReporter.verifyStatus(finalBasket, 200);
            ApiTestReporter.verifyBasketResponseContract(finalBasket, null, null);
            ApiTestReporter.verifyBasketProductAbsent(finalBasket, PRODUCT_SLUG);
            BasketSnapshot after = BasketSnapshot.fromResponse(finalBasket);
            ApiTestReporter.verifyEquals("Other basket products unchanged", otherProducts,
                    withoutProduct(after.quantities()));
        });
    }

    private Integer quantityFor(BasketSnapshot snapshot) {
        return snapshot.quantities().entrySet().stream()
                .filter(entry -> PRODUCT_SLUG.equals(entry.getKey().slug()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private Map<BasketItemKey, Integer> withoutProduct(Map<BasketItemKey, Integer> quantities) {
        Map<BasketItemKey, Integer> result = new java.util.LinkedHashMap<>(quantities);
        result.entrySet().removeIf(entry -> PRODUCT_SLUG.equals(entry.getKey().slug()));
        return result;
    }
}
