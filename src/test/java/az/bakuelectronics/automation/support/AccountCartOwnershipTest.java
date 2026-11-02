package az.bakuelectronics.automation.support;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class AccountCartOwnershipTest {
    @Test public void accountSnapshotReadsAllRowsAndWaitsForStableArithmetic() {
        var polls = new java.util.concurrent.atomic.AtomicInteger();
        var driver = (org.openqa.selenium.WebDriver) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{org.openqa.selenium.WebDriver.class,
                        org.openqa.selenium.JavascriptExecutor.class}, (proxy, method, args) -> {
                    Assert.assertEquals(method.getName(), "executeScript");
                    int count = polls.incrementAndGet();
                    return java.util.List.of(java.util.List.of(
                            java.util.List.of("existing", "Synthetic original", "12.00", "10.00", "3", "3"),
                            java.util.List.of("a", "Synthetic owned", "5.00", "4.00", "2", "2")),
                            "8.00", count == 1 ? "34.00" : "38.00");
                });
        var snapshot = new az.bakuelectronics.automation.pages.CartPage(driver, java.time.Duration.ofSeconds(3))
                .waitForAccountSnapshot();
        Assert.assertEquals(polls.get(), 3);
        Assert.assertEquals(snapshot.rows().size(), 2);
        Assert.assertEquals(snapshot.rows().get("existing").quantity(), 3);
        Assert.assertEquals(snapshot.total(), new java.math.BigDecimal("38.00"));
    }

    @Test public void preexistingRowsCannotBeOwnedOrChanged() {
        var owned = new AccountCartOwnership(Map.of("existing", 3));
        Assert.expectThrows(IllegalStateException.class, () -> owned.register("existing"));
        Assert.expectThrows(IllegalStateException.class, () -> owned.verifySafeRecovery(Map.of("existing", 2)));
        Assert.expectThrows(IllegalStateException.class, () -> owned.verifySafeRecovery(Map.of()));
        Assert.assertTrue(owned.owned().isEmpty());
    }
    @Test public void unknownRowsAndExcessQuantitiesStopRecovery() {
        var owned = new AccountCartOwnership(Map.of("existing", 3));
        owned.register("a");
        Assert.expectThrows(IllegalStateException.class,
                () -> owned.verifySafeRecovery(Map.of("existing", 3, "a", 1, "foreign", 1)));
        Assert.expectThrows(IllegalStateException.class,
                () -> owned.verifySafeRecovery(Map.of("existing", 3, "a", 2)));
        Assert.expectThrows(IllegalStateException.class, () -> owned.register("a"));
    }
    @Test public void ambiguousDispatchCanRecoverOnlyRegisteredProductsAndQuantities() {
        var owned = new AccountCartOwnership(Map.of("existing", 3));
        owned.register("a"); owned.expectTwo("a"); owned.register("b");
        for (var current : java.util.List.of(Map.of("existing", 3),
                Map.of("existing", 3, "a", 1), Map.of("existing", 3, "a", 2, "b", 1)))
            owned.verifySafeRecovery(current);
        Assert.expectThrows(IllegalStateException.class,
                () -> owned.verifySafeRecovery(Map.of("existing", 3, "a", 3)));
        Assert.expectThrows(UnsupportedOperationException.class, () -> owned.owned().clear());
    }
    @Test public void journalIsPrivateDurableAndBlocksASecondRun() throws Exception {
        var folder = java.nio.file.Files.createTempDirectory("account-cart-offline-");
        var path = folder.resolve("ownership.secret");
        try {
            var journal = new PrivateCartJournal(path);
            var owned = new AccountCartOwnership(Map.of("synthetic-existing", 3));
            owned.register("synthetic-a"); owned.expectTwo("synthetic-a");
            journal.save(owned, "synthetic pending quantity");
            String content = java.nio.file.Files.readString(path);
            Assert.assertTrue(content.contains("baseline.synthetic-existing=3"));
            Assert.assertTrue(content.contains("owned.synthetic-a=2"));
            var acl = java.nio.file.Files.getFileAttributeView(path,
                    java.nio.file.attribute.AclFileAttributeView.class).getAcl();
            Assert.assertEquals(acl.size(), 1);
            Assert.expectThrows(IllegalStateException.class, () -> new PrivateCartJournal(path));
            journal.complete();
            Assert.assertFalse(java.nio.file.Files.exists(path));
        } finally { java.nio.file.Files.deleteIfExists(path); java.nio.file.Files.deleteIfExists(folder); }
    }
}
