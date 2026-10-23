package az.bakuelectronics.automation.api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;


@FunctionalInterface
public interface ApiRequestExecutor {
    Response execute(RequestSpecification request, String method, String endpoint,
                     Map<String, ?> queryParameters, Object body, Map<String, String> headers);
}
