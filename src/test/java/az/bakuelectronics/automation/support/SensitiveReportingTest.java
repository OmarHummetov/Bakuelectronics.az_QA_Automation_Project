package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.driver.DriverManager;
import az.bakuelectronics.automation.support.api.BasketTestSupport;
import az.bakuelectronics.automation.ui.AccountLoginTest;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.testng.AllureTestNg;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static az.bakuelectronics.automation.support.OfflineSensitiveFixtures.*;

public class SensitiveReportingTest {
    static final String PRIVATE = "fake-password-731 fake-token-842 fake-account-953 fake@example.invalid";
    static final String URL = "https://example.invalid/auth?code=fake-code-164&token=fake-token-842#private";
    private static final List<String> MARKERS = List.of("fake-password-731", "fake-token-842",
            "fake-account-953", "fake@example.invalid", "fake-code-164", "example.invalid/auth");
    static int screenshots;
    static int pageReads;
    static int urlReads;
    static int attempts;
    static int quits;
    static int basketStarts;
    private static final String[] PROPERTIES = {"test", "authenticated.ui.approved",
            "authenticated.ui.dedicatedAccount", "authenticated.ui.personalAccountApproved"};

    @Test
    public void sensitiveTestFailureIsRedactedBeforeTestNgAndAllureReporting() throws Exception {
        Evidence evidence = run(SensitiveFailure.class, true);
        assertPrivate(evidence);
        Assert.assertEquals(evidence.failures().size(), 1);
        Assert.assertEquals(evidence.failures().getFirst().getStatus(), ITestResult.FAILURE);
        Assert.assertEquals(attempts, 1);
        Assert.assertEquals(quits, 1);
        Assert.assertTrue(evidence.artifacts().contains("\"status\":\"failed\""));
    }

    @Test
    public void sensitiveSetupAndTeardownFailuresAreRedacted() throws Exception {
        for (Class<?> type : new Class<?>[]{SetupFailure.class, TeardownFailure.class}) {
            Evidence evidence = run(type, true);
            assertPrivate(evidence);
            Assert.assertTrue(evidence.failed(), "Configuration failures must fail the run.");
            Assert.assertTrue(evidence.artifacts().contains("details suppressed"));
            Assert.assertEquals(quits, 1);
        }
    }

    @Test
    public void sensitiveCleanupFailureStillFailsAndReleasesBasketLock() throws Exception {
        Evidence evidence = run(CleanupFailure.class, true);
        assertPrivate(evidence);
        Assert.assertTrue(evidence.failed());
        Assert.assertEquals(evidence.failures().size(), 1);
        Assert.assertEquals(evidence.failures().getFirst().getStatus(), ITestResult.FAILURE);
        Assert.assertTrue(evidence.artifacts().contains("\"status\":\"failed\""));
        Assert.assertTrue(evidence.artifacts().contains("Cleanup failed"));
        Field field = BasketTestSupport.class.getDeclaredField("MUTATION_LOCK");
        field.setAccessible(true);
        Assert.assertFalse(((ReentrantLock) field.get(null)).isLocked());

        Path lock = Path.of(System.getProperty("basket.mutation.lock.file",
                Path.of(System.getProperty("java.io.tmpdir"), "baku-electronics-basket-mutation.lock").toString()));
        try (var channel = java.nio.channels.FileChannel.open(lock, java.nio.file.StandardOpenOption.WRITE);
             var acquired = channel.tryLock()) {
            Assert.assertNotNull(acquired);
        }
        Assert.assertEquals(quits, 1);
        Assert.assertEquals(basketStarts, 1);
        evidence = run(BodyAndCleanupFailure.class, true);
        assertPrivate(evidence);
        Assert.assertEquals(evidence.failures().getFirst().getStatus(), ITestResult.FAILURE);
        Assert.assertEquals(evidence.failures().getFirst().getThrowable().getSuppressed().length, 1);
        Assert.assertNull(evidence.failures().getFirst().getThrowable().getCause());
    }

    @Test
    public void ordinaryFailureRetainsDiagnosticsAndScreenshot() throws Exception {
        Evidence evidence = run(OrdinaryFailure.class, true);
        Assert.assertTrue(evidence.failed());
        Assert.assertEquals(screenshots, 1);
        Assert.assertEquals(urlReads, 1);
        Assert.assertEquals(pageReads, 0);
        Assert.assertEquals(quits, 1);
        Assert.assertTrue(evidence.artifacts().contains("Failure diagnostics"));
        Assert.assertTrue(evidence.artifacts().contains("Failure screenshot"));
        Assert.assertTrue(evidence.artifacts().contains("ordinary synthetic failure"));
    }

    @Test
    public void approvalGuardBlocksEveryBeforeMethodBeforeSideEffects() throws Exception {
        Evidence evidence = run(SensitiveFailure.class, false);
        assertPrivate(evidence);
        Assert.assertTrue(evidence.failed());
        Assert.assertEquals(attempts, 0);
        Assert.assertEquals(quits, 0);
        evidence = run(CleanupFailure.class, false);
        assertPrivate(evidence);
        Assert.assertTrue(evidence.failed());
        Assert.assertEquals(basketStarts, 0);
        Assert.assertEquals(attempts, 0);
        Assert.assertEquals(quits, 0);
        Map<String, String> previous = saveProperties();
        try {
            System.clearProperty("authenticated.ui.personalAccountApproved");
            System.setProperty("authenticated.ui.approved", "true");
            System.setProperty("authenticated.ui.dedicatedAccount", "true");
            for (String invalid : new String[]{"", "AccountLoginTest", "*Test", "AccountLoginTest#*",
                    "AccountLoginTest#configuredIdentifierCanBeEnteredInTheCorrectSsoMode,OtherTest"}) {
                System.setProperty("test", invalid);
                Assert.expectThrows(AssertionError.class, () -> SensitiveTestReporting.requireApproval(AccountLoginTest.class));
            }
            for (Class<?> target : new Class<?>[]{AccountLoginTest.class}) {
                var method = java.util.Arrays.stream(target.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(Test.class)).findFirst().orElseThrow();
                Assert.assertTrue(target.isAnnotationPresent(SensitiveAuth.class));
                var groups = List.of(method.getAnnotation(Test.class).groups());
                Assert.assertTrue(groups.contains("sensitive-auth") && groups.contains("regression"));
                Assert.assertTrue(RetryAnnotationTransformer.isExcluded(target, new String[0]));
                System.setProperty("test", target.getSimpleName() + "#" + method.getName());
                SensitiveTestReporting.requireApproval(target);
                System.setProperty("authenticated.ui.dedicatedAccount", "false");
                Assert.expectThrows(AssertionError.class, () -> SensitiveTestReporting.requireApproval(target));
                System.setProperty("authenticated.ui.dedicatedAccount", "true");
            }
        } finally {
            restoreProperties(previous);
        }
    }

    private static void assertPrivate(Evidence evidence) {
        for (String marker : MARKERS) {
            Assert.assertFalse(evidence.artifacts().contains(marker), "Private fixture data leaked to artifacts.");
            Assert.assertFalse(evidence.console().contains(marker), "Private fixture data leaked to console.");
        }
        Assert.assertFalse(evidence.artifacts().contains("Failure screenshot"));
        Assert.assertFalse(evidence.artifacts().contains("Failure diagnostics"));
        Assert.assertEquals(screenshots, 0);
        Assert.assertEquals(urlReads, 0);
        Assert.assertEquals(pageReads, 0);
        Assert.assertFalse(SensitiveTestReporting.active(), "Sensitive scope must not leak to subsequent tests.");
    }

    private record Evidence(boolean failed, List<ITestResult> failures, String artifacts, String console) { }

    private static Evidence run(Class<?> fixture, boolean approved) throws Exception {
        Map<String, String> previous = saveProperties();
        AllureLifecycle previousLifecycle = Allure.getLifecycle();
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        Path directory = Files.createTempDirectory(Path.of("target"), "reporting-offline-");
        screenshots = pageReads = urlReads = attempts = quits = basketStarts = 0;
        List<ITestResult> failures = new ArrayList<>();
        try (PrintStream capture = new PrintStream(console, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            System.setErr(capture);
            System.setProperty("test", fixture.getSimpleName() + "#failure");
            System.setProperty("authenticated.ui.approved", Boolean.toString(approved));
            System.setProperty("authenticated.ui.dedicatedAccount", "true");
            System.clearProperty("authenticated.ui.personalAccountApproved");
            AllureLifecycle lifecycle = new AllureLifecycle(new FileSystemResultsWriter(directory.resolve("allure")));
            Allure.setLifecycle(lifecycle);
            TestNG runner = new TestNG();

            runner.setServiceLoaderClassLoader(new java.net.URLClassLoader(new java.net.URL[0],
                    SensitiveReportingTest.class.getClassLoader()) {
                @Override public java.util.Enumeration<java.net.URL> getResources(String name)
                        throws java.io.IOException {
                    return name.equals("META-INF/services/org.testng.ITestNGListener")
                            ? java.util.Collections.emptyEnumeration() : super.getResources(name);
                }
            });
            runner.setUseDefaultListeners(true);
            runner.setVerbose(0);
            runner.setOutputDirectory(directory.resolve("testng").toString());
            runner.addListener(new AllureTestNg(lifecycle));
            runner.addListener(new RetryAnnotationTransformer());
            runner.addListener(new ITestListener() {
                @Override public void onTestFailure(ITestResult result) { failures.add(result); }
            });
            runner.setTestClasses(new Class<?>[]{fixture});
            runner.run();
            StringBuilder artifacts = new StringBuilder();
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    artifacts.append(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
                }
            }
            for (ITestResult failure : failures) {
                StringWriter trace = new StringWriter();
                failure.getThrowable().printStackTrace(new PrintWriter(trace));
                artifacts.append(trace);
            }
            return new Evidence(runner.hasFailure(), failures, artifacts.toString(), console.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
            Allure.setLifecycle(previousLifecycle);
            restoreProperties(previous);
            DriverManager.quitDriver();
        }
    }

    private static Map<String, String> saveProperties() {
        Map<String, String> result = new HashMap<>();
        for (String key : PROPERTIES) result.put(key, System.getProperty(key));
        return result;
    }

    private static void restoreProperties(Map<String, String> values) {
        values.forEach((key, value) -> {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
        });
    }

    @SuppressWarnings("unchecked")
    static void installDriver() {
        try {
            Field field = DriverManager.class.getDeclaredField("DRIVER");
            field.setAccessible(true);
            WebDriver driver = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, TakesScreenshot.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getCurrentUrl" -> { urlReads++; yield URL; }
                        case "getPageSource" -> { pageReads++; yield PRIVATE; }
                        case "getWindowHandles" -> Set.of("offline");
                        case "getScreenshotAs" -> {
                            screenshots++;
                            yield Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j3ioAAAAASUVORK5CYII=");
                        }
                        case "quit" -> { quits++; yield null; }
                        default -> throw new AssertionError("Unexpected offline driver operation.");
                    });
            ((ThreadLocal<WebDriver>) field.get(null)).set(driver);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not install offline driver.");
        }
    }

}
