package az.bakuelectronics.automation.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;


public final class ApiClient {

    public enum AuthMode { PUBLIC, AUTHENTICATED }

    private static volatile String cachedToken;
    private static final ApiRequestExecutor REST_ASSURED_EXECUTOR =
            (request, method, endpoint, queryParameters, body, headers) -> {
                RequestSpecification configured = request;
                if (queryParameters != null && !queryParameters.isEmpty()) {
                    configured = configured.queryParams(queryParameters);
                }
                if (body != null) {
                    configured = configured.body(body);
                }
                return configured.when().request(method, endpoint);
            };

    private final RequestSpecification requestSpecification;
    private final AuthMode authMode;
    private final Supplier<String> tokenSupplier;
    private final ApiRequestExecutor executor;

    public ApiClient() {
        this(AuthMode.AUTHENTICATED);
    }

    public ApiClient(AuthMode authMode) {
        this(authMode, ApiAuthProvider::getToken, REST_ASSURED_EXECUTOR);
    }

    public ApiClient(AuthMode authMode, Supplier<String> tokenSupplier,
                     ApiRequestExecutor executor) {
        this.authMode = Objects.requireNonNull(authMode, "authMode");
        this.tokenSupplier = Objects.requireNonNull(tokenSupplier, "tokenSupplier");
        this.executor = Objects.requireNonNull(executor, "executor");
        requestSpecification = new RequestSpecBuilder()
                .setBaseUri(ApiConfig.API_BASE_URL)
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .addHeader("lang", "az")
                .build();
    }

    public static ApiClient publicClient() {
        return new ApiClient(AuthMode.PUBLIC);
    }

    public static ApiClient authenticatedClient() {
        return new ApiClient(AuthMode.AUTHENTICATED);
    }


    static void clearTokenCache() {
        synchronized (ApiClient.class) {
            cachedToken = null;
        }
    }

    private static String getCachedToken(Supplier<String> tokenSupplier) {
        String token = cachedToken;
        if (token != null) {
            return token;
        }
        synchronized (ApiClient.class) {
            if (cachedToken == null) {
                try {
                    String resolved = tokenSupplier.get();
                    if (resolved == null || resolved.isBlank()) {
                        throw new ApiAuthenticationException("API authentication returned no token.");
                    }
                    cachedToken = resolved;
                } catch (ApiAuthenticationException exception) {
                    throw exception;
                } catch (RuntimeException exception) {
                    throw new ApiAuthenticationException("API authentication failed.", exception);
                }
            }
            return cachedToken;
        }
    }

    public Response get(String endpoint) { return execute("GET", endpoint, Map.of(), null); }
    public Response get(String endpoint, Map<String, ?> queryParameters) {
        return execute("GET", endpoint, queryParameters, null);
    }
    public Response post(String endpoint, Object body) { return execute("POST", endpoint, Map.of(), body); }
    public Response put(String endpoint, Object body) { return execute("PUT", endpoint, Map.of(), body); }
    public Response patch(String endpoint, Object body) { return execute("PATCH", endpoint, Map.of(), body); }
    public Response delete(String endpoint, Map<String, ?> queryParameters) {
        return execute("DELETE", endpoint, queryParameters, null);
    }

    private Response execute(String method, String endpoint, Map<String, ?> queryParameters, Object body) {
        RequestSpecification request = given().spec(requestSpecification);
        Map<String, String> headers = new java.util.LinkedHashMap<>();
        if (authMode == AuthMode.AUTHENTICATED) {
            String authorization = "Bearer " + getCachedToken(tokenSupplier);
            request.header("Authorization", authorization);
            headers.put("Authorization", authorization);
        }
        return executor.execute(request, method, endpoint, queryParameters, body, Map.copyOf(headers));
    }
}
