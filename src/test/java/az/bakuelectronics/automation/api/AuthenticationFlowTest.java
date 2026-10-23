package az.bakuelectronics.automation.api;
import az.bakuelectronics.automation.support.api.ApiTestReporter;

import az.bakuelectronics.automation.config.ConfigReader;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class AuthenticationFlowTest {

    private static final String REDID_CLIENT_ID =
            "83573ba0-161a-4a65-8e07-2d65c0d066c4";

    @Test(groups = {"api", "authenticated-api"})
    public void shouldCompleteAuthenticationFlowAndInspectSsoResponse() {
        ApiTestReporter.run("Authentication Flow", () -> {
            String login = ConfigReader.getLogin();
            String normalizedIdentifier = normalizeRedIdIdentifier(login);

            Map<String, String> authorizeRequest = Map.of(
                    "identifier", normalizedIdentifier,
                    "password", ConfigReader.getPassword(),
                    "clientId", REDID_CLIENT_ID);

            System.out.println("Authorize endpoint: " + ConfigReader.getRedIdAuthorizeUrl());
            System.out.println("Authorize clientId length: " + REDID_CLIENT_ID.length());
            System.out.println("Authorize normalized identifier length: " + normalizedIdentifier.length());
            System.out.println("Authorize Content-Type: application/json");

            Response authorizationResponse = given()
                    .contentType("application/json")
                    .accept("application/json")
                    .body(authorizeRequest)
                    .when()
                    .post(ConfigReader.getRedIdAuthorizeUrl());

            ApiTestReporter.verifySuccessfulStatus(authorizationResponse);
            ApiTestReporter.verifyJsonResponse(authorizationResponse);
            ApiTestReporter.verifyJsonRoot(authorizationResponse);
            ApiTestReporter.verifyFieldType(authorizationResponse, "success", Boolean.class);
            ApiTestReporter.verifyEquals(
                    "Authorization success", Boolean.TRUE,
                    authorizationResponse.jsonPath().get("success"));
            ApiTestReporter.verifyNonBlankString(authorizationResponse, "data.code");
            ApiTestReporter.verifyResponseTime(authorizationResponse);

            Object codeValue = authorizationResponse.jsonPath().get("data.code");
            String code = (String) codeValue;
            Response ssoResponse = given()
                    .contentType("application/json")
                    .accept("application/json")
                    .body(Map.of("code", code, "lang", "az"))
                    .when()
                    .post(ConfigReader.getSsoLoginUrl());

            ApiTestReporter.verifySuccessfulStatus(ssoResponse);
            ApiTestReporter.verifyJsonResponse(ssoResponse);
            ApiTestReporter.verifyJsonRoot(ssoResponse);
            ApiTestReporter.verifyNonBlankString(ssoResponse, "sso.token");
            ApiTestReporter.verifyResponseTime(ssoResponse);
        });
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
            throw new IllegalArgumentException(
                    "BAKU_ELECTRONICS_LOGIN must contain an Azerbaijani mobile number");
        }

        return String.format(
                "+994 %s %s %s %s",
                digitsOnly.substring(0, 2),
                digitsOnly.substring(2, 5),
                digitsOnly.substring(5, 7),
                digitsOnly.substring(7, 9));
    }
}
