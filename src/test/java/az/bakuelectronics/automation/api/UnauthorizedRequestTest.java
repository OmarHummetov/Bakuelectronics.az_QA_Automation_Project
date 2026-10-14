package az.bakuelectronics.automation.api;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class UnauthorizedRequestTest {

    private static final String PRODUCT_SLUG =
            "telefon-iphone-17-pro-max-2tb-cosmic-orange-222768";

    @Test(groups = {"api"})
    public void shouldRejectRequestWithoutAuthorization() {
        ApiTestReporter.run("Unauthorized Basket Request", () -> {
            Response response = given()
                    .baseUri(ApiConfig.API_BASE_URL)
                    .contentType("application/json")
                    .accept("application/json")
                    .header("lang", "az")
                    .body(Map.of("product_slug", PRODUCT_SLUG, "quantity", 1))
                    .when()
                    .post("/addbasket");

            ApiTestReporter.verifyStatusOneOf(response, 401, 403);
            ApiTestReporter.verifyJsonResponse(response);
            ApiTestReporter.verifyJsonRoot(response);
            Map<String, Object> responseBody = response.jsonPath().getMap("$");
            Object message = firstPresent(responseBody, "detail", "message", "error");
            ApiTestReporter.verifyValuePresent("Unauthorized error message", message);
            ApiTestReporter.verifyResponseTime(response);
        });
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
