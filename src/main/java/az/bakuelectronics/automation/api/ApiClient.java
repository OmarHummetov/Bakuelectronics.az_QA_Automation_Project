package az.bakuelectronics.automation.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;


public class ApiClient {

    private static volatile String cachedToken;

    private final RequestSpecification requestSpecification;
    private final boolean authenticated;

    public ApiClient() {
        this(true);
    }

    public static ApiClient publicClient() {
        return new ApiClient(false);
    }

    private ApiClient(boolean authenticated) {
        this.authenticated = authenticated;
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
                .setBaseUri(ApiConfig.API_BASE_URL)
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .addHeader("lang", "az");
        requestSpecification = requestSpecBuilder.build();
    }

    private static String getCachedToken() {
        String token = cachedToken;
        if (token != null) {
            return token;
        }

        synchronized (ApiClient.class) {
            if (cachedToken == null) {
                try {
                    String authenticatedToken = ApiAuthProvider.getToken();
                    if (authenticatedToken == null || authenticatedToken.isBlank()) {
                        throw new IllegalStateException("API authentication returned no token.");
                    }
                    cachedToken = authenticatedToken;
                } catch (RuntimeException exception) {
                    throw new IllegalStateException("API authentication failed.", exception);
                }
            }
            return cachedToken;
        }
    }

    public Response get(String endpoint) {
        return request()
                .when()
                .get(endpoint);
    }

    public Response get(String endpoint, Map<String, ?> queryParameters) {
        return request()
                .queryParams(queryParameters)
                .when()
                .get(endpoint);
    }

    public Response post(String endpoint, Object body) {
        return request()
                .body(body)
                .when()
                .post(endpoint);
    }

    public Response put(String endpoint, Object body) {
        return request()
                .body(body)
                .when()
                .put(endpoint);
    }

    public Response patch(String endpoint, Object body) {
        return request()
                .body(body)
                .when()
                .patch(endpoint);
    }

    public Response delete(String endpoint, Map<String, ?> queryParameters) {
        return request()
                .queryParams(queryParameters)
                .when()
                .delete(endpoint);
    }

    private RequestSpecification request() {
        var request = given().spec(requestSpecification);
        if (authenticated) {
            request.header("Authorization", "Bearer " + getCachedToken());
        }
        return request;
    }
}
