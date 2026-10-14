package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.config.ConfigReader;

public final class ApiConfig {

    public static final String API_BASE_URL = ConfigReader.getApiBaseUrl();

    private ApiConfig() {
    }
}
