package az.bakuelectronics.automation.support.api;

public final class BasketTestCleanup {

    private BasketTestCleanup() {
    }

    public static void lock() {
        BasketTestSupport.lock();
    }

    public static void unlock() {
        BasketTestSupport.unlock();
    }

}
