package az.bakuelectronics.automation.support;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public final class NoRetryAnalyzer implements IRetryAnalyzer {
    @Override
    public boolean retry(ITestResult result) {
        return false;
    }
}
