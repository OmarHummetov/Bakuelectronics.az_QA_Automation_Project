package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.driver.DriverManager;
import az.bakuelectronics.automation.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BaseTest {

    private static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());
    private static final String FAILURE_SCREENSHOT_ATTEMPTED =
            BaseTest.class.getName() + ".failureScreenshotAttempted";

    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverManager.quitDriver();
        DriverManager.createDriver();

    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (!result.isSuccess()
                    && result.getAttribute(FAILURE_SCREENSHOT_ATTEMPTED) == null) {
                result.setAttribute(FAILURE_SCREENSHOT_ATTEMPTED, Boolean.TRUE);
                Optional<Path> screenshot = ScreenshotUtils.capture(
                        driver(),
                        result.getTestClass().getName(),
                        result.getMethod().getMethodName()
                );
                screenshot.ifPresent(path -> attachFailureScreenshot(path));
            }
        } finally {
            DriverManager.quitDriver();
        }
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
