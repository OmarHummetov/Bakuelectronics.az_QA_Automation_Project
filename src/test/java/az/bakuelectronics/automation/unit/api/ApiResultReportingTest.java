package az.bakuelectronics.automation.support.api;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

public class ApiResultReportingTest {

    @DataProvider
    public Object[][] responseTimes() {
        return new Object[][] {{4999L, true}, {5000L, false}, {5001L, false}};
    }

    @Test(groups = {"unit", "read-only"}, dataProvider = "responseTimes")
    public void enforcesStrictFiveSecondResponseTimeLimit(long milliseconds, boolean accepted) {
        Response response = (Response) Proxy.newProxyInstance(Response.class.getClassLoader(),
                new Class<?>[] {Response.class}, (proxy, method, args) -> {
                    if (method.getName().equals("time") && method.getParameterCount() == 0) {
                        return milliseconds;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        if (accepted) {
            ApiTestReporter.verifyResponseTime(response);
        } else {
            Assert.expectThrows(AssertionError.class,
                    () -> ApiTestReporter.verifyResponseTime(response));
        }
    }

    @Test
    public void reportsPassOnlyAfterInvocationIsFinalized() {
        String identity = ApiTestReporter.newInvocationIdentity("pass-test");
        int[] before = ApiTestReporter.outcomeCounts();

        ApiTestReporter.run(identity, "Pass display", () -> { });
        assertCounts(before, ApiTestReporter.outcomeCounts());

        ApiTestReporter.finish(identity);

        assertCounts(new int[] {
                before[0] + 1, before[1] + 1, before[2]
        }, ApiTestReporter.outcomeCounts());
    }

    @Test
    public void reportsTestFailureExactlyOnce() {
        String identity = ApiTestReporter.newInvocationIdentity("test-failure");
        int[] before = ApiTestReporter.outcomeCounts();
        AssertionError original = new AssertionError("test failed");

        Assert.expectThrows(AssertionError.class,
                () -> ApiTestReporter.run(identity, "Fail display", () -> { throw original; }));
        ApiTestReporter.finish(identity);
        ApiTestReporter.finish(identity);

        assertCounts(new int[] {
                before[0] + 1, before[1], before[2] + 1
        }, ApiTestReporter.outcomeCounts());
    }

    @Test
    public void cleanupFailureReplacesProvisionalPassWithoutAddingAnotherResult() {
        String identity = ApiTestReporter.newInvocationIdentity("cleanup-failure");
        int[] before = ApiTestReporter.outcomeCounts();

        ApiTestReporter.run(identity, "Cleanup display", () -> { });
        ApiTestReporter.recordPostTestFailure(identity, "Cleanup display");
        ApiTestReporter.finish(identity);

        assertCounts(new int[] {
                before[0] + 1, before[1], before[2] + 1
        }, ApiTestReporter.outcomeCounts());
    }

    @Test
    public void duplicateCleanupFailureDoesNotDuplicateReportingOrCounts() {
        String identity = ApiTestReporter.newInvocationIdentity("duplicate-cleanup");
        int[] before = ApiTestReporter.outcomeCounts();

        ApiTestReporter.run(identity, "Duplicate display", () -> { });
        ApiTestReporter.recordPostTestFailure(identity, "Duplicate display");
        ApiTestReporter.recordPostTestFailure(identity, "Duplicate display");
        ApiTestReporter.finish(identity);
        ApiTestReporter.recordPostTestFailure(identity, "Duplicate display");
        ApiTestReporter.finish(identity);

        assertCounts(new int[] {
                before[0] + 1, before[1], before[2] + 1
        }, ApiTestReporter.outcomeCounts());
    }

    private static void assertCounts(int[] expected, int[] actual) {
        Assert.assertEquals(actual[0], expected[0], "total outcomes");
        Assert.assertEquals(actual[1], expected[1], "PASS outcomes");
        Assert.assertEquals(actual[2], expected[2], "FAIL outcomes");
    }
}
