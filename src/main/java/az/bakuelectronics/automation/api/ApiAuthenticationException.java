package az.bakuelectronics.automation.api;


public final class ApiAuthenticationException extends IllegalStateException {
    public ApiAuthenticationException(String message) { super(message); }
    public ApiAuthenticationException(String message, Throwable cause) { super(message, cause); }
}
