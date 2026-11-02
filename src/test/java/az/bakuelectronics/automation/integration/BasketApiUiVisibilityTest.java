package az.bakuelectronics.automation.integration;

import az.bakuelectronics.automation.api.ApiClient;
import az.bakuelectronics.automation.api.ProductApiService;
import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.driver.DriverManager;
import az.bakuelectronics.automation.models.ProductApiModel;
import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.LoginPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.AccountCartOwnership;
import az.bakuelectronics.automation.support.PrivateCartJournal;
import az.bakuelectronics.automation.support.api.ApiClientBasketMutationGateway;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.testng.ITestResult;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;

@Epic("API and UI Integration")
@Feature("Basket API to UI")
public class BasketApiUiVisibilityTest extends BaseTest {

    private static final int QUANTITY = 1;

    private final ApiClient apiClient = new ApiClient();
    private String addedProductSlug;
    private BasketTestSupport.BasketRestoreSession basketSession;
    private String reportIdentity;
    private AccountCartOwnership ownership;
    private PrivateCartJournal journal;

    @BeforeMethod(alwaysRun = true)
    public void lockBasketMutation() {
        addedProductSlug = null;
        basketSession = null;
        ownership = null;
        journal = null;
        reportIdentity = ApiTestReporter.newInvocationIdentity(getClass().getSimpleName());
        basketSession = BasketTestSupport.beginLocked(apiClient, getClass().getSimpleName());
        Map<String, Integer> baseline = new java.util.HashMap<>();
        new ApiClientBasketMutationGateway(apiClient).readSnapshot().quantities().forEach((key, quantity) -> {
            Assert.assertNull(baseline.put(key.mutationSlug(), quantity), "Basket rows must have unique slugs");
        });
        ownership = new AccountCartOwnership(baseline);
        try {
            journal = new PrivateCartJournal(Path.of(".basket-api-ui.secret").toAbsolutePath());
            journal.save(ownership, "baseline captured before integration mutation");
        } catch (IOException failure) {
            throw new IllegalStateException("Private integration ownership journal unavailable");
        }
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupBasket(ITestResult result) {
        try {
            if (basketSession != null) {
                BasketTestSupport.BasketRestoreResult restore = basketSession.restore();
                if (!restore.success()) {
                    BasketTestSupport.reportRestoreFailure(result, restore, reportIdentity,
                            "Basket API/UI Integration");
                } else if (ownership != null && journal != null) {
                    // A new browser must authenticate again; browser closure and HTTP 200 are insufficient.
                    DriverManager.quitDriver();
                    DriverManager.createDriver();
                    login();
                    driver().get(ConfigReader.getUiBaseUrl() + "/sebet");
                    CartPage cart = new CartPage(driver()).waitForCartPage();
                    driver().navigate().refresh();
                    Assert.assertTrue(ownership.baseline().equals(cart.waitForInventory()),
                            "Complete original basket must match after fresh login and refresh");
                    journal.complete();
                    Allure.step("RESTORED: complete original basket verified after fresh authentication and refresh");
                }
            }
        } catch (RuntimeException | AssertionError | IOException failure) {
            BasketTestSupport.reportRestoreFailure(result,
                    BasketTestSupport.BasketRestoreResult.failed(List.of(
                            "Independent basket restoration verification failed; retain ownership journal")),
                    reportIdentity, "Basket API/UI Integration");
        } finally {
            BasketTestSupport.unlock();
            ApiTestReporter.finish(reportIdentity);
        }
    }

    @Story("Add a product with API and verify it in the same UI account basket")
    @Test(groups = {"integration", "authenticated-api", "api-mutation"})
    public void shouldShowApiAddedProductInUiBasket() {
        ApiTestReporter.run(reportIdentity, "Basket API/UI Integration", () -> {
        ProductApiModel product = new ProductApiService(apiClient)
                .getFirstDefaultSearchProductWithCompleteData();
        ApiTestReporter.verifyValuePresent("Selected API product has a technical identifier", product.getSlug());
        addedProductSlug = product.getSlug();
        System.out.println("Integration phase: API account authentication uses configured credentials");

        ownership.register(addedProductSlug);
        try {
            journal.save(ownership, "test-owned add registered before dispatch");
        } catch (IOException failure) {
            throw new IllegalStateException("Integration ownership was not durably recorded");
        }
        basketSession.beforeMutation(addedProductSlug);
        Response addResponse = apiClient.post("/addbasket", Map.of(
                "product_slug", addedProductSlug,
                "quantity", QUANTITY));
        System.out.println("Integration phase: API basket mutation completed with HTTP "
                + addResponse.statusCode());
        ApiTestReporter.verifyBasketResponseContract(addResponse, addedProductSlug, QUANTITY);

        System.out.println("Integration phase: UI login uses the same configured credentials");
        login();
        System.out.println("Integration phase: UI login completed");

        CartPage cartPage = new CartPage(driver());
        cartPage.openCart();
        cartPage.waitForCartPage();
        boolean productVisible = cartPage.isProductVisible(addedProductSlug);
        ApiTestReporter.verifyUiProductVisible(addedProductSlug, productVisible);

        Integer uiQuantity = cartPage.waitForProductQuantity(addedProductSlug, QUANTITY);
        ApiTestReporter.verifyUiProductQuantity(addedProductSlug, QUANTITY, uiQuantity);
        System.out.println("Integration phase: UI basket contains API product slug " + addedProductSlug);
        });
    }

    private void login() {
        new HomePage(driver()).open().dismissCampaignPopupIfPresent().clickAccountLink();
        new LoginPage(driver()).loginWithConfiguredCredentials(java.time.Duration.ofSeconds(25),
                checkpoint -> Allure.attachment("Authentication checkpoint", checkpoint));
    }
}
