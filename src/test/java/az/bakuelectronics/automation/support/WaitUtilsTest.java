package az.bakuelectronics.automation.support;

import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;

public class WaitUtilsTest {

    @Test
    public void untilReturnsTheValueProducedByTheCondition() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> null);

        WebDriver result = new WaitUtils(driver, Duration.ofSeconds(1))
                .until(currentDriver -> currentDriver);

        Assert.assertSame(result, driver);
    }
}
