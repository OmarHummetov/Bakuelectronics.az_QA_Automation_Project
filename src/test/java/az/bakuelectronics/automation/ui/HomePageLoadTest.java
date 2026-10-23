package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.support.BaseTest;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

public class HomePageLoadTest extends BaseTest {

    @Test(groups = "smoke")
    public void homePageLoadsSuccessfully() {
        driver().get(ConfigReader.getUiBaseUrl() + "/");

        new WebDriverWait(driver(), Duration.ofSeconds(10))
                .until(ExpectedConditions.titleContains("Baku Electronics"));

        Assert.assertTrue(driver().getTitle().contains("Baku Electronics"),
                "The Baku Electronics home page title should be displayed.");
    }
}
