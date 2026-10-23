package az.bakuelectronics.automation.api;

import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;

public class ApiClientAuthenticationTest {

    @BeforeMethod
    public void clearTokenCacheBeforeTest() {
        ApiClient.clearTokenCache();
    }

    @AfterMethod
    public void resetTokenCache() {
        ApiClient.clearTokenCache();
    }

    @Test(groups = {"unit", "public-api"})
    public void publicRequestDoesNotReadCredentialsOrAddAuthorization() {
        AtomicInteger authCalls = new AtomicInteger();
        RequestCapture capture = new RequestCapture();
        ApiClient client = new ApiClient(ApiClient.AuthMode.PUBLIC,
                () -> { authCalls.incrementAndGet(); return "must-not-be-used"; }, capture::execute);

        client.get("/public");

        Assert.assertEquals(authCalls.get(), 0);
        Assert.assertFalse(capture.headers.containsKey("Authorization"));
    }

    @Test(groups = {"unit", "authenticated-api"})
    public void authenticatedRequestAddsBearerTokenAndCachesIt() {
        AtomicInteger authCalls = new AtomicInteger();
        RequestCapture capture = new RequestCapture();
        ApiClient client = new ApiClient(ApiClient.AuthMode.AUTHENTICATED,
                () -> { authCalls.incrementAndGet(); return "unit-token"; }, capture::execute);

        client.get("/private");
        client.get("/private-again");

        Assert.assertEquals(authCalls.get(), 1);
        Assert.assertEquals(capture.headers.get("Authorization"),
                "Bearer unit-token");
    }

    @Test(groups = {"unit", "authenticated-api"})
    public void authenticationFailureIsNotHidden() {
        ApiClient client = new ApiClient(ApiClient.AuthMode.AUTHENTICATED,
                () -> { throw new IllegalStateException("missing credentials"); },
                new RequestCapture()::execute);

        ApiAuthenticationException failure = Assert.expectThrows(ApiAuthenticationException.class,
                () -> client.get("/private"));
        Assert.assertTrue(failure.getMessage().contains("authentication failed"));
        Assert.assertTrue(failure.getCause().getMessage().contains("missing credentials"));
    }

    @Test(groups = {"unit", "public-api"})
    public void publicAndAuthenticatedClientsHaveIndependentPolicies() {
        AtomicInteger authCalls = new AtomicInteger();
        RequestCapture capture = new RequestCapture();
        ApiClient publicClient = new ApiClient(ApiClient.AuthMode.PUBLIC,
                () -> { authCalls.incrementAndGet(); return "unused"; }, capture::execute);
        ApiClient authenticatedClient = new ApiClient(ApiClient.AuthMode.AUTHENTICATED,
                () -> { authCalls.incrementAndGet(); return "private-token"; }, capture::execute);

        publicClient.get("/public");
        Assert.assertEquals(authCalls.get(), 0);
        authenticatedClient.get("/private");
        Assert.assertEquals(authCalls.get(), 1);
    }

    @Test(groups = {"unit", "authenticated-api"})
    public void unauthorizedAndForbiddenResponsesRemainDistinguishable() {
        RequestCapture capture = new RequestCapture();
        ApiClient client = new ApiClient(ApiClient.AuthMode.PUBLIC, () -> "unused", capture::execute);

        capture.response = response(401);
        Assert.assertEquals(client.get("/private").statusCode(), 401);
        capture.response = response(403);
        Assert.assertEquals(client.get("/private").statusCode(), 403);
    }

    @Test(groups = "unit")
    public void unauthorizedBasketRejectionAssertionsAccept401And403WithoutReportingErrorValues() {
        for (int status : new int[]{401, 403}) {
            BasketUnauthorizedRequestTest.verifyUnauthorizedResponse(unauthorizedResponse(status,
                    "{\"detail\":\"synthetic-private-error-value\"}"));
        }
    }

    @Test(groups = "unit")
    public void unexpectedlyAcceptedUnauthorizedBasketResponseStillFails() {
        AssertionError failure = Assert.expectThrows(AssertionError.class, () ->
                BasketUnauthorizedRequestTest.verifyUnauthorizedResponse(unauthorizedResponse(200,
                        "{\"products\":[]}")));
        Assert.assertTrue(failure.getMessage().contains("200"));
    }

    @Test(groups = "unit")
    public void unauthorizedRejectionStillRequiresJsonObjectAndErrorMessage() {
        for (String body : new String[]{"[]", "{}", "{\"unexpected\":\"value\"}"}) {
            Assert.expectThrows(AssertionError.class, () ->
                    BasketUnauthorizedRequestTest.verifyUnauthorizedResponse(unauthorizedResponse(401, body)));
        }
    }

    @Test(groups = "unit")
    public void unavailableGuestRecoveryBlocksBeforeAnyTransport() {
        AtomicInteger attempts = new AtomicInteger();
        BasketUnauthorizedRequestTest test = new BasketUnauthorizedRequestTest(() -> {
            attempts.incrementAndGet();
            return unauthorizedResponse(200, "{\"products\":[]}");
        });
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                test::shouldRejectRequestWithoutAuthorization);
        Assert.assertEquals(attempts.get(), 0, "An accepted response must never be risked without guest recovery.");
        Assert.assertTrue(failure.getMessage().contains("blocked before request"));
        Assert.assertTrue(failure.getMessage().contains("same-basket baseline read and recovery contract"));
        Assert.assertNull(failure.getCause(), "The blocker must not expose transport/account details.");
    }

    @Test(groups = "unit")
    public void unavailableGuestRecoveryNeverUsesAnAuthenticatedFallback() {
        AtomicInteger authCalls = new AtomicInteger();
        ApiClient client = new ApiClient(ApiClient.AuthMode.AUTHENTICATED,
                () -> { authCalls.incrementAndGet(); return "offline-cached-token"; }, new RequestCapture()::execute);
        client.get("/offline-cache-setup");
        AtomicInteger requests = new AtomicInteger();
        BasketUnauthorizedRequestTest test = new BasketUnauthorizedRequestTest(() -> {
            requests.incrementAndGet();
            return unauthorizedResponse(401, "{\"detail\":\"unauthorized\"}");
        });
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                test::shouldRejectRequestWithoutAuthorization);
        Assert.assertEquals(requests.get(), 0);
        Assert.assertTrue(failure.getMessage().contains("Authenticated fallback is forbidden"));
        Assert.assertFalse(failure.getMessage().contains("offline-cached-token"));
        client.get("/offline-cache-verification");
        Assert.assertEquals(authCalls.get(), 1);
    }

    @Test(groups = "unit")
    public void unauthorizedMutationRetainsNegativeAuthLabelsAndCannotRetry() throws Exception {
        Test annotation = BasketUnauthorizedRequestTest.class
                .getMethod("shouldRejectRequestWithoutAuthorization").getAnnotation(Test.class);
        Assert.assertEquals(annotation.retryAnalyzer(), NoRetryAnalyzer.class);
        Assert.assertTrue(java.util.Arrays.asList(annotation.groups()).containsAll(
                java.util.List.of("api", "authenticated-api", "negative-auth", "api-mutation", "no-retry")));
    }

    private static Response unauthorizedResponse(int status, String body) {
        return new ResponseBuilder().setStatusCode(status).setContentType("application/json").setBody(body).build();
    }

    private static Response response(int status) {
        return new ResponseBuilder().setStatusCode(status)
                .setContentType("application/json").setBody("{}").build();
    }

    private static final class RequestCapture {
        private RequestSpecification request;
        private Map<String, String> headers = Map.of();
        private Response response = response(200);

        private Response execute(RequestSpecification request, String method, String endpoint,
                                 Map<String, ?> queryParameters, Object body,
                                 Map<String, String> headers) {
            this.request = request;
            this.headers = headers;
            return response;
        }
    }
}
