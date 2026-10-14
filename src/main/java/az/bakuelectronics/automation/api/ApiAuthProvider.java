package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.config.ConfigReader;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;

public final class ApiAuthProvider {

    private static final String REDID_CLIENT_ID =
            "83573ba0-161a-4a65-8e07-2d65c0d066c4";

    private ApiAuthProvider() {
    }


    public static String getToken() {
        try {
            return authenticate();
        } catch (RuntimeException exception) {
            logAuthenticationFailure(exception);
            throw exception;
        }
    }

    private static String authenticate() {
        System.out.println("Auth step: reading credentials");
        String login = ConfigReader.getLogin();
        String password = ConfigReader.getPassword();
        System.out.println("Auth step: credentials validated");

        System.out.println("Auth step: detecting identifier type");
        String identifierType = login.startsWith("+") ? "international" : "local";
        System.out.println("Auth step: identifier type detected (" + identifierType + ")");

        String normalizedIdentifier = normalizeRedIdIdentifier(login);
        System.out.println("Auth step: identifier normalized");

        System.out.println("Auth step: building RedID request");
        RequestSpecification redIdRequest = given()
                .contentType("application/json")
                .accept("application/json")
                .body(Map.of(
                        "identifier", normalizedIdentifier,
                        "password", password,
                        "clientId", REDID_CLIENT_ID));
        System.out.println("Auth step: RedID request built");

        System.out.println("Auth step: sending RedID request");
        Response authorizationResponse;
        try {
            authorizationResponse = redIdRequest
                    .when()
                    .post(ConfigReader.getRedIdAuthorizeUrl());
        } catch (RuntimeException exception) {
            throw authenticationFailure(
                    "RedID authorization request could not be completed.", exception);
        }
        System.out.println("Auth step: RedID response received");

        System.out.println("RedID status: " + authorizationResponse.statusCode());
        if (!isSuccessfulJsonResponse(authorizationResponse)) {
            throw authenticationFailure(
                    "RedID authorization request failed: "
                            + safeResponseSummary(authorizationResponse));
        }

        Object success;
        Object codeValue;
        try {
            success = authorizationResponse.jsonPath().get("success");
            codeValue = authorizationResponse.jsonPath().get("data.code");
        } catch (RuntimeException exception) {
            throw authenticationFailure(
                    "RedID authorization response could not be read as JSON.", exception);
        }
        if (!Boolean.TRUE.equals(success)
                || !(codeValue instanceof String code)
                || code.isBlank()) {
            throw authenticationFailure("RedID authorization code missing.");
        }

        Response ssoResponse;
        try {
            ssoResponse = given()
                    .contentType("application/json")
                    .accept("application/json")
                    .body(Map.of("code", code, "lang", "az"))
                    .when()
                    .post(ConfigReader.getSsoLoginUrl());
        } catch (RuntimeException exception) {
            throw authenticationFailure(
                    "SSO login request could not be completed.", exception);
        }

        if (!isSuccessfulResponse(ssoResponse)) {
            throw authenticationFailure(
                    "SSO login request failed with an unsuccessful response.");
        }

        Object tokenValue;
        try {
            tokenValue = ssoResponse.jsonPath().get("sso.token");
        } catch (RuntimeException exception) {
            throw authenticationFailure(
                    "SSO login response could not be read as JSON.", exception);
        }
        if (!(tokenValue instanceof String token) || token.isBlank()) {
            throw authenticationFailure("SSO token missing.");
        }

        return token;
    }

    private static String normalizeRedIdIdentifier(String identifier) {
        if (identifier.startsWith("+994")) {
            return identifier;
        }

        String digitsOnly = identifier.replaceAll("\\D", "");
        if (digitsOnly.startsWith("994")) {
            digitsOnly = digitsOnly.substring(3);
        }
        if (digitsOnly.startsWith("0") && digitsOnly.length() == 10) {
            digitsOnly = digitsOnly.substring(1);
        }
        if (digitsOnly.length() != 9) {
            throw new IllegalStateException(
                    "Environment variable 'BAKU_ELECTRONICS_LOGIN'"
                            + "' must contain an Azerbaijani mobile number.");
        }

        return String.format(
                "+994 %s %s %s %s",
                digitsOnly.substring(0, 2),
                digitsOnly.substring(2, 5),
                digitsOnly.substring(5, 7),
                digitsOnly.substring(7, 9));
    }

    private static boolean isSuccessfulJsonResponse(Response response) {
        String contentType = response.getHeader("Content-Type");
        return response.statusCode() >= 200
                && response.statusCode() < 300
                && contentType != null
                && contentType.toLowerCase().contains("json");
    }

    private static boolean isSuccessfulResponse(Response response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private static String safeResponseSummary(Response response) {
        String contentType = response.getHeader("Content-Type");
        StringBuilder summary = new StringBuilder()
                .append("status=").append(response.statusCode())
                .append(", statusLine=").append(response.statusLine())
                .append(", contentType=").append(contentType);

        String rawBody = response.asString();
        if (rawBody.isBlank()) {
            return summary.append(", body=<empty>").toString();
        }

        try {
            Object json = response.jsonPath().get("$");
            if (!(json instanceof Map<?, ?>) && !(json instanceof List<?>)) {
                return summary.append(", body=<JSON scalar omitted>").toString();
            }
            return summary.append(", body=")
                    .append(safeJsonValue(json))
                    .toString();
        } catch (RuntimeException ignored) {
            return summary.append(", body=<non-JSON body omitted>").toString();
        }
    }

    private static Object safeJsonValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, isSensitiveField(key)
                        ? "<redacted>"
                        : safeJsonValue(entry.getValue()));
            }
            return sanitized;
        }

        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : list) {
                sanitized.add(safeJsonValue(item));
            }
            return sanitized;
        }

        return value;
    }

    private static boolean isSensitiveField(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.contains("credential")
                || normalized.equals("code")
                || normalized.endsWith("code")
                || normalized.contains("secret")
                || normalized.contains("session")
                || normalized.equals("jwt")
                || normalized.contains("identifier")
                || normalized.equals("login")
                || normalized.contains("username")
                || normalized.contains("email")
                || normalized.contains("phone")
                || normalized.contains("mobile");
    }

    private static IllegalStateException authenticationFailure(String message) {
        return new IllegalStateException(message);
    }

    private static IllegalStateException authenticationFailure(String message, Throwable cause) {
        return new IllegalStateException(message, cause);
    }

    private static void logAuthenticationFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            System.err.println("Auth exception: " + current.getClass().getName());
            StackTraceElement[] stackTrace = current.getStackTrace();
            if (stackTrace.length > 0) {
                StackTraceElement location = stackTrace[0];
                System.err.println("Auth exception location: " + location.getFileName()
                        + ":" + location.getLineNumber());
            }
            current = current.getCause();
        }
    }
}
