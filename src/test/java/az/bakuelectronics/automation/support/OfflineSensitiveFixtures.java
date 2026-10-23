package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.support.api.BasketItemKey;
import az.bakuelectronics.automation.support.api.BasketMutationGateway;
import az.bakuelectronics.automation.support.api.BasketSnapshot;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import org.openqa.selenium.TimeoutException;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static az.bakuelectronics.automation.support.SensitiveReportingTest.*;

public final class OfflineSensitiveFixtures {
    public static class OfflineFixture extends BaseTest {
        @Override @BeforeMethod(alwaysRun = true) public void setUp() { installDriver(); }
    }

    @SensitiveAuth
    public static class SensitiveFailure extends OfflineFixture {
        @Test public void failure() {
            attempts++;
            TimeoutException failure = new TimeoutException(PRIVATE + " " + URL,
                    new IllegalStateException(PRIVATE));
            failure.addSuppressed(new IllegalStateException(URL));
            throw failure;
        }
    }

    @SensitiveAuth
    public static class SetupFailure extends OfflineFixture {
        @Override @BeforeMethod(alwaysRun = true) public void setUp() {
            super.setUp();
            throw new IllegalStateException(PRIVATE + " " + URL);
        }
        @Test public void failure() { attempts++; }
    }

    @SensitiveAuth
    public static class TeardownFailure extends OfflineFixture {
        @Override @org.testng.annotations.AfterMethod(alwaysRun = true)
        public void tearDown(ITestResult result) {
            super.tearDown(result);
            throw new IllegalStateException(PRIVATE + " " + URL);
        }
        @Test public void failure() { attempts++; }
    }

    @SensitiveAuth
    public static class CleanupFailure extends BasketMutationBaseTest {
        @Override @BeforeMethod(alwaysRun = true) public void setUp() { installDriver(); }
        @Override @BeforeMethod(alwaysRun = true) public void beginBasketMutationSession() {
            basketStarts++;
            BasketItemKey product = new BasketItemKey(null, "offline-product", null, null);
            BasketMutationGateway gateway = new BasketMutationGateway() {
                private int reads;
                @Override public BasketSnapshot readSnapshot() {
                    return BasketSnapshot.from(reads++ == 0 ? Map.of() : Map.of(product, 1));
                }
                @Override public void add(BasketItemKey key, int quantity) { throw new AssertionError(); }
                @Override public void update(BasketItemKey key, int quantity) { throw new AssertionError(); }
                @Override public void remove(BasketItemKey key) { throw new IllegalStateException(PRIVATE + URL); }
            };
            var session = BasketTestSupport.beginLocked(gateway, "offline-cleanup");
            try {
                Field field = BasketMutationBaseTest.class.getDeclaredField("basketSession");
                field.setAccessible(true);
                field.set(this, session);
                field = BasketMutationBaseTest.class.getDeclaredField("reportIdentity");
                field.setAccessible(true);
                field.set(this, "offline-sensitive-cleanup");
            } catch (ReflectiveOperationException exception) {
                BasketTestSupport.unlock();
                throw new AssertionError("Could not install offline restore session.");
            }
        }
        @Test public void failure() { attempts++; }
    }

    public static class BodyAndCleanupFailure extends CleanupFailure {
        @Override @Test public void failure() { throw new TimeoutException(PRIVATE + URL); }
    }

    public static class OrdinaryFailure extends OfflineFixture {
        @Test(retryAnalyzer = NoRetryAnalyzer.class)
        public void failure() { throw new AssertionError("ordinary synthetic failure"); }
    }
}
