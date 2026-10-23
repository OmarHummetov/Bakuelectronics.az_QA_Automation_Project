package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
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
@Feature("Basket API")
public class BasketProductAdditionTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";
    private static final int QUANTITY = 1;

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

    @Story("Add product to basket")
    @Test(groups = {"api", "authenticated-api", "api-mutation"})
    public void shouldAddProductToBasket() {
        ApiTestReporter.run(reportIdentity, "Add Basket", () -> {
            basketSession.beforeMutation(PRODUCT_SLUG);
            Response response = apiClient.post("/addbasket", Map.of(
                    "product_slug", PRODUCT_SLUG, "quantity", QUANTITY));
            ApiTestReporter.verifyBasketResponseContract(response, PRODUCT_SLUG, QUANTITY);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
