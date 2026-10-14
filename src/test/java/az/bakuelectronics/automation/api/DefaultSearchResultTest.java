package az.bakuelectronics.automation.api;

import io.restassured.response.Response;
import org.testng.annotations.Test;

public class DefaultSearchResultTest {

    private final ApiClient apiClient = ApiClient.publicClient();

    @Test(groups = "api")
    public void shouldReturnDefaultSearchResult() {
        ApiTestReporter.run("Default Search Result", () -> {
            Response response = apiClient.get("/default-search-result");

            ApiTestReporter.verifySuccessfulStatus(response);
            ApiTestReporter.verifyJsonResponse(response);
            ApiTestReporter.verifyJsonRoot(response);
            // Smoke responsibility: endpoint availability and presence of a product object.
            // Detailed contract checks belong to DefaultSearchResultContractTest.
            ApiTestReporter.verifyDefaultSearchProductPresent(response);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
