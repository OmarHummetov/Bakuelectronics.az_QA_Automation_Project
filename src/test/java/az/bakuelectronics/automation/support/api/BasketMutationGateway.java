package az.bakuelectronics.automation.support.api;

public interface BasketMutationGateway {
    BasketSnapshot readSnapshot();

    void add(BasketItemKey key, int quantity);

    void update(BasketItemKey key, int quantity);

    void remove(BasketItemKey key);
}
