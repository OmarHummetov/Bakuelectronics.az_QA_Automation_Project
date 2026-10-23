package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.ui.AccountLoginTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

public class AuthenticatedUiApprovalTest {
    @Test
    public void personalOptInIsExclusiveExactAndRestricted() {
        String[] keys = {"test", "authenticated.ui.approved", "authenticated.ui.dedicatedAccount",
                "authenticated.ui.personalAccountApproved", "groups", "surefire.rerunFailingTestsCount"};
        Map<String, String> previous = new HashMap<>();
        for (String key : keys) { previous.put(key, System.getProperty(key)); System.clearProperty(key); }
        try {
            System.setProperty("authenticated.ui.approved", "true");
            System.setProperty("authenticated.ui.personalAccountApproved", "true");
            for (Class<?> type : new Class<?>[]{AccountLoginTest.class}) {
                String method = java.util.Arrays.stream(type.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(Test.class)).findFirst().orElseThrow().getName();
                String exact = type.getSimpleName() + "#" + method;
                System.setProperty("test", exact);
                SensitiveTestReporting.requireApproval(type);
                for (String selection : new String[]{type.getSimpleName(), "*Test#*", exact + ",OtherTest",
                        exact + "+other", "", type.getSimpleName() + "#*"}) {
                    System.setProperty("test", selection);
                    Assert.expectThrows(AssertionError.class, () -> SensitiveTestReporting.requireApproval(type));
                }
                System.setProperty("test", exact);
                for (String restriction : new String[]{"authenticated.ui.dedicatedAccount", "groups",
                        "surefire.rerunFailingTestsCount"}) {
                    System.setProperty(restriction, "true");
                    Assert.expectThrows(AssertionError.class, () -> SensitiveTestReporting.requireApproval(type));
                    System.clearProperty(restriction);
                }
                System.setProperty("authenticated.ui.approved", "false");
                Assert.expectThrows(AssertionError.class, () -> SensitiveTestReporting.requireApproval(type));
                System.setProperty("authenticated.ui.approved", "true");
                System.setProperty("authenticated.ui.personalAccountApproved", "false");
                Assert.expectThrows(AssertionError.class, () -> SensitiveTestReporting.requireApproval(type));
                System.setProperty("authenticated.ui.personalAccountApproved", "true");
            }
            System.setProperty("test", "SensitiveFailure#failure");
            Assert.expectThrows(AssertionError.class,
                    () -> SensitiveTestReporting.requireApproval(OfflineSensitiveFixtures.SensitiveFailure.class));
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key);
                else System.setProperty(key, value); });
        }
    }

}
