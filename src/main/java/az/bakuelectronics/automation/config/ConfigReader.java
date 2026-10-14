package az.bakuelectronics.automation.config;

public final class ConfigReader {

    private static final String LOGIN_VARIABLE = "BAKU_ELECTRONICS_LOGIN";
    private static final String PASSWORD_VARIABLE = "BAKU_ELECTRONICS_PASSWORD";
    private static final String DEFAULT_UI_BASE_URL = "https://www.bakuelectronics.az";
    private static final String DEFAULT_API_BASE_URL = "https://5vyra9x.bakuelectronics.az";
    private static final String DEFAULT_REDID_AUTHORIZE_URL =
            "https://api.redid.az/client/v1/auth/authorize";
    private static final String DEFAULT_SSO_LOGIN_URL =
            "https://www.bakuelectronics.az/api/auth/sso-login";

    private ConfigReader() {
    }

    public static String getLogin() {
        return getRequiredEnvironmentVariable(LOGIN_VARIABLE);
    }

    public static String getPassword() {
        return getRequiredEnvironmentVariable(PASSWORD_VARIABLE);
    }

    /**
     * Selects the environment from -Denv first, then BAKU_ELECTRONICS_ENV.
     * The absent value intentionally keeps the original Baku Electronics setup.
     */
    public static String getEnvironment() {
        return firstNonBlank(System.getProperty("env"), System.getenv("BAKU_ELECTRONICS_ENV"), "default")
                .trim().toLowerCase();
    }

    public static String getUiBaseUrl() {
        return getUrl("UI_BASE_URL", "ui.base.url", DEFAULT_UI_BASE_URL);
    }

    public static String getApiBaseUrl() {
        return getUrl("API_BASE_URL", "api.base.url", DEFAULT_API_BASE_URL);
    }

    public static String getRedIdAuthorizeUrl() {
        return getUrl("REDID_AUTHORIZE_URL", "redid.authorize.url", DEFAULT_REDID_AUTHORIZE_URL);
    }

    public static String getSsoLoginUrl() {
        return getUrl("SSO_LOGIN_URL", "sso.login.url", DEFAULT_SSO_LOGIN_URL);
    }

    private static String getUrl(String variableSuffix, String propertySuffix, String defaultValue) {
        String environment = getEnvironment();
        String environmentProperty = environment + "." + propertySuffix;
        String environmentVariable = "BAKU_ELECTRONICS_"
                + environment.toUpperCase().replace('-', '_') + "_" + variableSuffix;
        String value = firstNonBlank(
                System.getProperty(environmentProperty),
                System.getenv(environmentVariable),
                System.getProperty(propertySuffix),
                System.getenv("BAKU_ELECTRONICS_" + variableSuffix));

        if (value == null && "default".equals(environment)) {
            value = defaultValue;
        }
        if (value == null) {
            throw new IllegalStateException("No " + propertySuffix + " configured for environment '"
                    + environment + "'. Set -D" + propertySuffix + " or " + environmentVariable + ".");
        }
        return stripTrailingSlash(value.trim());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String stripTrailingSlash(String value) {
        return value.replaceFirst("/+$", "");
    }

    private static String getRequiredEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Authenticated test requires BAKU_ELECTRONICS_LOGIN and "
                            + "BAKU_ELECTRONICS_PASSWORD to be set in the Maven process environment. "
                            + "Credential values are intentionally not displayed.");
        }
        return value;
    }

}
