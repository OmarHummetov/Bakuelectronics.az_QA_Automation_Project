package az.bakuelectronics.automation.api;

import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.internal.TestResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class InvalidProductSlugSafetyTest {
    @Test
    public void unchangedNonemptyBasketPreservesResponseChecksWithoutInventingStatus() throws Exception {
        Fixture fixture = new Fixture();
        ITestResult result = run(fixture);
        Assert.assertNull(result.getThrowable());
        Assert.assertEquals(fixture.state, Map.of("unrelated", 3));
        Assert.assertEquals(fixture.posts, 1);
        Assert.assertEquals(fixture.readsBeforePost, 2, "Capture and verify baseline before POST.");
        Assert.assertTrue(fixture.cleanupMutations.isEmpty());
        assertLockReleased();
    }

    @Test
    public void unexpectedChangesToUnrelatedAndNewLinesFailAndRestoreBaseline() throws Exception {
        Fixture fixture = new Fixture();
        fixture.changeBasket = true;
        ITestResult result = run(fixture);
        Assert.assertTrue(result.getThrowable() instanceof AssertionError);
        Assert.assertTrue(result.getThrowable().getMessage().contains("changed the basket"));
        Assert.assertEquals(fixture.state, Map.of("unrelated", 3));
        Assert.assertEquals(fixture.cleanupMutations.size(), 2);
        assertLockReleased();
    }

    @Test
    public void ambiguousTransportFailureRestoresAndPreservesOriginalException() throws Exception {
        Fixture fixture = new Fixture();
        fixture.changeBasket = true;
        fixture.postFailure = new IllegalStateException("Synthetic ambiguous POST");
        ITestResult result = run(fixture);
        Assert.assertSame(result.getThrowable(), fixture.postFailure);
        Assert.assertEquals(fixture.state, Map.of("unrelated", 3));
        Assert.assertEquals(fixture.posts, 1);
        assertLockReleased();
    }

    @Test
    public void responseAssertionFailureStillReleasesLock() throws Exception {
        Fixture fixture = new Fixture();
        fixture.responseBody = "{\"other\":1}";
        ITestResult result = run(fixture);
        Assert.assertTrue(result.getThrowable() instanceof AssertionError,
                "The original missing-error-message assertion must still fail.");
        Assert.assertEquals(fixture.state, Map.of("unrelated", 3));
        assertLockReleased();
    }

    @Test
    public void cleanupFailureIsSuppressedOnOriginalFailureAndReleasesLock() throws Exception {
        Fixture fixture = new Fixture();
        fixture.changeBasket = true;
        fixture.failCleanup = true;
        fixture.postFailure = new IllegalStateException("Synthetic ambiguous POST");
        ITestResult result = run(fixture);
        Assert.assertSame(result.getThrowable(), fixture.postFailure);
        Assert.assertEquals(result.getThrowable().getSuppressed().length, 1);
        Assert.assertEquals(result.getStatus(), ITestResult.FAILURE);
        assertLockReleased();
    }

    @Test
    public void unreadableBaselineRefusesPostAndReleasesLock() throws Exception {
        Fixture fixture = new Fixture();
        fixture.failRead = true;
        ITestResult result = run(fixture);
        Assert.assertTrue(result.getThrowable() instanceof IllegalStateException);
        Assert.assertEquals(fixture.posts, 0);
        Assert.assertTrue(fixture.cleanupMutations.isEmpty());
        assertLockReleased();
    }

    private static ITestResult run(Fixture fixture) {
        BasketInvalidSlugResponseTest test = new BasketInvalidSlugResponseTest(fixture.client());
        ITestResult result = TestResult.newEmptyTestResult();
        result.setStatus(ITestResult.SUCCESS);
        try {
            test.lockBasketMutation();
            test.shouldRejectNonexistentProductSlug();
        } catch (RuntimeException | AssertionError failure) {
            result.setThrowable(failure);
            result.setStatus(ITestResult.FAILURE);
        } finally {
            test.cleanupBasket(result);
        }
        return result;
    }

    private static void assertLockReleased() throws Exception {
        CompletableFuture.runAsync(() -> {
            az.bakuelectronics.automation.support.api.BasketTestSupport.lock();
            az.bakuelectronics.automation.support.api.BasketTestSupport.unlock();
        }).get(5, TimeUnit.SECONDS);
    }

    private static final class Fixture {
        private final Map<String, Integer> state = new LinkedHashMap<>(Map.of("unrelated", 3));
        private final List<String> cleanupMutations = new ArrayList<>();
        private boolean changeBasket;
        private boolean failRead;
        private boolean failCleanup;
        private RuntimeException postFailure;
        private int reads;
        private int readsBeforePost;
        private int posts;
        private String responseBody = "{\"detail\":\"Synthetic error\"}";

        private ApiClient client() {
            return new ApiClient(ApiClient.AuthMode.PUBLIC,
                    () -> { throw new AssertionError("Must not read credentials"); },
                    (request, method, endpoint, query, body, headers) -> {
                        if ("GET".equals(method)) {
                            if (failRead) throw new IllegalStateException("Synthetic snapshot unavailable");
                            reads++;
                            String products = state.entrySet().stream()
                                    .map(entry -> "{\"slug\":\"" + entry.getKey()
                                            + "\",\"quantity\":" + entry.getValue() + "}")
                                    .collect(java.util.stream.Collectors.joining(","));
                            return json(200, "{\"products\":[" + products + "]}");
                        }
                        if ("POST".equals(method) && "/addbasket".equals(endpoint)) {
                            posts++;
                            readsBeforePost = reads;
                            if (changeBasket) {
                                state.put("unrelated", 8);
                                state.put("unexpected", 1);
                            }
                            if (postFailure != null) throw postFailure;

                            return json(418, responseBody);
                        }
                        cleanupMutations.add(method);
                        if (failCleanup) throw new IllegalStateException("Synthetic restore failure");
                        if ("PUT".equals(method)) {
                            Map<?, ?> payload = (Map<?, ?>) body;
                            state.put((String) payload.get("product_slug"), (Integer) payload.get("quantity"));
                        } else if ("DELETE".equals(method)) {
                            state.remove(query.get("slug"));
                        } else {
                            throw new AssertionError("Unexpected cleanup request: " + method);
                        }
                        return json(200, "{}");
                    });
        }

        private static Response json(int status, String body) {
            return new ResponseBuilder().setStatusCode(status).setContentType("application/json")
                    .setBody(body).build();
        }
    }
}
