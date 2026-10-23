package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;

import io.restassured.response.Response;
import org.testng.annotations.Test;

public class DefaultSearchAvailabilityTest {

    private final ApiClient apiClient = ApiClient.publicClient();

    @Test(groups = {"api", "public-api", "read-only"})
    public void shouldReturnDefaultSearchResult() {
        ApiTestReporter.run("Default Search Result", () -> {
            Response response = apiClient.get("/default-search-result");

            ApiTestReporter.verifySuccessfulStatus(response);
            ApiTestReporter.verifyJsonResponse(response);
            ApiTestReporter.verifyJsonRoot(response);


            ApiTestReporter.verifyDefaultSearchProductPresent(response);
            ApiTestReporter.verifyResponseTime(response);
        });
    }
}
