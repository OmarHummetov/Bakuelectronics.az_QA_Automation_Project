package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;

import io.restassured.response.Response;
import org.testng.annotations.Test;

public class DefaultSearchResultContractTest {

    private final ApiClient apiClient = ApiClient.publicClient();

    @Test(groups = {"api", "public-api", "read-only"})
    public void shouldExposeDefaultSearchResultJsonContract() {
        ApiTestReporter.run("Default Search Result Contract", () -> {
            Response response = apiClient.get("/default-search-result");

            ApiTestReporter.verifyStatus(response, 200);
            ApiTestReporter.verifyJsonResponse(response);
            ApiTestReporter.verifyJsonRoot(response);
            ApiTestReporter.verifyDefaultSearchProduct(response);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
