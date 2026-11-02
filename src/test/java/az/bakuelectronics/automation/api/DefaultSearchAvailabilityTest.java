package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

public class DefaultSearchAvailabilityTest {

    private final ApiClient apiClient = ApiClient.publicClient();

    @Test(groups = {"api", "public-api", "read-only"},
            description = "Verify endpoint availability and JSON object format independently of suggestion data")
    public void shouldReturnDefaultSearchResult() {
        ApiTestReporter.run("Default Search Availability", () -> {
            Response response = apiClient.get("/default-search-result");

            ApiTestReporter.verifyStatus(response, 200);
            ApiTestReporter.verifyJsonResponse(response);
            // Populated product search is checked separately by FastSearchExpandedContractTest.
            ApiTestReporter.verifyFieldType(response, "$", Map.class);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
