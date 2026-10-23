package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.support.api.BasketSnapshot;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;


public class BasketSnapshotReadTest {
    private final ApiClient apiClient = new ApiClient();

    @Test(groups = {"api", "authenticated-api", "read-only"})
    public void shouldReadAuthenticatedBasketSnapshotWithoutMutation() {
        ApiTestReporter.run("Read-only Basket Snapshot", () -> {
            Response response = apiClient.get("/getBasket");

            ApiTestReporter.verifyStatus(response, 200);
            ApiTestReporter.verifyJsonResponse(response);
            Object root = ApiTestReporter.verifyJsonRoot(response);
            Assert.assertTrue(root instanceof Map<?, ?>,
                    "GET /getBasket response root must be an object");
            ApiTestReporter.verifyFieldType(response, "products", List.class);
            ApiTestReporter.verifyResponseTime(response);

            printShape(response.jsonPath().get("$"), "$", 0);

            BasketSnapshot snapshot = BasketSnapshot.fromResponse(response);
            Assert.assertNotNull(snapshot, "GET /getBasket must produce a basket snapshot");
        });
    }

    private static void printShape(Object value, String path, int depth) {
        if (depth > 4) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childPath = path.equals("$") ? "$.": path + ".";
                childPath += String.valueOf(entry.getKey());
                Object child = entry.getValue();
                System.out.println("[BASKET SHAPE] " + childPath + " -> " + typeName(child));
                printShape(child, childPath, depth + 1);
            }
        } else if (value instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                Object child = list.get(index);
                String childPath = path + "[" + index + "]";
                System.out.println("[BASKET SHAPE] " + childPath + " -> " + typeName(child));
                printShape(child, childPath, depth + 1);
            }
        }
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
