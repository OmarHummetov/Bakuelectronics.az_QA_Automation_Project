package az.bakuelectronics.automation.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

public final class ScreenshotUtils {

    private static final Logger LOGGER = Logger.getLogger(ScreenshotUtils.class.getName());
    private static final Path SCREENSHOT_DIRECTORY =
            Path.of("target", "test-output", "screenshots");
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ScreenshotUtils() {
    }

    public static Optional<Path> capture(WebDriver driver, String testClass, String testMethod) {
        if (driver == null) {
            LOGGER.warning("Skipping failure screenshot because the WebDriver is null.");
            return Optional.empty();
        }

        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            LOGGER.warning("Skipping failure screenshot because the WebDriver does not support screenshots.");
            return Optional.empty();
        }

        if (!hasActiveWindow(driver)) {
            LOGGER.warning("Skipping failure screenshot because the WebDriver session is not active.");
            return Optional.empty();
        }

        try {
            Files.createDirectories(SCREENSHOT_DIRECTORY);
            String safeClass = safeName(testClass);
            String safeMethod = safeName(testMethod);
            Path destination = SCREENSHOT_DIRECTORY.resolve(
                    safeClass + "-" + safeMethod + "-"
                            + FILE_TIMESTAMP.format(LocalDateTime.now())
                            + "-thread-" + Thread.currentThread().getId() + ".png");
            Files.write(destination, screenshotDriver.getScreenshotAs(OutputType.BYTES),
                    StandardOpenOption.CREATE_NEW);
            return Optional.of(destination);
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.WARNING,
                    "Failure screenshot could not be saved for " + safeName(testClass)
                            + "." + safeName(testMethod), exception);
            return Optional.empty();
        }
    }

    private static boolean hasActiveWindow(WebDriver driver) {
        try {
            return !driver.getWindowHandles().isEmpty();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING,
                    "Skipping failure screenshot because the WebDriver session is unavailable.", exception);
            return false;
        }
    }

    private static String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "unknown-test";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
