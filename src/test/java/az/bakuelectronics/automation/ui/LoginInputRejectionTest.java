package az.bakuelectronics.automation.ui;

import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.pages.LoginPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.LoginDiagnosticInput;
import az.bakuelectronics.automation.support.NoRetryAnalyzer;
import az.bakuelectronics.automation.utils.CampaignPopup;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginInputRejectionTest extends BaseTest {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final By ACCOUNT_LINK = By.cssSelector("a[href^='https://sso.redid.az/auth/']");
    private static final AtomicBoolean PASSWORD_SCENARIO_STARTED = new AtomicBoolean();

    @Test(groups = "regression")
    public void malformedEmailIsBlockedByBrowserValidation() {
        String malformedEmail = System.getProperty("login.negative.malformed.email", "negative-login-without-at");
        Assert.assertTrue(!malformedEmail.isBlank() && !malformedEmail.contains("@"),
                "The format-validation identifier must be nonempty and omit '@'.");
        LoginPage loginPage = openRedIdLogin();
        loginPage.selectEmailLogin().enterEmail(malformedEmail);
        loginPage.submitLogin();

        Assert.assertTrue(loginPage.waitForEmailFormatValidation(),
                "Submitting a malformed email should focus the input with a native type-mismatch error.");
        Assert.assertFalse(loginPage.isPasswordInputVisible(),
                "A malformed email should not advance to password entry.");
    }

    @Test(groups = {"manual-auth", "no-retry", "sensitive-auth"}, retryAnalyzer = NoRetryAnalyzer.class)
    public void incorrectPasswordIsRejectedForDedicatedTestAccount() {
        if (!Boolean.parseBoolean(System.getProperty("login.negative.password.enabled", "false"))) {
            throw new SkipException("Wrong-password test requires -Dlogin.negative.password.enabled=true. No attempt was made.");
        }
        if (!PASSWORD_SCENARIO_STARTED.compareAndSet(false, true)) {
            throw new AssertionError("This password scenario may run only once per JVM. No additional attempt was made.");
        }
        boolean diagnosticMode = Boolean.parseBoolean(
                System.getProperty("login.negative.password.diagnostic", "false"));
        boolean personalAccount = Boolean.parseBoolean(
                System.getProperty("login.negative.password.personalAccount.enabled", "false"));
        String identifier = personalAccount ? ConfigReader.getLogin()
                : requiredEnvironmentVariable("BAKU_ELECTRONICS_NEGATIVE_LOGIN_ACCOUNT");
        String validPassword = personalAccount ? ConfigReader.getPassword()
                : requiredEnvironmentVariable("BAKU_ELECTRONICS_NEGATIVE_LOGIN_PASSWORD");
        String wrongPassword = UUID.randomUUID().toString();
        if (wrongPassword.equals(validPassword)) {
            wrongPassword += "!";
        }
        LoginDiagnosticInput diagnosticInput = diagnosticMode ? LoginDiagnosticInput.configured() : null;
        if (diagnosticInput != null) {
            diagnosticInput.confirmInputBeforeSubmission();
        }
        try {
            LoginPage loginPage = openRedIdLogin();
            enterIdentifier(loginPage, identifier);
            loginPage.submitLogin();
            loginPage.waitForPasswordInput();
            loginPage.enterPassword(wrongPassword);
            Assert.assertTrue(loginPage.isPasswordValidForSubmission(),
                    "The generated password must satisfy browser validation before the server attempt.");
            loginPage.assertPasswordRejectionAbsent();
            loginPage.submitPassword();
            if (diagnosticMode) {
                LoginDiagnosticInput.Evidence evidence = diagnosticInput.inspect(identifier, validPassword, wrongPassword);
                loginPage.assertAuthenticationRejected(By.cssSelector(evidence.css()), evidence.text());
            } else {
                loginPage.assertIncorrectPasswordRejected();
            }
        } catch (WebDriverException exception) {

            throw new AssertionError("RED ID password scenario failed during browser interaction. Sensitive browser details were suppressed; do not retry automatically.");
        }
    }

    @Test(groups = "regression")
    public void emptyRequiredIdentifierIsBlockedByBrowserValidation() {
        LoginPage loginPage = openRedIdLogin();
        loginPage.selectMobileNumberLogin();
        loginPage.submitLogin();
        Assert.assertTrue(loginPage.isMobileNumberValueMissing(),
                "The empty required RED ID mobile number should remain invalid after submission.");

        loginPage.selectEmailLogin();
        loginPage.submitLogin();
        Assert.assertTrue(loginPage.isEmailValueMissing(),
                "The empty required RED ID email should remain invalid after submission.");
        Assert.assertTrue(driver().getCurrentUrl().startsWith("https://sso.redid.az/"),
                "Submitting an empty identifier should leave the browser on the RED ID form.");
    }

    private LoginPage openRedIdLogin() {
        driver().get(ConfigReader.getUiBaseUrl() + "/");
        CampaignPopup.dismissIfPresent(driver());
        new WebDriverWait(driver(), WAIT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(ACCOUNT_LINK)).click();
        new WebDriverWait(driver(), WAIT_TIMEOUT)
                .until(ExpectedConditions.urlMatches("https://sso\\.redid\\.az(?:/.*)?"));
        Assert.assertTrue(driver().getCurrentUrl().startsWith("https://sso.redid.az/"),
                "The account link should open the RED ID SSO page.");
        return new LoginPage(driver());
    }

    private void enterIdentifier(LoginPage loginPage, String identifier) {
        if (identifier.contains("@") && identifier.indexOf('@') > 0
                && identifier.indexOf('@') < identifier.length() - 1) {
            loginPage.selectEmailLogin().enterEmail(identifier);
        } else {
            loginPage.selectMobileNumberLogin().enterMobileNumber(identifier);
        }
    }

    private static String requiredEnvironmentVariable(String environmentVariable) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            throw new SkipException("Wrong-password test requires the dedicated-account environment variable "
                    + environmentVariable + ". No password attempt was made.");
        }
        return value;
    }
}
