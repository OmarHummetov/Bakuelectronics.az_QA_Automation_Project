package az.bakuelectronics.automation.unit.api;

import az.bakuelectronics.automation.support.api.BasketItemKey;
import az.bakuelectronics.automation.support.api.BasketMutationGateway;
import az.bakuelectronics.automation.support.api.BasketSnapshot;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import az.bakuelectronics.automation.integration.BasketApiUiVisibilityTest;
import az.bakuelectronics.automation.support.api.ApiTestReporter;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.internal.TestResult;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.openqa.selenium.TimeoutException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class BasketStateRestorationTest {
    private static final BasketItemKey TEST = new BasketItemKey(null, "test-product", null, null);
    private static final BasketItemKey UNTOUCHED = new BasketItemKey(null, "untouched", null, null);

    @Test
    public void removesProductThatWasAbsentBeforeTest() {
        FakeGateway gateway = new FakeGateway();
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.begin(gateway, "absent");
        session.beforeMutation(TEST);
        gateway.add(TEST, 1);

        Assert.assertTrue(session.restore().success());
        Assert.assertNull(gateway.state.get(TEST));
    }

    @Test
    public void restoresOriginalQuantityWithoutTouchingOtherProducts() {
        FakeGateway gateway = new FakeGateway(Map.of(TEST, 1, UNTOUCHED, 7));
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.begin(gateway, "quantity");
        session.beforeMutation(TEST);
        gateway.simulateTestQuantityChange(TEST, 2);

        Assert.assertTrue(session.restore().success());
        Assert.assertEquals(gateway.state.get(TEST), Integer.valueOf(1));
        Assert.assertEquals(gateway.state.get(UNTOUCHED), Integer.valueOf(7));
        Assert.assertEquals(gateway.updateCalls, 1);
    }

    @Test
    public void restoresProductDeletedDuringTest() {
        FakeGateway gateway = new FakeGateway(Map.of(TEST, 3));
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.begin(gateway, "deleted");
        session.beforeMutation(TEST);
        gateway.remove(TEST);

        Assert.assertTrue(session.restore().success());
        Assert.assertEquals(gateway.state.get(TEST), Integer.valueOf(3));
    }

    @Test
    public void restoresWhenMutationAppliedBeforeClientTimeout() {
        FakeGateway gateway = new FakeGateway();
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.begin(gateway, "timeout");
        session.beforeMutation(TEST);
        try {
            gateway.add(TEST, 1);
            throw new RuntimeException("client timeout after server accepted request");
        } catch (RuntimeException expected) {

        }

        Assert.assertTrue(session.restore().success());
        Assert.assertNull(gateway.state.get(TEST));
    }

    @DataProvider
    public Object[][] basketBaselines() {
        return new Object[][]{{false}, {true}};
    }

    @Test(dataProvider = "basketBaselines")
    public void integrationCleanupAfterUiFailureRestoresOnlySelectedProduct(boolean alreadyPresent) throws Exception {
        FakeGateway gateway = new FakeGateway(alreadyPresent
                ? Map.of(TEST, 3, UNTOUCHED, 7) : Map.of(UNTOUCHED, 7));
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.beginLocked(gateway, "after-ui-login");
        try {
            session.beforeMutation(TEST.slug());
            gateway.add(TEST, 1);
            gateway.simulateTestQuantityChange(UNTOUCHED, 8);
            ITestResult result = TestResult.newEmptyTestResult();
            TimeoutException original = new TimeoutException("Selected product missing after UI login");
            result.setThrowable(original);
            result.setStatus(ITestResult.FAILURE);

            BasketApiUiVisibilityTest integration = integrationWithSession(session);
            integration.cleanupBasket(result);
            integration.cleanupBasket(result);

            Assert.assertSame(result.getThrowable(), original);
            Assert.assertEquals(result.getStatus(), ITestResult.FAILURE);
            Assert.assertEquals(gateway.state.get(TEST), alreadyPresent ? Integer.valueOf(3) : null);
            Assert.assertEquals(gateway.state.get(UNTOUCHED), Integer.valueOf(8));
            Assert.assertEquals(gateway.addCalls, 1, "Cleanup must not duplicate the accepted addition.");
            Assert.assertEquals(gateway.removeCalls, alreadyPresent ? 0 : 1);
            Assert.assertEquals(gateway.updateCalls, alreadyPresent ? 1 : 0);
            Assert.assertEquals(gateway.snapshotReads, 3, "Baseline, current and restored state must be read.");
            assertMutationFileLockReleased();
        } finally {
            BasketTestSupport.unlock();
        }
    }

    @Test
    public void integrationCleanupFailureKeepsUiFailureAndReleasesLock() throws Exception {
        FakeGateway gateway = new FakeGateway(Map.of(UNTOUCHED, 7));
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.beginLocked(gateway, "ui-cleanup-failure");
        try {
            session.beforeMutation(TEST);
            gateway.add(TEST, 1);
            gateway.failRemove = true;
            ITestResult result = TestResult.newEmptyTestResult();
            TimeoutException original = new TimeoutException("UI visibility timed out after login");
            result.setThrowable(original);
            result.setStatus(ITestResult.FAILURE);

            integrationWithSession(session).cleanupBasket(result);

            Assert.assertSame(result.getThrowable(), original);
            Assert.assertEquals(original.getSuppressed().length, 1);
            Assert.assertTrue(original.getSuppressed()[0].getMessage().contains("Basket cleanup failed"));
            Assert.assertEquals(gateway.state.get(UNTOUCHED), Integer.valueOf(7));
            Assert.assertEquals(gateway.removeCalls, 1);
            Assert.assertEquals(gateway.addCalls, 1);
            assertMutationFileLockReleased();
        } finally {
            BasketTestSupport.unlock();
        }
    }

    private static BasketApiUiVisibilityTest integrationWithSession(
            BasketTestSupport.BasketRestoreSession session) throws Exception {
        BasketApiUiVisibilityTest integration = new BasketApiUiVisibilityTest();
        java.lang.reflect.Field field = BasketApiUiVisibilityTest.class.getDeclaredField("basketSession");
        field.setAccessible(true);
        field.set(integration, session);
        java.lang.reflect.Field identity = BasketApiUiVisibilityTest.class.getDeclaredField("reportIdentity");
        identity.setAccessible(true);
        identity.set(integration, ApiTestReporter.newInvocationIdentity("offline-ui-cleanup"));
        return integration;
    }

    private static void assertMutationFileLockReleased() throws IOException {
        Path lockPath = Path.of(System.getProperty("basket.mutation.lock.file",
                Path.of(System.getProperty("java.io.tmpdir"), "baku-electronics-basket-mutation.lock").toString()));
        try (FileChannel channel = FileChannel.open(lockPath, java.nio.file.StandardOpenOption.WRITE);
             FileLock lock = channel.tryLock()) {
            Assert.assertNotNull(lock, "Integration finally must release the mutation file lock.");
        }
    }

    @Test
    public void cleanupFailureIsReportedAndRepeatRestoreIsIdempotent() {
        FakeGateway gateway = new FakeGateway();
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.begin(gateway, "failure");
        session.beforeMutation(TEST);
        gateway.add(TEST, 1);
        gateway.failRemove = true;

        BasketTestSupport.BasketRestoreResult first = session.restore();
        BasketTestSupport.BasketRestoreResult second = session.restore();

        Assert.assertFalse(first.success());
        Assert.assertFalse(second.success());
        Assert.assertSame(second, first);
        Assert.assertEquals(gateway.removeCalls, 1);
    }

    @Test
    public void beginFailureReleasesAcquiredMutationLock() {
        BasketMutationGateway failingGateway = new FakeGateway() {
            @Override
            public BasketSnapshot readSnapshot() {
                throw new IllegalStateException("snapshot unavailable");
            }
        };

        IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                () -> BasketTestSupport.beginLocked(failingGateway, "begin-failure"));
        Assert.assertEquals(failure.getMessage(), "snapshot unavailable");
        BasketTestSupport.lock();
        BasketTestSupport.unlock();
    }

    @Test
    public void restoreFailureIsSuppressedWithoutReplacingOriginalFailure() {
        ITestResult result = TestResult.newEmptyTestResult();
        AssertionError original = new AssertionError("test assertion failed");
        result.setThrowable(original);
        result.setStatus(org.testng.ITestResult.FAILURE);

        BasketTestSupport.reportRestoreFailure(result,
                BasketTestSupport.BasketRestoreResult.failed(java.util.List.of("restore failed")), "cleanup-test");

        Assert.assertSame(result.getThrowable(), original);
        Assert.assertEquals(original.getSuppressed().length, 1);
        Assert.assertTrue(original.getSuppressed()[0].getMessage().contains("restore failed"));
    }

    @Test
    public void restoresDiscoveredUiChangesWithoutTouchingUnchangedLines() {
        BasketItemKey discovered = new BasketItemKey(null, "discovered", null, null);
        FakeGateway gateway = new FakeGateway(Map.of(UNTOUCHED, 7));
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.begin(gateway, "ui-diff");
        gateway.add(discovered, 1);

        Assert.assertTrue(session.restoreAllChanges().success());
        Assert.assertNull(gateway.state.get(discovered));
        Assert.assertEquals(gateway.state.get(UNTOUCHED), Integer.valueOf(7));
    }

    @Test
    public void lockCanBeReleasedSafelyWhenSetupDidNotAcquireIt() {
        BasketTestSupport.unlock();
        BasketTestSupport.lock();
        BasketTestSupport.unlock();
        BasketTestSupport.unlock();
    }

    @Test
    public void fileLockTimeoutIsReportedAndJvmLockIsReleased() throws IOException {
        Path lockPath = Files.createTempFile("basket-mutation-lock-test", ".lock");
        String previousPath = System.getProperty("basket.mutation.lock.file");
        String previousTimeout = System.getProperty("basket.mutation.lock.timeout.ms");
        try (FileChannel channel = FileChannel.open(lockPath, java.nio.file.StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            System.setProperty("basket.mutation.lock.file", lockPath.toString());
            System.setProperty("basket.mutation.lock.timeout.ms", "100");

            IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                    BasketTestSupport::lock);
            Assert.assertTrue(failure.getMessage().contains("Timed out"), failure.getMessage());


            ignored.release();
            BasketTestSupport.lock();
            BasketTestSupport.unlock();
        } finally {
            restoreProperty("basket.mutation.lock.file", previousPath);
            restoreProperty("basket.mutation.lock.timeout.ms", previousTimeout);
            Files.deleteIfExists(lockPath);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }


    private static class FakeGateway implements BasketMutationGateway {
        private final Map<BasketItemKey, Integer> state = new LinkedHashMap<>();
        private int updateCalls;
        private int removeCalls;
        private int addCalls;
        private int snapshotReads;
        private boolean failRemove;

        private FakeGateway() {
        }

        private FakeGateway(Map<BasketItemKey, Integer> initial) {
            state.putAll(initial);
        }

        @Override
        public BasketSnapshot readSnapshot() {
            snapshotReads++;
            return BasketSnapshot.from(state);
        }

        @Override
        public void add(BasketItemKey key, int quantity) {
            addCalls++;
            state.put(key, quantity);
        }

        private void simulateTestQuantityChange(BasketItemKey key, int quantity) {
            state.put(key, quantity);
        }

        @Override
        public void update(BasketItemKey key, int quantity) {
            updateCalls++;
            state.put(key, quantity);
        }

        @Override
        public void remove(BasketItemKey key) {
            removeCalls++;
            if (failRemove) {
                throw new IllegalStateException("simulated cleanup failure");
            }
            state.remove(key);
        }
    }
}
