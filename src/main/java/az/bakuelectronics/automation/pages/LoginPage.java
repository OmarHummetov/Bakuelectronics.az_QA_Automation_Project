package az.bakuelectronics.automation.pages;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;
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
    private static final By PASSWORD_REJECTION = By.cssSelector("form > p.text-red-600");
    private static final String PASSWORD_REJECTION_TEXT = "Daxilolma uğursuz oldu. Məlumatlarınızı yoxlayın.";
    private static final By LOGIN_BUTTON = By.xpath("//button[normalize-space()='Daxil ol']");
    private static final By AUTHENTICATED_ACTION = By.xpath(
            "//*[normalize-space()='Çıxış'] | //a[@href='/istifadeci/sifarisler']");
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

    public boolean isMobileNumberValueMissing() {
        return hasMissingValue(wait.until(ExpectedConditions.visibilityOfElementLocated(MOBILE_NUMBER_INPUT)));
    }

    public boolean isEmailValueMissing() {
        return hasMissingValue(wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT)));
    }

    public boolean waitForEmailFormatValidation() {
        return wait.until(currentDriver -> Boolean.TRUE.equals(((JavascriptExecutor) currentDriver)
                .executeScript("const input = arguments[0]; return input.type === 'email'"
                        + " && input.validity.typeMismatch && !input.validity.valueMissing"
                        + " && !input.validity.valid && document.activeElement === input;",
                        currentDriver.findElement(EMAIL_INPUT))));
    }

    public void waitForPasswordInput() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));
    }

    public void assertPasswordRejectionAbsent() {
        if (isAuthenticationErrorVisible(PASSWORD_REJECTION)) {
            throw new AssertionError("The password rejection must be absent before the single submission.");
        }
    }

    public void assertIncorrectPasswordRejected() {
        assertAuthenticationRejected(PASSWORD_REJECTION, PASSWORD_REJECTION_TEXT);
    }


    public void assertAuthenticationRejected(By errorLocator, String expectedText) {
        if (errorLocator == null || expectedText == null || expectedText.isBlank()) {
            throw new AssertionError("Rejection verification requires a locator and nonempty expected text.");
        }
        try {
            wait.until(currentDriver -> hasExactAuthenticationError(errorLocator, expectedText)
                    && isUnauthenticatedPasswordStep());
        } catch (WebDriverException ignored) {

            throw new AssertionError("RED ID rejection verification failed: require one visible error with exact text,"
                    + " a visible password form and no authenticated state. Sensitive details were suppressed.");
        }
    }

    public boolean isAuthenticationErrorVisible(By errorLocator) {
        return driver.findElements(errorLocator).stream().anyMatch(WebElement::isDisplayed);
    }

    public boolean hasExactAuthenticationError(By errorLocator, String expectedText) {
        var errors = driver.findElements(errorLocator);
        return errors.size() == 1 && errors.getFirst().isDisplayed()
                && errors.getFirst().getText().equals(expectedText);
    }

    public boolean isUnauthenticatedPasswordStep() {

        return driver.getCurrentUrl().startsWith("https://sso.redid.az/")
                && isPasswordFormVisible()
                && driver.findElements(AUTHENTICATED_ACTION).stream().noneMatch(WebElement::isDisplayed);
    }

    public boolean isPasswordFormVisible() {
        return isPasswordInputVisible()
                && driver.findElements(PASSWORD_SUBMIT_BUTTON).stream().anyMatch(WebElement::isDisplayed);
    }

    public boolean isPasswordValidForSubmission() {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return arguments[0].validity.valid;", driver.findElement(PASSWORD_INPUT)));
    }

    public boolean isPasswordInputVisible() {
        return !driver.findElements(PASSWORD_INPUT).isEmpty()
                && driver.findElement(PASSWORD_INPUT).isDisplayed();
    }

    private boolean hasMissingValue(WebElement input) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver)
                .executeScript("return arguments[0].validity.valueMissing;", input));
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
