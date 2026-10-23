package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.support.api.BasketItemKey;
import az.bakuelectronics.automation.support.api.BasketMutationGateway;
import az.bakuelectronics.automation.support.api.BasketSnapshot;
import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups = {"unit", "read-only"})
public class InvalidSlugDiagnosticSafetyTest {
    private static final BasketItemKey ITEM = new BasketItemKey(null, "synthetic-private-item", null, null);

    public void requiresAllOptIns() {
        Assert.expectThrows(SkipException.class,
                () -> InvalidProductSlugDiagnosticTest.requireOptIn(false, "isolated-test-account", "fake"));
        Assert.expectThrows(SkipException.class,
                () -> InvalidProductSlugDiagnosticTest.requireOptIn(true, "personal", "fake"));
        Assert.expectThrows(SkipException.class,
                () -> InvalidProductSlugDiagnosticTest.requireOptIn(true, "isolated-test-account", " "));
        InvalidProductSlugDiagnosticTest.requireOptIn(true, "isolated-test-account", "fake");
    }

    public void unchangedBasketUsesOneAttemptAndNoCleanupMutation() {
        FakeGateway gateway = new FakeGateway();
        AtomicInteger attempts = new AtomicInteger();
        InvalidProductSlugDiagnosticTest.execute(gateway, () -> {
            attempts.incrementAndGet();
            return response();
        });
        Assert.assertEquals(attempts.get(), 1);
        Assert.assertEquals(gateway.removals, 0);
    }

    public void refusesNonemptyOrUnreadableBaselineBeforeAttempt() {
        FakeGateway gateway = new FakeGateway();
        gateway.state.put(ITEM, 2);
        AtomicInteger attempts = new AtomicInteger();
        Assert.expectThrows(IllegalStateException.class,
                () -> InvalidProductSlugDiagnosticTest.execute(gateway, () -> {
                    attempts.incrementAndGet();
                    return response();
                }));
        Assert.assertEquals(attempts.get(), 0);
        Assert.assertEquals(gateway.state.get(ITEM), Integer.valueOf(2));
        gateway.failRead = true;
        IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                () -> InvalidProductSlugDiagnosticTest.execute(gateway, () -> {
                    attempts.incrementAndGet();
                    return response();
                }));
        Assert.assertEquals(attempts.get(), 0);
        Assert.assertFalse(failure.getMessage().contains("synthetic-secret"));
        Assert.assertNull(failure.getCause());
    }

    public void detectsMutationRestoresBasketAndReleasesLock() throws Exception {
        FakeGateway gateway = new FakeGateway();
        AtomicInteger attempts = new AtomicInteger();
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> InvalidProductSlugDiagnosticTest.execute(gateway, () -> {
                    attempts.incrementAndGet();
                    gateway.state.put(ITEM, 1);
                    return response();
                }));
        Assert.assertEquals(attempts.get(), 1);
        Assert.assertTrue(failure.getMessage().contains("changed the basket"));
        Assert.assertFalse(failure.getMessage().contains(ITEM.slug()));
        Assert.assertTrue(gateway.state.isEmpty());
        Assert.assertEquals(gateway.removals, 1);

        CompletableFuture.runAsync(() -> InvalidProductSlugDiagnosticTest.execute(new FakeGateway(),
                InvalidSlugDiagnosticSafetyTest::response)).get(5, TimeUnit.SECONDS);
    }

    public void transportFailureStillRestoresWithoutRetryOrLeakingDetails() {
        FakeGateway gateway = new FakeGateway();
        AtomicInteger attempts = new AtomicInteger();
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> InvalidProductSlugDiagnosticTest.execute(gateway, () -> {
                    attempts.incrementAndGet();
                    gateway.state.put(ITEM, 1);
                    throw new IllegalStateException("synthetic-secret");
                }));
        Assert.assertEquals(attempts.get(), 1);
        Assert.assertTrue(gateway.state.isEmpty());
        Assert.assertFalse(failure.getMessage().contains("synthetic-secret"));
        Assert.assertNull(failure.getCause());
    }

    public void cleanupFailureFailsDiagnosticWithoutLeakingDetails() {
        FakeGateway gateway = new FakeGateway();
        gateway.failRemove = true;
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> InvalidProductSlugDiagnosticTest.execute(gateway, () -> {
                    gateway.state.put(ITEM, 1);
                    return response();
                }));
        Assert.assertTrue(failure.getMessage().contains("cleanup failed"));
        Assert.assertFalse(failure.getMessage().contains("synthetic-secret"));
        Assert.assertFalse(failure.getMessage().contains(ITEM.slug()));
    }

    public void observationOnlyContainsAllowlistedMetadata() {
        String summary = InvalidProductSlugDiagnosticTest.observation(response());
        Assert.assertEquals(summary,
                "[INVALID SLUG OBSERVATION] status=418, Content-Type=application/json, errorFields=[detail, error]");
        String unsafeHeader = InvalidProductSlugDiagnosticTest.observation(new ResponseBuilder()
                .setStatusCode(502).setContentType("synthetic-secret").setBody("synthetic-secret").build());
        Assert.assertEquals(unsafeHeader,
                "[INVALID SLUG OBSERVATION] status=502, Content-Type=other, errorFields=unavailable");
        String malformed = InvalidProductSlugDiagnosticTest.observation(new ResponseBuilder()
                .setStatusCode(502).setContentType("application/json").setBody("synthetic-secret").build());
        Assert.assertFalse(malformed.contains("synthetic-secret"));
    }

    private static Response response() {

        return new ResponseBuilder().setStatusCode(418).setContentType("application/json; charset=UTF-8")
                .setBody("{\"detail\":\"synthetic-secret\",\"error\":null,\"synthetic-private-field\":1}").build();
    }

    private static final class FakeGateway implements BasketMutationGateway {
        private final Map<BasketItemKey, Integer> state = new LinkedHashMap<>();
        private int removals;
        private boolean failRead;
        private boolean failRemove;

        @Override
        public BasketSnapshot readSnapshot() {
            if (failRead) {
                throw new IllegalStateException("synthetic-secret");
            }
            return BasketSnapshot.from(state);
        }

        @Override
        public void remove(BasketItemKey key) {
            if (failRemove) {
                throw new IllegalStateException("synthetic-secret");
            }
            removals++;
            state.remove(key);
        }

        @Override
        public void add(BasketItemKey key, int quantity) {
            throw new AssertionError("Diagnostic must not add baseline products");
        }

        @Override
        public void update(BasketItemKey key, int quantity) {
            throw new AssertionError("Diagnostic must not update baseline products");
        }
    }
}
