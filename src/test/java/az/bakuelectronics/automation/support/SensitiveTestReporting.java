package az.bakuelectronics.automation.support;

import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.lang.reflect.InvocationTargetException;


public final class SensitiveTestReporting {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final String REDACTED = "Sensitive authentication failure: details suppressed.";

    private SensitiveTestReporting() { }

    public static boolean isSensitive(ITestResult result) {
        return result.getInstance() != null
                && result.getInstance().getClass().isAnnotationPresent(SensitiveAuth.class)
                || result.getMethod() != null
                && Arrays.asList(result.getMethod().getGroups()).contains("sensitive-auth");
    }

    static boolean enter(boolean sensitive) {
        boolean previous = ACTIVE.get();
        ACTIVE.set(previous || sensitive);
        return previous;
    }

    static void leave(boolean previous) {
        if (previous) ACTIVE.set(true);
        else ACTIVE.remove();
    }

    public static boolean active() {
        return ACTIVE.get();
    }

    public static String safeDetail(String detail) {
        return active() ? REDACTED : detail;
    }

    public static Throwable safeFailure(Throwable failure) {
        return active() ? redact(failure) : failure;
    }

    public static void sanitize(ITestResult result) {
        if (isSensitive(result) && result.getThrowable() != null) {
            result.setThrowable(redact(result.getThrowable()));
        }
    }

    static Throwable redact(Throwable failure) {
        int suppressed = 0;
        while (failure instanceof InvocationTargetException && failure.getCause() != null) {
            suppressed += failure.getSuppressed().length;
            failure = failure.getCause();
        }

        Throwable safe = failure instanceof SkipException ? new SkipException(REDACTED)
                : new AssertionError(REDACTED);
        suppressed += failure.getSuppressed().length;
        for (int index = 0; index < suppressed; index++) {
            safe.addSuppressed(new AssertionError("Sensitive cleanup failure: details suppressed."));
        }
        return safe;
    }

    static void requireApproval(Class<?> type) {
        if (!type.isAnnotationPresent(SensitiveAuth.class)) return;
        String selection = System.getProperty("test", "");
        boolean exactMethod = Arrays.stream(type.getMethods())
                .filter(method -> method.isAnnotationPresent(Test.class))
                .anyMatch(method -> selection.equals(type.getSimpleName() + "#" + method.getName())
                        || selection.equals(type.getName() + "#" + method.getName()));
        boolean dedicated = Boolean.getBoolean("authenticated.ui.dedicatedAccount");
        boolean personal = Boolean.getBoolean("authenticated.ui.personalAccountApproved");
        String personalMethod = type == az.bakuelectronics.automation.ui.AccountLoginTest.class
                ? "configuredIdentifierCanBeEnteredInTheCorrectSsoMode" : "";
        boolean personalTarget = !personalMethod.isEmpty()
                && (selection.equals(type.getSimpleName() + "#" + personalMethod)
                || selection.equals(type.getName() + "#" + personalMethod));
        if (!exactMethod || !Boolean.getBoolean("authenticated.ui.approved")
                || dedicated == personal || personal && !personalTarget
                || !"0".equals(System.getProperty("surefire.rerunFailingTestsCount", "0"))
                || !System.getProperty("groups", "").isBlank()) {
            throw new AssertionError("Authenticated UI execution requires one exact -Dtest=Class#method, "
                    + "-Dauthenticated.ui.approved=true and exactly one account approval mode. "
                    + "Personal approval is restricted to the reviewed AccountLoginTest method.");
        }
    }
}
