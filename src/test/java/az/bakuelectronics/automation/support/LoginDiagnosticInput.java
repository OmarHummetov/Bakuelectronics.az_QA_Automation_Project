package az.bakuelectronics.automation.support;

import org.testng.SkipException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;

public final class LoginDiagnosticInput {
    @FunctionalInterface
    interface Prompt {
        String read(String message) throws IOException;
    }

    public record Evidence(String css, String text) {
        @Override
        public String toString() {
            return "Diagnostic evidence [contents suppressed]";
        }
    }

    private Prompt prompt;
    private Prompt fallback;

    LoginDiagnosticInput(Prompt prompt, Prompt fallback) {
        this.prompt = prompt;
        this.fallback = fallback;
    }


    public static void main(String[] args) {
        configured().confirmInputBeforeSubmission();
        System.out.println("Offline diagnostic input probe succeeded. No browser was created or request submitted.");
    }

    public static LoginDiagnosticInput configured() {
        String mode = System.getProperty("login.negative.password.diagnostic.input", "console");
        if ("dialog".equals(mode)) {
            return new LoginDiagnosticInput(LoginDiagnosticInput::dialog, null);
        }
        if (!"console".equals(mode)) {
            throw inconclusive();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        return new LoginDiagnosticInput(message -> {
            System.out.println(message);
            System.out.flush();
            return reader.readLine();
        }, LoginDiagnosticInput::dialog);
    }

    public void confirmInputBeforeSubmission() {
        if (!"ready".equalsIgnoreCase(read("Before navigation: type READY to confirm input works,"
                + " or FINISH to cancel. DIALOG switches input to a local dialog.").trim())) {
            throw inconclusive();
        }
    }

    public Evidence inspect(String... sensitiveValues) {
        String command = read("One password submission completed. Keep Chrome open for read-only DevTools inspection."
                + " Type VERIFY only after observing a server incorrect-password rejection; FINISH ends inconclusive."
                + " DIALOG switches input. Enter no personal identifiers, credentials, tokens, URLs or HTML.");
        if (!"verify".equalsIgnoreCase(command.trim())) {
            throw inconclusive();
        }
        String css = read("Enter the verified error CSS selector (prefer role, aria-live, data-testid"
                + " or structural CSS); FINISH cancels:").trim();
        String text = read("Enter the exact non-sensitive visible authentication rejection text; FINISH cancels:");
        if (css.isBlank() || text.isBlank()) {
            throw inconclusive();
        }
        for (String sensitive : sensitiveValues) {
            if (sensitive != null && !sensitive.isEmpty()
                    && (css.contains(sensitive) || text.contains(sensitive))) {
                throw inconclusive();
            }
        }
        return new Evidence(css, text);
    }

    private String read(String message) {
        while (true) {
            String value;
            try {
                value = prompt.read(message);
            } catch (IOException | RuntimeException ignored) {
                value = null;
            }
            if (value == null || "dialog".equalsIgnoreCase(value.trim())) {
                if (fallback == null) {
                    throw inconclusive();
                }
                prompt = fallback;
                fallback = null;
                continue;
            }
            if ("finish".equalsIgnoreCase(value.trim())) {
                throw inconclusive();
            }
            return value;
        }
    }

    private static String dialog(String message) throws IOException {
        FutureTask<String> task = new FutureTask<>(() -> JOptionPane.showInputDialog(
                null, message, "RED ID diagnostic", JOptionPane.QUESTION_MESSAGE));
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeAndWait(task);
            }
            return task.get();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            throw new IOException("Diagnostic input interrupted.");
        } catch (Exception ignored) {
            throw new IOException("Diagnostic input unavailable.");
        }
    }

    private static SkipException inconclusive() {
        return new SkipException("INCONCLUSIVE: diagnostic evidence was cancelled, unavailable or unsafe."
                + " No additional password submission was made; do not retry automatically.");
    }
}
