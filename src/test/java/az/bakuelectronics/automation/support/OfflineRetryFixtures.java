package az.bakuelectronics.automation.support;

import org.openqa.selenium.StaleElementReferenceException;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public final class OfflineRetryFixtures {
    static final Map<String, Integer> ATTEMPTS = new HashMap<>();

    private static void failTransient(String key) {
        ATTEMPTS.merge(key, 1, Integer::sum);
        throw new StaleElementReferenceException("synthetic offline failure");
    }

    public static class OfflineBase extends BaseTest {
        @Override @BeforeMethod(alwaysRun = true)
        public void setUp() { }
        @Override @AfterMethod(alwaysRun = true)
        public void tearDown(ITestResult result) { }
    }

    public static class MethodFixture extends OfflineBase {
        @Test public void transientFailure() { failTransient("method-transient"); }
        @Test public void assertion() {
            ATTEMPTS.merge("assertion", 1, Integer::sum);
            throw new AssertionError("synthetic assertion");
        }
        @Test public void wrappedAssertion() {
            ATTEMPTS.merge("wrapped-assertion", 1, Integer::sum);
            throw new StaleElementReferenceException("synthetic", new AssertionError("mismatch"));
        }
        @Test(retryAnalyzer = NoRetryAnalyzer.class)
        public void explicit() { failTransient("explicit"); }
        @Test(retryAnalyzer = CustomAnalyzer.class)
        public void custom() { failTransient("custom"); }
        @Test(groups = "no-retry", retryAnalyzer = RetryAnalyzer.class)
        public void noRetry() { failTransient("no-retry"); }
        @Test(groups = "api-mutation", retryAnalyzer = RetryAnalyzer.class)
        public void apiMutation() { failTransient("api-mutation"); }
        @Test(groups = "ui-mutation") public void uiMutation() { failTransient("ui-mutation"); }
        @Test(groups = "manual-auth") public void manualAuth() { failTransient("manual-auth"); }
        @Test(groups = "sensitive-auth") public void sensitiveAuth() { failTransient("sensitive-auth"); }
    }

    @Test
    public static class ClassFixture extends OfflineBase {
        public void transientFailure() { failTransient("class-transient"); }
    }

    @Test(groups = "no-retry")
    public static class ClassExcludedFixture extends OfflineBase {
        @Test(retryAnalyzer = RetryAnalyzer.class)
        public void failure() { failTransient("class-group"); }
    }

    @Test(retryAnalyzer = NoRetryAnalyzer.class)
    public static class ClassExplicitFixture extends OfflineBase {
        @Test(retryAnalyzer = RetryAnalyzer.class)
        public void failure() { failTransient("class-explicit"); }
    }

    public static class InheritedExcludedFixture extends ClassExplicitFixture { }

    public static class CustomAnalyzer implements IRetryAnalyzer {
        @Override public boolean retry(ITestResult result) { return false; }
    }
}
