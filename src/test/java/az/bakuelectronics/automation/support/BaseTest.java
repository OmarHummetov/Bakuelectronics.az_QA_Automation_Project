package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.driver.DriverManager;
import az.bakuelectronics.automation.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.HasCapabilities;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.ITestResult;
import org.testng.IHookable;
import org.testng.IHookCallBack;
import org.testng.IConfigurable;
import org.testng.IConfigureCallBack;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseTest implements IHookable, IConfigurable {

    private static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());
    private static final String FAILURE_SCREENSHOT_ATTEMPTED =
            BaseTest.class.getName() + ".failureScreenshotAttempted";

    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    @Override
    public final void run(IHookCallBack callback, ITestResult result) {
        invokeProtected(result, () -> {
            SensitiveTestReporting.requireApproval(getClass());
            try {
                callback.runTestMethod(result);
            } finally {
                finishTestInvocation(result);
            }
        });
    }


    protected void finishTestInvocation(ITestResult result) { }

    @Override
    public final void run(IConfigureCallBack callback, ITestResult result) {
        invokeProtected(result, () -> {

            if (result.getMethod().isBeforeMethodConfiguration()) {
                SensitiveTestReporting.requireApproval(getClass());
            }
            callback.runConfigurationMethod(result);
        });
    }

    private static void invokeProtected(ITestResult result, Runnable invocation) {
        boolean sensitive = SensitiveTestReporting.isSensitive(result);
        boolean previous = SensitiveTestReporting.enter(sensitive);
        try {
            invocation.run();
        } catch (Throwable failure) {
            if (!sensitive) throw failure;
            Throwable safe = SensitiveTestReporting.redact(failure);
            if (safe instanceof RuntimeException runtime) throw runtime;
            throw (Error) safe;
        } finally {
            if (sensitive) {
                SensitiveTestReporting.sanitize(result);
                for (Object parameter : result.getParameters()) {
                    if (parameter instanceof ITestResult testResult) SensitiveTestReporting.sanitize(testResult);
                }
            }
            SensitiveTestReporting.leave(previous);
        }
        if (sensitive && result.getThrowable() != null) {


            Throwable safe = result.getThrowable();
            if (safe instanceof RuntimeException runtime) throw runtime;
            throw (Error) safe;
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverManager.quitDriver();
        DriverManager.createDriver();

    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (SensitiveTestReporting.isSensitive(result)) {
                SensitiveTestReporting.sanitize(result);
                return;
            }
            if (!result.isSuccess()
                    && result.getAttribute(FAILURE_SCREENSHOT_ATTEMPTED) == null) {
                result.setAttribute(FAILURE_SCREENSHOT_ATTEMPTED, Boolean.TRUE);
                WebDriver activeDriver = currentDriver();
                attachFailureDiagnostics(result, activeDriver);
                Optional<Path> screenshot = ScreenshotUtils.capture(activeDriver,
                        result.getTestClass().getName(), result.getMethod().getMethodName());
                screenshot.ifPresent(path -> attachFailureScreenshot(path));
            }
        } finally {
            DriverManager.quitDriver();
        }
    }

    private static WebDriver currentDriver() {
        try {
            return DriverManager.getDriver();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private static void attachFailureDiagnostics(ITestResult result, WebDriver activeDriver) {
        Throwable failure = result.getThrowable();
        String exceptionType = failure == null ? "unknown" : failure.getClass().getName();
        String exceptionMessage = failure == null ? "unknown" : safe(failure.getMessage());
        String diagnostics = "Test: " + result.getTestClass().getName() + "."
                        + result.getMethod().getMethodName() + "\n"
                        + "Exception: " + exceptionType + "\n"
                        + "Message: " + exceptionMessage + "\n"
                        + "URL: " + currentUrl(activeDriver) + "\n"
                        + "Browser: " + browser(activeDriver);
        Allure.attachment("Failure diagnostics", "text/plain",
                new ByteArrayInputStream(diagnostics.getBytes(StandardCharsets.UTF_8)),
                AttachmentOptions.withFileExtension(".txt"));
    }

    private static String currentUrl(WebDriver activeDriver) {
        try {
            return activeDriver == null ? "unavailable" : safe(activeDriver.getCurrentUrl());
        } catch (RuntimeException exception) {
            return "unavailable (" + exception.getClass().getSimpleName() + ")";
        }
    }

    private static String browser(WebDriver activeDriver) {
        try {
            return activeDriver instanceof HasCapabilities capable
                    ? capable.getCapabilities().getBrowserName() : "unavailable";
        } catch (RuntimeException exception) {
            return "unavailable (" + exception.getClass().getSimpleName() + ")";
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    private static void attachFailureScreenshot(Path screenshot) {
        try (InputStream screenshotStream = Files.newInputStream(screenshot)) {
            Allure.attachment("Failure screenshot", "image/png", screenshotStream,
                    AttachmentOptions.withFileExtension(".png"));
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.WARNING,
                    "Failure screenshot was saved but could not be attached to Allure: " + screenshot,
                    exception);
        }
    }
}
