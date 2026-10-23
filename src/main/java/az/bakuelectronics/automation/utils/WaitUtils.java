package az.bakuelectronics.automation.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.time.Duration;
import java.util.function.Function;
import org.openqa.selenium.support.ui.ExpectedConditions;

public final class WaitUtils {

    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver, Duration timeout) {
        this.wait = new WebDriverWait(driver, timeout);
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    public <T> T until(Function<? super WebDriver, T> condition) {
        return wait.until(condition);
    }

    public WebElement visible(By locator) {
        return until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return until(ExpectedConditions.elementToBeClickable(locator));
    }

    public boolean gone(By locator) {
        return until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean urlContains(String fragment) {
        return until(ExpectedConditions.urlContains(fragment));
    }

    public <T> T refreshed(ExpectedCondition<T> condition) {
        return until(ExpectedConditions.refreshed(condition));
    }
}
