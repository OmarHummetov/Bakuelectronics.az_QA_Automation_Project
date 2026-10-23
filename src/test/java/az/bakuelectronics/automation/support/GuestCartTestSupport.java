package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.pages.CartPage;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class GuestCartTestSupport {
    private GuestCartTestSupport() {
    }

    public static void withEmptyCart(CartPage cart, String productSlug, Runnable scenario) {
        cart.openCart();
        cart.waitForCartPage();

        Assert.assertTrue(cart.isCartEmpty(), "The isolated guest cart must initially be empty.");
        Throwable testFailure = null;
        try {

            scenario.run();
        } catch (RuntimeException | AssertionError failure) {
            testFailure = failure;
            attachState(cart, productSlug, "Guest cart state before finally cleanup");
            throw failure;
        } finally {
            try {
                cart.openCart();
                cart.waitForCartPage();
                boolean productPresent;
                try {
                    productPresent = cart.isProductVisible(productSlug);
                } catch (TimeoutException absent) {

                    productPresent = false;
                }
                if (productPresent) {
                    cart.removeProduct(productSlug);
                }
                Assert.assertTrue(cart.isCartEmpty(),
                        "Guest cart cleanup must restore the initial empty state.");
            } catch (RuntimeException | AssertionError failure) {
                attachState(cart, productSlug, "Guest cart cleanup failure state");
                AssertionError cleanupFailure = new AssertionError("Guest cart cleanup failed", failure);
                if (testFailure == null) {
                    throw cleanupFailure;
                }
                testFailure.addSuppressed(cleanupFailure);
            }
        }
    }

    private static void attachState(CartPage cart, String slug, String title) {
        if (SensitiveTestReporting.active()) return;
        try {
            String snapshot = cart.failureState(slug);
            if (Allure.getLifecycle().getCurrentExecutableKey().isPresent()) {
                Allure.getLifecycle().addAttachment(title, "text/plain",
                        new ByteArrayInputStream(snapshot.getBytes(StandardCharsets.UTF_8)),
                        AttachmentOptions.withFileExtension(".txt"));
            }
        } catch (RuntimeException ignored) {

        }
    }
}
