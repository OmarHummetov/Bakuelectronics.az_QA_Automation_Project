package az.bakuelectronics.automation.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;


public final class WaitUtils {

    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver, Duration timeout) {
        this.wait = new WebDriverWait(driver, timeout);
    }

    public <T> T until(Function<? super WebDriver, T> condition) {
        return wait.until(condition);
    }
}
