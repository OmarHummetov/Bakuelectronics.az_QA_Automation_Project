package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.pages.CartPage;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import io.qameta.allure.Allure;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Empty guest baseline only. Own before dispatch, including ambiguous add outcomes. */
public final class OwnedGuestCart {
    private final CartPage cart;
    private final Set<String> owned = new LinkedHashSet<>();
    private boolean active = true;

    private OwnedGuestCart(CartPage cart) { this.cart = cart; }

    public static void withEmptyCart(CartPage cart, Consumer<OwnedGuestCart> scenario) {
        BasketTestSupport.lock();
        try {
            cart.openCart();
            if (!cart.waitForInventory().isEmpty())
                throw new IllegalStateException("Refusing to modify a nonempty guest baseline");
            OwnedGuestCart session = new OwnedGuestCart(cart);
            Throwable primary = null;
            try { scenario.accept(session); }
            catch (RuntimeException | Error failure) { primary = failure; throw failure; }
            finally {
                session.active = false;
                try { session.restore(); }
                catch (RuntimeException | Error failure) {
                    if (primary != null) primary.addSuppressed(failure);
                    else throw failure;
                }
            }
        } finally { BasketTestSupport.unlock(); }
    }

    public void add(String slug, Runnable singleAdd) {
        if (!active || !slug.matches("[a-zA-Z0-9_-]+") || owned.contains(slug))
            throw new IllegalStateException("Invalid, repeated or expired ownership registration");
        cart.openCart();
        Map<String, Integer> before = cart.waitForInventory();
        if (before.containsKey(slug) || !owned.containsAll(before.keySet()))
            throw new IllegalStateException("Unexpected product before addition");
        owned.add(slug);
        singleAdd.run();
        cart.openCart();
        Map<String, Integer> expected = new java.util.HashMap<>(before);
        expected.put(slug, 1);
        if (!cart.waitForInventory().equals(expected))
            throw new AssertionError("Single add did not preserve other rows and create exactly quantity one");
    }

    private void restore() {
        cart.openCart();
        Throwable failures = null;
        for (String slug : owned) {
            try {
                if (cart.waitForInventory().containsKey(slug)) cart.removeOwnedProduct(slug);
            } catch (RuntimeException | Error failure) {
                if (failures == null) failures = new AssertionError("Owned guest cart cleanup failed");
                failures.addSuppressed(failure);
            }
        }
        try {
            if (!cart.waitForInventory().isEmpty())
                throw new AssertionError("Original empty guest cart was not restored; unrelated products were not removed");
            Allure.attachment("Phase 4 cleanup verification", "text/plain", new java.io.ByteArrayInputStream(
                    "Original empty guest inventory restored and verified".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    io.qameta.allure.AttachmentOptions.withFileExtension(".txt"));
        } catch (RuntimeException | Error failure) {
            if (failures == null) failures = new AssertionError("Guest baseline restoration was not verified");
            failures.addSuppressed(failure);
        }
        if (failures != null) throw (AssertionError) failures;
    }
}
