package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;


public class LoginPage {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    private static final By MOBILE_NUMBER_TAB = By.xpath("//button[normalize-space()='Mobil nömrə']");
    private static final By EMAIL_TAB = By.xpath("//button[normalize-space()='E-poçt']");
    private static final By MOBILE_NUMBER_INPUT = By.cssSelector("input[placeholder='+994']");
    private static final By EMAIL_INPUT = By.cssSelector("input[placeholder='siz@example.com']");
    private static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
    private static final By PASSWORD_SUBMIT_BUTTON = By.cssSelector("button[type='submit']");
    private static final By LOGIN_BUTTON = By.xpath("//button[normalize-space()='Daxil ol']");
    private static final String HOME_PAGE_URL = ConfigReader.getUiBaseUrl() + "/";
    private static final Duration POST_LOGIN_WAIT_TIMEOUT = Duration.ofSeconds(25);

    private final WebDriver driver;
    private final WaitUtils wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver, WAIT_TIMEOUT);
    }

    public LoginPage selectMobileNumberLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(MOBILE_NUMBER_TAB)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(MOBILE_NUMBER_INPUT));
        return this;
    }

    public LoginPage enterMobileNumber(String mobileNumber) {
        enterText(MOBILE_NUMBER_INPUT, mobileNumber);
        return this;
    }

    public LoginPage selectEmailLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(EMAIL_TAB)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        return this;
    }

    public LoginPage enterEmail(String email) {
        enterText(EMAIL_INPUT, email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        enterText(PASSWORD_INPUT, password);
        return this;
    }

    public void submitPassword() {
        wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_SUBMIT_BUTTON)).click();
    }

    public LoginPage loginWithConfiguredCredentials() {
        String login = ConfigReader.getLogin();
        if (looksLikeEmail(login)) {
            selectEmailLogin().enterEmail(login);
        } else {
            selectMobileNumberLogin().enterMobileNumber(login);
        }

        submitLogin();
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));
        enterPassword(ConfigReader.getPassword());
        submitPassword();

        new WaitUtils(driver, POST_LOGIN_WAIT_TIMEOUT).until(
                ExpectedConditions.and(
                        ExpectedConditions.urlToBe(HOME_PAGE_URL),
                        ExpectedConditions.titleContains("Baku Electronics")));
        return this;
    }

    public void submitLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON)).click();
    }

    private void enterText(By locator, String value) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        input.clear();
        input.sendKeys(value);
    }

    private boolean looksLikeEmail(String login) {
        return login.contains("@") && login.indexOf('@') > 0 && login.indexOf('@') < login.length() - 1;
    }
}
