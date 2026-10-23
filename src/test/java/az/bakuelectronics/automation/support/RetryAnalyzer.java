package az.bakuelectronics.automation.support;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = Integer.getInteger("test.retry.max", 1);
    private int retryCount;

    @Override
    public boolean retry(ITestResult result) {
        if (result.getMethod() != null
                && RetryAnnotationTransformer.isExcluded(result.getMethod().getRealClass(),
                        result.getMethod().getGroups())) {
            return false;
        }
        if (result.isSuccess()) {
            return false;
        }

        if (retryCount < MAX_RETRIES && isTransientInfrastructureFailure(result.getThrowable())) {
            retryCount++;
            return true;
        }

        return false;
    }

    private static boolean isTransientInfrastructureFailure(Throwable throwable) {
        boolean transientFailure = false;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = throwable; current != null && seen.add(current); current = current.getCause()) {
            if (current instanceof AssertionError) {
                return false;
            }
            transientFailure |= isTransientSeleniumFailure(current);
        }
        return transientFailure;
    }

    private static boolean isTransientSeleniumFailure(Throwable throwable) {
        if (throwable instanceof StaleElementReferenceException
                || throwable instanceof ElementClickInterceptedException
                || throwable instanceof ElementNotInteractableException
                || throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof WebDriverException) {
            String message = throwable.getMessage();
            return message != null && (message.contains("disconnected")
                    || message.contains("not reachable")
                    || message.contains("target window already closed"));
        }
        return false;
    }
}
