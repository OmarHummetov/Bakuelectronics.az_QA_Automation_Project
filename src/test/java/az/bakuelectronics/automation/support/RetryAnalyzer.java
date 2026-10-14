package az.bakuelectronics.automation.support;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;


public final class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = 1;
    private int retryCount;

    @Override
    public boolean retry(ITestResult result) {
        if (result.isSuccess()) {
            return false;
        }

        if (retryCount < MAX_RETRIES) {
            retryCount++;
            return true;
        }

        return false;
    }
}
