package az.bakuelectronics.automation.utils;

import az.bakuelectronics.automation.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public final class CampaignPopup {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(3);
    private static final By CLOSE_BUTTON = By.cssSelector("button.campaignModalClose");

    private CampaignPopup() {
    }

    public static void dismissIfPresent(WebDriver driver) {
        if (ConfigReader.isCampaignPopupSuppressionEnabled()) {
            return;
        }
        try {
            new WaitUtils(driver, WAIT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(CLOSE_BUTTON))
                    .click();
        } catch (TimeoutException ignored) {

        }
    }
}
