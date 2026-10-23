package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

public class BasketUnauthorizedRequestTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";
    private final Supplier<Response> request;

    public BasketUnauthorizedRequestTest() {

        this(() -> given()
                .baseUri(ApiConfig.API_BASE_URL)
                .contentType("application/json")
                .accept("application/json")
                .header("lang", "az")
                .body(Map.of("product_slug", PRODUCT_SLUG, "quantity", 1))
                .when().post("/addbasket"));
    }


    BasketUnauthorizedRequestTest(Supplier<Response> request) {
        this.request = Objects.requireNonNull(request);
    }

    @Test(groups = {"api", "authenticated-api", "negative-auth", "api-mutation", "no-retry"},
            retryAnalyzer = NoRetryAnalyzer.class)
    public void shouldRejectRequestWithoutAuthorization() {
        ApiTestReporter.run("Unauthorized Basket Request", () -> {
            requireGuestRecoveryContract();
            verifyUnauthorizedResponse(request.get());
        });
    }

    private static void requireGuestRecoveryContract() {




        throw new AssertionError("Unauthorized basket POST blocked before request: no verified isolated guest "
                + "identity with same-basket baseline read and recovery contract. Authenticated fallback is forbidden.");
    }


    static void verifyUnauthorizedResponse(Response response) {
        ApiTestReporter.verifyStatusOneOf(response, 401, 403);
        ApiTestReporter.verifyJsonResponse(response);
        ApiTestReporter.verifyJsonRoot(response);
        Map<String, Object> responseBody = response.jsonPath().getMap("$");
        Object message = firstPresent(responseBody, "detail", "message", "error");

        ApiTestReporter.verifyValuePresent("Unauthorized error message", message == null ? null : "<present>");
        ApiTestReporter.verifyResponseTime(response);
    }

    private static Object firstPresent(Map<String, Object> responseBody, String... fields) {
        if (responseBody == null) {
            return null;
        }
        for (String field : fields) {
            if (responseBody.containsKey(field)) {
                return responseBody.get(field);
            }
        }
        return null;
    }
}
