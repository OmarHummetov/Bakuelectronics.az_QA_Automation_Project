package az.bakuelectronics.automation.integration;

import az.bakuelectronics.automation.api.ApiClient;
import az.bakuelectronics.automation.api.ApiTestReporter;
import az.bakuelectronics.automation.api.BasketTestCleanup;
import az.bakuelectronics.automation.api.ProductApiService;
import az.bakuelectronics.automation.models.ProductApiModel;
import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.LoginPage;
import az.bakuelectronics.automation.support.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

@Epic("API and UI Integration")
@Feature("Basket API to UI")
public class BasketApiToUiIntegrationTest extends BaseTest {

    private static final int QUANTITY = 1;

    private final ApiClient apiClient = new ApiClient();
    private String addedProductSlug;
    private boolean cleanupRequired;

    @BeforeMethod(alwaysRun = true)
    public void lockBasketMutation() {
        addedProductSlug = null;
        cleanupRequired = false;
        BasketTestCleanup.lock();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupBasket(ITestResult result) {
        try {
            if (cleanupRequired && addedProductSlug != null) {
                boolean cleanupSucceeded = BasketTestCleanup.removeProduct(
                        apiClient, addedProductSlug, getClass().getSimpleName());
                if (!cleanupSucceeded && result.getStatus() == ITestResult.SUCCESS) {
                    ApiTestReporter.recordPostTestFailure("Basket API/UI Integration");
                    result.setStatus(ITestResult.FAILURE);
                    result.setThrowable(new AssertionError(
                            "Basket cleanup failed for the integration test product."));
                }
            }
        } finally {
            BasketTestCleanup.unlock();
        }
    }

    @Story("Add a product with API and verify it in the same UI account basket")
    @Test(groups = {"integration", "regression"})
    public void shouldShowApiAddedProductInUiBasket() {
        ApiTestReporter.run("Basket API/UI Integration", () -> {
        ProductApiModel product = new ProductApiService(apiClient)
                .getFirstDefaultSearchProductWithCompleteData();
        ApiTestReporter.verifyValuePresent("Selected API product has a technical identifier", product.getSlug());
        addedProductSlug = product.getSlug();
        System.out.println("Integration phase: API account authentication uses configured credentials");

        Response addResponse = apiClient.post("/addbasket", Map.of(
                "product_slug", addedProductSlug,
                "quantity", QUANTITY));
        System.out.println("Integration phase: API basket mutation completed with HTTP "
                + addResponse.statusCode());
        ApiTestReporter.verifyBasketResponseContract(addResponse, addedProductSlug, QUANTITY);
        cleanupRequired = true;

        System.out.println("Integration phase: UI login uses the same configured credentials");
        new HomePage(driver()).open()
                .dismissCampaignPopupIfPresent()
                .clickAccountLink();
        new LoginPage(driver()).loginWithConfiguredCredentials();
        System.out.println("Integration phase: UI login completed");

        CartPage cartPage = new CartPage(driver());
        cartPage.openCart();
        cartPage.waitForCartPage();
        boolean productVisible = cartPage.isProductVisible(addedProductSlug);
        ApiTestReporter.verifyUiProductVisible(addedProductSlug, productVisible);

        Integer uiQuantity = cartPage.getProductQuantity(addedProductSlug);
        ApiTestReporter.verifyUiProductQuantity(addedProductSlug, 1, uiQuantity);
        System.out.println("Integration phase: UI basket contains API product slug " + addedProductSlug);
        });
    }
}
