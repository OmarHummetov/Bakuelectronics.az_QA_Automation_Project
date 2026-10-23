package az.bakuelectronics.automation.support.api;

import az.bakuelectronics.automation.api.ApiClient;

import io.restassured.response.Response;

import java.util.Map;


public final class ApiClientBasketMutationGateway implements BasketMutationGateway {
    private final ApiClient apiClient;
    private final String readEndpoint;

    public ApiClientBasketMutationGateway(ApiClient apiClient) {
        this(apiClient, System.getProperty("basket.read.endpoint", "/getBasket").trim());
    }

    ApiClientBasketMutationGateway(ApiClient apiClient, String readEndpoint) {
        this.apiClient = apiClient;
        this.readEndpoint = readEndpoint;
    }

    @Override
    public BasketSnapshot readSnapshot() {
        if (readEndpoint.isBlank()) {
            throw new IllegalStateException("Basket snapshot endpoint is not configured. "
                    + "Set -Dbasket.read.endpoint to the repository-confirmed basket GET endpoint.");
        }
        return BasketSnapshot.fromResponse(apiClient.get(readEndpoint));
    }

    @Override
    public void add(BasketItemKey key, int quantity) {
        requireOk(apiClient.post("/addbasket", Map.of(
                "product_slug", key.mutationSlug(), "quantity", quantity)), "add", key);
    }

    @Override
    public void update(BasketItemKey key, int quantity) {
        requireOk(apiClient.put("/updateBasket", Map.of(
                "product_slug", key.mutationSlug(), "quantity", quantity)), "update", key);
    }

    @Override
    public void remove(BasketItemKey key) {
        requireOk(apiClient.delete("/basket-remove-product", Map.of("slug", key.mutationSlug())),
                "remove", key);
    }

    private static void requireOk(Response response, String operation, BasketItemKey key) {
        if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Basket " + operation + " failed for " + key
                    + ": HTTP " + (response == null ? "no response" : response.statusCode()));
        }
    }
}
