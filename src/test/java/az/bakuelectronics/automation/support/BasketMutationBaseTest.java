package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.api.ApiClient;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;


public abstract class BasketMutationBaseTest extends BaseTest {
    private final ApiClient basketApiClient = new ApiClient();
    private BasketTestSupport.BasketRestoreSession basketSession;
    private String reportIdentity;

    @Override
    protected void finishTestInvocation(ITestResult result) {
        if (SensitiveTestReporting.isSensitive(result)) {
            restoreBasketMutationSession(result);
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void beginBasketMutationSession() {
        reportIdentity = ApiTestReporter.newInvocationIdentity(getClass().getSimpleName());
        basketSession = BasketTestSupport.beginLocked(basketApiClient, getClass().getSimpleName());
    }

    @AfterMethod(alwaysRun = true)
    public void restoreBasketMutationSession(ITestResult result) {
        try {
            if (basketSession != null) {
                BasketTestSupport.BasketRestoreResult restore = basketSession.restoreAllChanges();
                if (!restore.success()) {
                    BasketTestSupport.reportRestoreFailure(result, restore, reportIdentity,
                            getClass().getSimpleName());
                    ApiTestReporter.finish(reportIdentity);
                }
            }
        } finally {
            basketSession = null;
            BasketTestSupport.unlock();
        }
    }
}
