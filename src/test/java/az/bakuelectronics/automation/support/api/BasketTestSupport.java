package az.bakuelectronics.automation.support.api;

import az.bakuelectronics.automation.api.ApiClient;
import az.bakuelectronics.automation.support.SensitiveTestReporting;

import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BasketTestSupport {
    private static final Logger LOGGER = Logger.getLogger(BasketTestSupport.class.getName());
    private static final ReentrantLock MUTATION_LOCK = new ReentrantLock();
    private static final long DEFAULT_LOCK_TIMEOUT_MILLIS = 30_000L;
    private static FileChannel processLockChannel;
    private static FileLock processLock;

    private BasketTestSupport() {
    }

    public static void lock() {
        MUTATION_LOCK.lock();
        try {
            acquireProcessLock();
        } catch (RuntimeException exception) {
            MUTATION_LOCK.unlock();
            throw exception;
        }
    }

    public static void unlock() {
        if (MUTATION_LOCK.isHeldByCurrentThread()) {
            try {
                releaseProcessLock();
            } finally {
                MUTATION_LOCK.unlock();
            }
        }
    }

    private static void acquireProcessLock() {
        Path lockPath = Path.of(System.getProperty("basket.mutation.lock.file",
                Path.of(System.getProperty("java.io.tmpdir"), "baku-electronics-basket-mutation.lock").toString()));
        long timeout = Long.getLong("basket.mutation.lock.timeout.ms", DEFAULT_LOCK_TIMEOUT_MILLIS);
        if (timeout < 1) {
            throw new IllegalArgumentException("basket.mutation.lock.timeout.ms must be positive.");
        }
        long deadline = System.nanoTime() + timeout * 1_000_000L;
        try {
            processLockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            while (processLock == null && System.nanoTime() < deadline) {
                try {
                    processLock = processLockChannel.tryLock();
                } catch (OverlappingFileLockException ignored) {

                }
                if (processLock == null) {
                    Thread.sleep(50L);
                }
            }
            if (processLock == null) {
                closeProcessLockChannel();
                throw new IllegalStateException("Timed out after " + timeout
                        + " ms waiting for basket mutation file lock: " + lockPath);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            closeProcessLockChannel();
            throw new IllegalStateException("Interrupted while waiting for basket mutation file lock: " + lockPath,
                    exception);
        } catch (IOException exception) {
            closeProcessLockChannel();
            throw new IllegalStateException("Could not open basket mutation file lock: " + lockPath, exception);
        }
    }

    private static void releaseProcessLock() {
        try {
            if (processLock != null && processLock.isValid()) {
                processLock.release();
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not release basket mutation file lock.",
                    SensitiveTestReporting.safeFailure(exception));
        } finally {
            processLock = null;
            closeProcessLockChannel();
        }
    }

    private static void closeProcessLockChannel() {
        if (processLockChannel != null) {
            try {
                processLockChannel.close();
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Could not close basket mutation file lock channel.",
                        SensitiveTestReporting.safeFailure(exception));
            } finally {
                processLockChannel = null;
            }
        }
    }

    public static BasketRestoreSession begin(ApiClient apiClient, String testName) {
        return new BasketRestoreSession(new ApiClientBasketMutationGateway(apiClient), testName);
    }

    public static BasketRestoreSession beginLocked(ApiClient apiClient, String testName) {
        return beginLocked(new ApiClientBasketMutationGateway(apiClient), testName);
    }

    public static BasketRestoreSession beginLocked(BasketMutationGateway gateway, String testName) {
        lock();
        try {
            return begin(gateway, testName);
        } catch (RuntimeException | Error exception) {
            unlock();
            throw exception;
        }
    }

    public static void reportRestoreFailure(ITestResult result, BasketRestoreResult restore, String testName) {
        reportRestoreFailure(result, restore, testName, testName);
    }

    public static void reportRestoreFailure(ITestResult result, BasketRestoreResult restore,
                                            String identity, String displayName) {
        ApiTestReporter.recordPostTestFailure(identity, displayName);
        AssertionError cleanupFailure = new AssertionError("Basket cleanup failed: "
                + SensitiveTestReporting.safeDetail(restore.summary()));
        if (result.getThrowable() == null) {
            result.setThrowable(cleanupFailure);
        } else {
            result.getThrowable().addSuppressed(cleanupFailure);
        }
        result.setStatus(ITestResult.FAILURE);
    }

    public static BasketRestoreSession begin(BasketMutationGateway gateway, String testName) {
        return new BasketRestoreSession(gateway, testName);
    }

    public static boolean containsProduct(io.restassured.response.Response response, String productSlug) {
        try {
            List<String> slugs = response.jsonPath().getList("products.slug");
            return slugs != null && slugs.contains(productSlug);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.FINE, "Could not inspect basket response.", SensitiveTestReporting.safeFailure(exception));
            return false;
        }
    }

    public static final class BasketRestoreSession {
        private final BasketMutationGateway gateway;
        private final String testName;
        private final BasketSnapshot before;
        private final Set<BasketItemKey> mutated = new LinkedHashSet<>();
        private boolean restored;
        private BasketRestoreResult restoreResult;

        private BasketRestoreSession(BasketMutationGateway gateway, String testName) {
            this.gateway = gateway;
            this.testName = testName;
            this.before = gateway.readSnapshot();
        }

        public void beforeMutation(BasketItemKey key) {
            if (restored) {
                throw new IllegalStateException("Basket restore session is already complete.");
            }
            mutated.add(key);
        }

        public void beforeMutation(String slug) {
            beforeMutation(new BasketItemKey(null, slug, null, null));
        }

        public BasketRestoreResult restore() {
            return restore(false);
        }





        public BasketRestoreResult restoreAllChanges() {
            return restore(true);
        }

        public void verifyUnchanged() {
            BasketSnapshot current = gateway.readSnapshot();
            if (!before.quantities().equals(current.quantities())) {
                throw new AssertionError(testName + " changed the basket for an invalid mutation: expected="
                        + before.quantities() + ", actual=" + current.quantities());
            }
        }

        private BasketRestoreResult restore(boolean discoverChanges) {
            if (restored) {
                return restoreResult;
            }
            restored = true;
            List<String> errors = new ArrayList<>();
            try {
                BasketSnapshot current = gateway.readSnapshot();
                Set<BasketItemKey> keysToRestore = new LinkedHashSet<>(mutated);
                if (discoverChanges) {
                    keysToRestore.addAll(before.quantities().keySet());
                    keysToRestore.addAll(current.quantities().keySet());
                }
                for (BasketItemKey key : keysToRestore) {
                    restoreItem(current, key, errors);
                }
                if (errors.isEmpty()) {
                    verifyRestored(errors, keysToRestore);
                }
            } catch (RuntimeException exception) {
                errors.add(testName + " cleanup could not read/restore basket: " + message(exception));
            }
            if (!errors.isEmpty()) {
                logCleanupFailure(String.join(System.lineSeparator(), errors), null);
                restoreResult = BasketRestoreResult.failed(errors.stream()
                        .map(SensitiveTestReporting::safeDetail).toList());
                return restoreResult;
            }
            restoreResult = BasketRestoreResult.ok();
            Allure.step("Cleanup passed: baseline basket restored for " + testName);
            return restoreResult;
        }

        private void restoreItem(BasketSnapshot current, BasketItemKey key, List<String> errors) {
            Integer expected = before.quantity(key);
            Integer actual = current.quantity(key);
            try {
                if (expected == null && actual != null) {
                    gateway.remove(key);
                } else if (expected != null && actual == null) {
                    gateway.add(key, expected);
                } else if (expected != null && !expected.equals(actual)) {
                    gateway.update(key, expected);
                }
            } catch (RuntimeException exception) {
                errors.add(testName + " failed to restore " + key + ": " + message(exception));
            }
        }

        private void verifyRestored(List<String> errors, Set<BasketItemKey> keysToVerify) {
            BasketSnapshot after = gateway.readSnapshot();
            for (BasketItemKey key : keysToVerify) {
                Integer expected = before.quantity(key);
                Integer actual = after.quantity(key);
                if (!java.util.Objects.equals(expected, actual)) {
                    errors.add(testName + " restore verification failed for " + key
                            + ": expected=" + expected + ", actual=" + actual);
                }
            }
        }
    }

    public record BasketRestoreResult(boolean success, List<String> errors) {
        public static BasketRestoreResult ok() {
            return new BasketRestoreResult(true, List.of());
        }

        public static BasketRestoreResult failed(List<String> errors) {
            return new BasketRestoreResult(false, List.copyOf(errors));
        }

        public String summary() {
            return String.join(System.lineSeparator(), errors);
        }
    }

    private static String message(Throwable exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static void logCleanupFailure(String message, Throwable exception) {
        message = SensitiveTestReporting.safeDetail(message);
        if (SensitiveTestReporting.active()) exception = null;
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
                    SensitiveTestReporting.safeFailure(attachmentException));
        }
    }
}
