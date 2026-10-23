package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import az.bakuelectronics.automation.support.api.ApiClientBasketMutationGateway;
import az.bakuelectronics.automation.support.api.BasketItemKey;
import az.bakuelectronics.automation.support.api.BasketMutationGateway;
import az.bakuelectronics.automation.support.api.BasketSnapshot;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


public class InvalidProductSlugDiagnosticTest {
    private static final AtomicBoolean ATTEMPTED = new AtomicBoolean();

    @Test(groups = {"api", "authenticated-api", "api-mutation", "manual-auth", "no-retry"},
            retryAnalyzer = NoRetryAnalyzer.class)
    public void observeInvalidSlugOnceOnIsolatedAccount() {
        requireOptIn(Boolean.getBoolean("invalid.slug.diagnostic.approved"),
                System.getenv("BAKU_INVALID_SLUG_ACCOUNT"), System.getenv("BAKU_INVALID_SLUG_TOKEN"));
        if (!ATTEMPTED.compareAndSet(false, true)) {
            throw new SkipException("Diagnostic already attempted in this JVM; no automatic repeat permitted.");
        }
        String token = System.getenv("BAKU_INVALID_SLUG_TOKEN");

        ApiClient client = new ApiClient(ApiClient.AuthMode.PUBLIC,
                () -> { throw new IllegalStateException("Normal authentication is forbidden."); },
                (request, method, endpoint, query, body, headers) -> {
                    try {
                        request.noFilters().config(transportConfig()).redirects().follow(false);
                        request.header("Authorization", "Bearer " + token);
                        if (!query.isEmpty()) {
                            request.queryParams(query);
                        }
                        if (body != null) {
                            request.body(body);
                        }
                        return request.when().request(method, endpoint);
                    } catch (RuntimeException ignored) {
                        throw new IllegalStateException("Diagnostic transport failed; details suppressed.");
                    }
                });
        execute(new ApiClientBasketMutationGateway(client), () -> client.post("/addbasket", Map.of(
                "product_slug", "__api-test-nonexistent-product-slug__", "quantity", 1)));
    }

    static void requireOptIn(boolean approved, String account, String token) {
        if (!approved || !"isolated-test-account".equals(account) || token == null || token.isBlank()) {
            throw new SkipException("Diagnostic requires explicit approval, isolated test account attestation"
                    + " and a dedicated token. Personal accounts are forbidden.");
        }
    }

    @SuppressWarnings("deprecation")
    private static RestAssuredConfig transportConfig() {
        return RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", 15000)
                .setParam("http.socket.timeout", 15000)
                .httpClientFactory(() -> {
                    DefaultHttpClient http = new DefaultHttpClient();
                    http.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
                    return http;
                }));
    }

    static void execute(BasketMutationGateway gateway, Supplier<Response> attempt) {
        BasketTestSupport.BasketRestoreSession session = BasketTestSupport.beginLocked(
                new PrivateGateway(gateway), "Invalid slug diagnostic");
        try {
            try {
                System.out.println(observation(attempt.get()));
            } catch (RuntimeException ignored) {
                throw new AssertionError("Diagnostic observation failed; details suppressed. Do not retry.");
            }
            try {
                session.verifyUnchanged();
            } catch (AssertionError ignored) {
                throw new AssertionError("Invalid slug request changed the basket; cleanup required.");
            } catch (RuntimeException ignored) {
                throw new AssertionError("Basket comparison unavailable; cleanup required.");
            }
        } finally {
            try {
                if (!session.restoreAllChanges().success()) {
                    throw new AssertionError("Diagnostic cleanup failed; inspect isolated account manually.");
                }
            } finally {
                BasketTestSupport.unlock();
            }
        }
    }

    static String observation(Response response) {
        String raw = response.getHeader("Content-Type");
        String mediaType = raw == null ? "missing" : raw.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!List.of("application/json", "application/problem+json", "text/html", "text/plain", "missing")
                .contains(mediaType)) {
            mediaType = "other";
        }
        String fields = "unavailable";
        if (mediaType.equals("application/json") || mediaType.equals("application/problem+json")) {
            try {
                Object root = response.jsonPath().get("$");
                if (root instanceof Map<?, ?> map) {
                    fields = List.of("detail", "message", "error").stream()
                            .filter(map::containsKey).toList().toString();
                }
            } catch (RuntimeException ignored) {

            }
        }
        return "[INVALID SLUG OBSERVATION] status=" + response.statusCode()
                + ", Content-Type=" + mediaType + ", errorFields=" + fields;
    }


    private static final class PrivateGateway implements BasketMutationGateway {
        private final BasketMutationGateway delegate;
        private final Map<BasketItemKey, BasketItemKey> identities = new LinkedHashMap<>();
        private boolean initial = true;

        private PrivateGateway(BasketMutationGateway delegate) {
            this.delegate = delegate;
        }

        @Override
        public BasketSnapshot readSnapshot() {
            try {
                BasketSnapshot snapshot = delegate.readSnapshot();
                if (initial && !snapshot.quantities().isEmpty()) {
                    throw new IllegalStateException("Nonempty baseline");
                }
                initial = false;
                Map<BasketItemKey, Integer> safe = new LinkedHashMap<>();
                snapshot.quantities().forEach((key, quantity) -> {
                    BasketItemKey alias = identities.computeIfAbsent(key, ignored ->
                            new BasketItemKey(null, "diagnostic-item-" + identities.size(), null, null));
                    safe.put(alias, quantity);
                });
                return BasketSnapshot.from(safe);
            } catch (RuntimeException ignored) {
                throw new IllegalStateException("Diagnostic requires a readable, initially empty isolated basket.");
            }
        }

        @Override
        public void remove(BasketItemKey alias) {
            try {
                BasketItemKey actual = identities.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(alias)).findFirst().orElseThrow().getKey();
                delegate.remove(actual);
            } catch (RuntimeException ignored) {
                throw new IllegalStateException("Diagnostic cleanup removal failed; details suppressed.");
            }
        }

        @Override
        public void add(BasketItemKey key, int quantity) {
            throw new IllegalStateException("Diagnostic cannot add baseline products.");
        }

        @Override
        public void update(BasketItemKey key, int quantity) {
            throw new IllegalStateException("Diagnostic cannot update baseline products.");
        }
    }
}
