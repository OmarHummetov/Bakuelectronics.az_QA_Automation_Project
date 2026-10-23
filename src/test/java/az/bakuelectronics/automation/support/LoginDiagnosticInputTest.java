package az.bakuelectronics.automation.support;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginDiagnosticInputTest {
    @Test
    public void configuredConsoleReadsUtf8StdinWithoutEchoingEvidence() {
        var originalInput = System.in;
        var originalOutput = System.out;
        String property = "login.negative.password.diagnostic.input";
        String originalMode = System.getProperty(property);
        var output = new ByteArrayOutputStream();
        try {
            System.setProperty(property, "console");
            System.setIn(new ByteArrayInputStream(
                    "ready\nverify\n[role='alert']\nŞifrə yanlışdır.\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            LoginDiagnosticInput input = LoginDiagnosticInput.configured();
            input.confirmInputBeforeSubmission();
            var evidence = input.inspect();
            Assert.assertEquals(evidence.text(), "Şifrə yanlışdır.");
            String printed = output.toString(StandardCharsets.UTF_8);
            Assert.assertFalse(printed.contains(evidence.css()));
            Assert.assertFalse(printed.contains(evidence.text()));
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
            if (originalMode == null) System.clearProperty(property);
            else System.setProperty(property, originalMode);
        }
    }

    @Test
    public void consoleHandshakeAndEvidencePreserveExactText() {
        LoginDiagnosticInput input = input("READY", "verify", "[role='alert']", "Şifrə yanlışdır.");
        input.confirmInputBeforeSubmission();
        var evidence = input.inspect("offline-account", "offline-password");
        Assert.assertEquals(evidence.css(), "[role='alert']");
        Assert.assertEquals(evidence.text(), "Şifrə yanlışdır.");
        Assert.assertFalse(evidence.toString().contains(evidence.text()));
    }

    @Test
    public void finishCancelsBeforeSubmissionAndDuringInspection() {
        Assert.expectThrows(SkipException.class, () -> input("finish").confirmInputBeforeSubmission());
        Assert.expectThrows(SkipException.class, () -> input("finish").inspect());
        Assert.expectThrows(SkipException.class, () -> input("verify", "finish").inspect());
        Assert.expectThrows(SkipException.class, () -> input("verify", "[role='alert']", "finish").inspect());
    }

    @Test
    public void closedConsoleFallsBackWithoutRepeatingTheHandshake() {
        AtomicInteger consoleReads = new AtomicInteger();
        var answers = new ArrayDeque<>(Arrays.asList("verify", "[role='alert']", "Incorrect password"));
        LoginDiagnosticInput input = new LoginDiagnosticInput(message -> {
            return consoleReads.incrementAndGet() == 1 ? "ready" : null;
        }, message -> answers.remove());
        input.confirmInputBeforeSubmission();
        Assert.assertEquals(input.inspect().text(), "Incorrect password");
        Assert.assertEquals(consoleReads.get(), 2);
        Assert.assertTrue(answers.isEmpty());
    }

    @Test
    public void inputFailureUsesFallbackAndUnavailableFallbackIsInconclusive() {
        LoginDiagnosticInput input = new LoginDiagnosticInput(message -> {
            throw new IOException("offline input failure");
        }, message -> "ready");
        input.confirmInputBeforeSubmission();
        Assert.expectThrows(SkipException.class,
                () -> new LoginDiagnosticInput(message -> null, message -> null).confirmInputBeforeSubmission());
    }

    @Test
    public void userCanSwitchToDialogWithoutSubmittingAnything() {
        LoginDiagnosticInput input = new LoginDiagnosticInput(message -> "dialog", message -> "ready");
        input.confirmInputBeforeSubmission();
    }

    @Test
    public void incompleteOrSensitiveEvidenceCannotPass() {
        Assert.expectThrows(SkipException.class, () -> input("not-ready").confirmInputBeforeSubmission());
        Assert.expectThrows(SkipException.class, () -> input("not-verified").inspect());
        Assert.expectThrows(SkipException.class, () -> input("verify", " ", "Incorrect password").inspect());
        Assert.expectThrows(SkipException.class, () -> input("verify", "[role='alert']", " ").inspect());
        for (String sensitive : new String[]{"offline-account", "offline-password", "offline-generated"}) {
            SkipException failure = Assert.expectThrows(SkipException.class,
                    () -> input("verify", "[role='alert']", sensitive).inspect(sensitive));
            Assert.assertFalse(failure.getMessage().contains(sensitive));
            Assert.expectThrows(SkipException.class,
                    () -> input("verify", "[data-value='" + sensitive + "']", "Incorrect password").inspect(sensitive));
        }
    }

    private static LoginDiagnosticInput input(String... responses) {
        var answers = new ArrayDeque<>(Arrays.asList(responses));
        return new LoginDiagnosticInput(message -> answers.poll(), null);
    }
}
