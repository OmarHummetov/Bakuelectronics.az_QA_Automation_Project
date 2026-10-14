package az.bakuelectronics.automation.api;

import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

@Epic("API Automation")
@Feature("Basket API")
public class AddBasketTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";
    private static final int QUANTITY = 1;

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

    @Story("Add product to basket")
    @Test(groups = "api")
    public void shouldAddProductToBasket() {
        ApiTestReporter.run("Add Basket", () -> {
            Response response = apiClient.post("/addbasket", Map.of(
                    "product_slug", PRODUCT_SLUG,
                    "quantity", QUANTITY));

            ApiTestReporter.verifyBasketResponseContract(response, PRODUCT_SLUG, QUANTITY);
            cleanupRequired = true;
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}

final class BasketTestSupport {

    private static final Logger LOGGER = Logger.getLogger(BasketTestSupport.class.getName());
    private static final ReentrantLock MUTATION_LOCK = new ReentrantLock();

    private BasketTestSupport() {
    }

    static void lock() {
        MUTATION_LOCK.lock();
    }

    static void unlock() {
        MUTATION_LOCK.unlock();
    }

    static boolean removeProduct(ApiClient apiClient, String productSlug, String testName) {
        try {
            Response response = apiClient.delete(
                    "/basket-remove-product", Map.of("slug", productSlug));
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return true;
            }

            String message = testName + " basket cleanup returned HTTP " + statusCode;
            logCleanupFailure(message, null);
            return false;
        } catch (RuntimeException exception) {
            logCleanupFailure(testName + " basket cleanup could not be completed.", exception);
            return false;
        }
    }

    static boolean containsProduct(Response response, String productSlug) {
        try {
            return response.jsonPath().getList("products.slug") != null
                    && response.jsonPath().getList("products.slug").contains(productSlug);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.FINE, "Could not inspect basket response for cleanup.", exception);
            return false;
        }
    }

    private static void logCleanupFailure(String message, Throwable exception) {
        if (exception == null) {
            LOGGER.warning(message);
        } else {
            LOGGER.log(Level.WARNING, message, exception);
        }
        try {
            Allure.attachment("Basket cleanup failure", "text/plain",
                    new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)),
                    AttachmentOptions.withFileExtension(".txt"));
        } catch (RuntimeException attachmentException) {
            LOGGER.log(Level.FINE, "Could not attach basket cleanup failure to Allure.",
                    attachmentException);
        }
    }
}
