package az.bakuelectronics.automation.api;

public final class BasketTestCleanup {

    private BasketTestCleanup() {
    }

    public static void lock() {
        BasketTestSupport.lock();
    }

    public static void unlock() {
        BasketTestSupport.unlock();
    }

    public static boolean removeProduct(ApiClient apiClient, String productSlug, String testName) {
        boolean removed = BasketTestSupport.removeProduct(apiClient, productSlug, testName);
        if (removed) {
            System.out.println("[PASS] [Cleanup] Test product was removed successfully"
                    + " | Expected: HTTP 200 | Actual: HTTP 200"
                    + " | Technical identifier: " + productSlug);
        } else {
            System.out.println("[FAIL] [Cleanup] Test product removal failed"
                    + " | Expected: HTTP 200 | Actual: unsuccessful response"
                    + " | Technical identifier: " + productSlug);
        }
        return removed;
    }
}
