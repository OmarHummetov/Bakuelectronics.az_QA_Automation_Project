package az.bakuelectronics.automation.support.api;

import io.restassured.response.Response;
import io.qameta.allure.Allure;
import org.testng.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

public final class ApiTestReporter {

    private static final AtomicInteger TOTAL = new AtomicInteger();
    private static final AtomicInteger PASSED = new AtomicInteger();
    private static final AtomicInteger FAILED = new AtomicInteger();
    private static final ConcurrentLinkedQueue<String> FAILED_TESTS = new ConcurrentLinkedQueue<>();
    private static final Set<String> PASSED_TESTS = ConcurrentHashMap.newKeySet();
    private static final Map<String, TestOutcome> OUTCOMES = new ConcurrentHashMap<>();
    private static final Set<String> FINALIZED = ConcurrentHashMap.newKeySet();
    private static final Set<String> CLEANUP_REPORTED = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> LATEST_IDENTITIES = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ApiTestReporter::printSummary));
    }

    private ApiTestReporter() {
    }

    public static void run(String testName, Runnable testBody) {
        String identity = newInvocationIdentity(testName);
        LATEST_IDENTITIES.put(testName, identity);
        try {
            run(identity, testName, testBody);
        } finally {
            finish(identity);
        }
    }

    public static String newInvocationIdentity(String testName) {
        return testName + "#" + UUID.randomUUID();
    }


    public static void run(String identity, String displayName, Runnable testBody) {
        begin(identity, displayName);
        try {
            testBody.run();
            OUTCOMES.putIfAbsent(identity, TestOutcome.PASS);
        } catch (AssertionError | RuntimeException exception) {
            OUTCOMES.put(identity, TestOutcome.FAIL);
            throw exception;
        }
    }

    private static void begin(String identity, String displayName) {
        synchronized (OUTCOMES) {
            OUTCOMES.remove(identity);
            FINALIZED.remove(identity);
            CLEANUP_REPORTED.remove(identity);
            DISPLAY_NAMES.put(identity, displayName);
            LATEST_IDENTITIES.put(displayName, identity);
        }
        System.out.println("\n[API TEST] " + displayName);
        Allure.step("Test purpose: " + displayName);
        Allure.step("Operation: execute the requested API mutation and verify the resulting basket state.");
    }

    public static void recordPostTestFailure(String identity) {
        String invocationIdentity = OUTCOMES.containsKey(identity)
                ? identity : LATEST_IDENTITIES.getOrDefault(identity, identity);
        recordPostTestFailure(invocationIdentity, identity);
    }

    public static void recordPostTestFailure(String identity, String displayName) {
        synchronized (OUTCOMES) {
            DISPLAY_NAMES.putIfAbsent(identity, displayName);
            TestOutcome previous = OUTCOMES.put(identity, TestOutcome.FAIL);
            if (FINALIZED.contains(identity) && previous == TestOutcome.PASS) {
                PASSED.decrementAndGet();
                FAILED.incrementAndGet();
                PASSED_TESTS.remove(identity);
                FAILED_TESTS.add(identity);
            }
            if (!CLEANUP_REPORTED.add(identity)) {
                return;
            }
        }
        System.out.println("[FAIL] [Cleanup] " + displayName
                + " — post-test cleanup failed"
                + " | Expected: baseline basket restored | Actual: cleanup error");
        Allure.step("Cleanup failed: baseline basket was not restored for " + displayName);
    }


    public static void finish(String identity) {
        synchronized (OUTCOMES) {
            if (!FINALIZED.add(identity)) {
                return;
            }
            TestOutcome outcome = OUTCOMES.getOrDefault(identity, TestOutcome.FAIL);
            String displayName = DISPLAY_NAMES.getOrDefault(identity, identity);
            if (outcome == TestOutcome.PASS) {
                TOTAL.incrementAndGet();
                PASSED.incrementAndGet();
                PASSED_TESTS.add(identity);
                System.out.println("[PASS] [Data Structure] Contract checks completed"
                        + " | Expected: all checks passed | Actual: all checks passed");
                Allure.step("Test passed: " + displayName);
            } else {
                TOTAL.incrementAndGet();
                FAILED.incrementAndGet();
                FAILED_TESTS.add(identity);
                System.out.println("[FAIL] " + displayName);
                Allure.step("Test failed: " + displayName);
            }
        }
    }

    static int[] outcomeCounts() {
        return new int[] {TOTAL.get(), PASSED.get(), FAILED.get()};
    }

    private enum TestOutcome {
        PASS, FAIL
    }





    public static void verifySetupResponse(Response response, int expectedStatus) {
        check("HTTP Response", "Setup request status code is correct", String.valueOf(expectedStatus),
                response.statusCode(), () -> Assert.assertEquals(response.statusCode(), expectedStatus,
                        "Expected HTTP " + expectedStatus));
        String contentType = response.getHeader("Content-Type");
        check("Response Format", "Setup response Content-Type is JSON", "JSON", contentType,
                () -> Assert.assertTrue(contentType != null && contentType.toLowerCase().contains("json"),
                        "Setup response must be JSON"));
        check("Response Format", "Setup response body is not empty", "non-empty response",
                response.asString().isBlank() ? "empty response" : "non-empty response",
                () -> Assert.assertFalse(response.asString().isBlank(),
                        "Setup response body must not be empty"));
        Object root = response.jsonPath().get("$");
        check("Data Structure", "Setup response has a non-empty object root", "non-empty object",
                root instanceof Map<?, ?> map && !map.isEmpty() ? "non-empty object" : typeName(root),
                () -> Assert.assertTrue(root instanceof Map<?, ?> && !((Map<?, ?>) root).isEmpty(),
                        "Setup response must have a non-empty object root"));
    }





    public static void verifyBasketResponseContract(Response response, String expectedSlug,
                                                     Integer expectedQuantity) {
        verifyStatus(response, 200);
        verifyJsonResponse(response);
        verifyJsonRoot(response);
        verifyFieldType(response, "basket_id", Number.class);
        verifyFieldType(response, "total", Number.class);
        verifyFieldType(response, "delivery_configs", Map.class);
        verifyFieldType(response, "products", List.class);
        verifyFieldType(response, "delivery_configs.starex_delivery_is_available", Boolean.class);
        verifyFieldType(response, "delivery_configs.internal_delivery_cities", List.class);

        List<?> products = response.jsonPath().getList("products");
        for (int index = 0; index < products.size(); index++) {
            Object productValue = products.get(index);
            String path = "products[" + index + "]";
            check("Basket Information", "Basket product is an object", "object", typeName(productValue),
                    () -> Assert.assertTrue(productValue instanceof Map<?, ?>,
                            path + " must be an object"));
            if (!(productValue instanceof Map<?, ?> product)) {
                continue;
            }
            Object slug = product.get("slug");
            check("Product Information", "Basket product technical identifier is not blank", "non-blank string",
                    safeValue(slug), () -> Assert.assertTrue(slug instanceof String
                            && !((String) slug).isBlank(), path + ".slug must be non-blank"));
            Object quantity = product.get("quantity");
            check("Basket Information", "Basket product quantity is numeric", "Number", typeName(quantity),
                    () -> Assert.assertTrue(quantity instanceof Number,
                            path + ".quantity must be numeric"));
        }

        if (expectedSlug != null) {
            Map<?, ?> product = findBasketProduct(products, expectedSlug);
            check("Basket Information", "Expected product is present in the basket", expectedSlug,
                    product == null ? null : product.get("slug"),
                    () -> Assert.assertNotNull(product,
                            "Expected basket product was not returned: " + expectedSlug));
            if (product != null && expectedQuantity != null) {
                Object actualQuantity = product.get("quantity");
                check("Basket Information", "Basket product quantity is correct",
                        expectedQuantity, actualQuantity,
                        () -> Assert.assertEquals(actualQuantity, expectedQuantity,
                                "Basket product quantity is incorrect for slug " + expectedSlug));
            }
        }
    }

    public static void verifyBasketProductAbsent(Response response, String productSlug) {
        List<?> products = response.jsonPath().getList("products");
        boolean present = products != null && findBasketProduct(products, productSlug) != null;
        check("Cleanup", "Test product is absent after removal", "absent",
                present ? productSlug : "absent",
                () -> Assert.assertFalse(present,
                        "Removed product must not remain in the basket: " + productSlug));
    }

    public static void verifyStatus(Response response, int expectedStatus) {
        check("HTTP Response", "Status code is correct", String.valueOf(expectedStatus), response.statusCode(),
                () -> Assert.assertEquals(response.statusCode(), expectedStatus,
                        "Expected HTTP " + expectedStatus));
    }

    public static void verifySuccessfulStatus(Response response) {
        check("HTTP Response", "Response has a successful status code", "2xx", response.statusCode(),
                () -> Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                        "Expected a successful HTTP status code"));
    }

    public static void verifyStatusOneOf(Response response, int... expectedStatuses) {
        String expected = java.util.Arrays.toString(expectedStatuses);
        check("HTTP Response", "Status code is one of the allowed values", expected, response.statusCode(), () -> {
            for (int expectedStatus : expectedStatuses) {
                if (response.statusCode() == expectedStatus) {
                    return;
                }
            }
            Assert.fail("Expected one of " + expected + " but got " + response.statusCode());
        });
    }

    public static void verifyResponseTime(Response response) {
        check("HTTP Response", "Response time is below five seconds", "< 5000 ms", response.time() + " ms",
                () -> Assert.assertTrue(response.time() < 5000,
                        "Response must be returned within 5 seconds"));
    }

    public static void verifyJsonResponse(Response response) {
        String contentType = response.getHeader("Content-Type");
        check("Response Format", "Response Content-Type is JSON", "JSON", contentType,
                () -> Assert.assertNotNull(contentType, "Content-Type response header must be present"));
        check("Response Format", "Response Content-Type uses the JSON media type", "application/json", contentType,
                () -> Assert.assertTrue(contentType != null
                                && contentType.toLowerCase().contains("json"),
                        "Response must be JSON"));
        check("Response Format", "Response body is not empty", "non-empty", response.asString().isBlank() ? "empty" : "non-empty",
                () -> Assert.assertFalse(response.asString().isBlank(),
                        "Response body must not be empty"));
    }

    public static Object verifyJsonRoot(Response response) {
        Object root = response.jsonPath().get("$");
        check("Data Structure", "JSON root is an object", "object", typeName(root),
                () -> Assert.assertTrue(root instanceof Map<?, ?>,
                        "JSON response must have an object at its root"));
        check("Data Structure", "JSON root contains required fields", "at least one", root instanceof Map<?, ?> map ? map.size() : 0,
                () -> Assert.assertTrue(root instanceof Map<?, ?> map && !map.isEmpty(),
                        "JSON response must contain at least one field"));
        return root;
    }

    public static Object verifyFieldPresent(Response response, String path) {
        Object value = response.jsonPath().get(path);
        check("Response field: " + path, "present and non-null", value,
                () -> Assert.assertNotNull(value, "Response field must be present: " + path));
        return value;
    }

    public static void verifyFieldType(Response response, String path, Class<?> expectedType) {
        Object value = response.jsonPath().get(path);
        check("Field type: " + path, expectedType.getSimpleName(), typeName(value),
                () -> Assert.assertTrue(expectedType.isInstance(value),
                        "Expected " + path + " to be " + expectedType.getSimpleName()));
    }

    public static void verifyNonBlankString(Response response, String path) {
        Object value = response.jsonPath().get(path);
        Object displayedValue = isSensitivePath(path) ? "<redacted>" : value;
        check("Field value: " + path, "non-blank string", displayedValue,
                () -> Assert.assertTrue(value instanceof String && !((String) value).isBlank(),
                        "Expected " + path + " to be a non-blank string"));
    }

    public static void verifyListContains(Response response, String path, Object expectedValue) {
        Object value = response.jsonPath().get(path);
        check("Array field: " + path, "contains " + safeValue(expectedValue), value,
                () -> Assert.assertTrue(value instanceof List<?> list && list.contains(expectedValue),
                        "Expected " + path + " to contain " + expectedValue));
    }

    public static void verifyValuePresent(String label, Object value) {
        check(label, "present", value,
                () -> Assert.assertNotNull(value, label + " must be present"));
    }

    public static void verifyEquals(String label, Object expected, Object actual) {
        check(label, safeValue(expected), actual,
                () -> Assert.assertEquals(actual, expected, label + " has an unexpected value"));
    }

    public static void verifyNonBlankValue(String label, Object value) {
        check(label, "non-blank", value,
                () -> Assert.assertTrue(value != null && !String.valueOf(value).isBlank(),
                        label + " must not be blank"));
    }

    public static void verifyUiProductVisible(String productSlug, boolean visible) {
        check("UI Checks", "API-added product is visible in the basket", "visible",
                visible ? "visible" : "not visible",
                () -> Assert.assertTrue(visible,
                        "Product with slug " + productSlug + " must be visible in the UI basket"));
    }

    public static void verifyUiProductQuantity(String productSlug, int expectedQuantity,
                                               Integer actualQuantity) {
        check("UI Checks", "API-added product UI quantity is correct",
                expectedQuantity, actualQuantity,
                () -> Assert.assertEquals(actualQuantity, expectedQuantity,
                        "UI quantity for product slug " + productSlug + " has an unexpected value"));
    }

    public static void verifyCondition(String category, String description, Object expected,
                                       Object actual, boolean condition, String failureMessage) {
        check(category, description, expected, actual,
                () -> Assert.assertTrue(condition, failureMessage));
    }

    public static void verifyUiEquals(String description, Object expected, Object actual,
                                      String failureMessage) {
        check("UI Checks", description, expected, actual,
                () -> Assert.assertEquals(actual, expected, failureMessage));
    }

    public static void verifyDefaultSearchProduct(Response response) {
        Object root = response.jsonPath().get("$");
        ProductCandidate candidate = findProduct(root, "$");
        check("Product object", "one product with slug/name-or-title/price", candidate,
                () -> Assert.assertNotNull(candidate,
                        "Default search response must contain a product object"));
        if (candidate == null) {
            return;
        }

        Map<?, ?> product = candidate.product();
        String slugKey = candidate.path() + ".slug";
        Object slug = product.get("slug");
        check("Field type: " + slugKey, "String", typeName(slug),
                () -> Assert.assertTrue(slug instanceof String, "Product slug must be a string"));
        check("Field value: " + slugKey, "non-blank", slug,
                () -> Assert.assertTrue(slug instanceof String && !((String) slug).isBlank(),
                        "Product slug must not be blank"));

        String nameKey = product.containsKey("name") ? "name" : "title";
        Object name = product.get(nameKey);
        check("Field type: " + candidate.path() + "." + nameKey, "String", typeName(name),
                () -> Assert.assertTrue(name instanceof String, "Product name/title must be a string"));
        check("Field value: " + candidate.path() + "." + nameKey, "non-blank", name,
                () -> Assert.assertTrue(name instanceof String && !((String) name).isBlank(),
                        "Product name/title must not be blank"));

        Object price = product.get("price");
        verifyNumericPrice(candidate.path() + ".price", price);
    }

    public static void verifyDefaultSearchProductPresent(Response response) {
        ProductCandidate candidate = findProduct(response.jsonPath().get("$"), "$");
        check("Default search product", "present", candidate,
                () -> Assert.assertNotNull(candidate,
                        "Default search response must contain a product object"));
    }

    private static void check(String label, String expected, Object actual, Runnable assertion) {
        check(categoryForLabel(label), humanLabel(label), expected, actual, assertion);
    }

    private static void check(String category, String label, Object expected, Object actual,
                              Runnable assertion) {
        try {
            Allure.step(category + " — " + label
                    + " | Expected: " + safeValue(expected)
                    + " | Actual: " + safeValue(actual), () -> assertion.run());
            System.out.println("[PASS] [" + category + "] " + label
                    + " | Expected: " + safeValue(expected)
                    + " | Actual: " + safeValue(actual));
        } catch (AssertionError | RuntimeException exception) {
            System.out.println("[FAIL] [" + category + "] " + label
                    + " | Expected: " + safeValue(expected)
                    + " | Actual: " + safeValue(actual)
                    + " | Reason: " + safeMessage(exception));
            throw exception;
        }
    }

    private static String categoryForLabel(String label) {
        String normalized = label.toLowerCase();
        if (normalized.contains("http") || normalized.contains("response time")) {
            return "HTTP Response";
        }
        if (normalized.contains("content-type") || normalized.contains("response body")) {
            return "Response Format";
        }
        if (normalized.contains("removed") || normalized.contains("cleanup")) {
            return "Cleanup";
        }
        if (normalized.equals("products") || normalized.contains("delivery_configs")) {
            return "Basket Information";
        }
        if (normalized.contains("product state") || normalized.contains("nested object")
                || normalized.contains("products[")) {
            return "Basket Information";
        }
        if (normalized.contains("product") || normalized.contains("slug")
                || normalized.contains("price") || normalized.contains("name")) {
            return "Product Information";
        }
        if (normalized.contains("basket") || normalized.contains("quantity")
                || normalized.contains("delivery") || normalized.contains("total")) {
            return "Basket Information";
        }
        return "Data Structure";
    }

    private static String humanLabel(String label) {
        String normalized = label.toLowerCase();
        if (normalized.equals("http status")) {
            return "Status code is correct";
        }
        if (normalized.equals("content-type")) {
            return "Response Content-Type is JSON";
        }
        if (normalized.equals("content-type format")) {
            return "Response Content-Type uses the JSON media type";
        }
        if (normalized.equals("response body")) {
            return "Response body is not empty";
        }
        if (normalized.equals("json root")) {
            return "JSON root is an object";
        }
        if (normalized.equals("json root fields")) {
            return "JSON root contains required fields";
        }
        if (normalized.equals("unauthorized error message")) {
            return "Unauthorized request has an error message";
        }
        if (normalized.startsWith("quantity for ")) {
            return "Basket product quantity is correct (" + label.substring(14) + ")";
        }
        if (normalized.startsWith("product state:")) {
            return "Expected product is present in the basket";
        }
        if (normalized.startsWith("removed product:")) {
            return "Test product is removed from the basket";
        }
        if (normalized.startsWith("nested object:")) {
            return "Basket product is an object";
        }
        if (normalized.startsWith("field type:")) {
            return "Response field type is correct (" + label.substring(11) + ")";
        }
        if (normalized.startsWith("field value:")) {
            return "Response field value is correct (" + label.substring(12) + ")";
        }
        if (normalized.startsWith("field presence:")) {
            return "Response field is present (" + label.substring(16) + ")";
        }
        if (normalized.startsWith("response field:")) {
            return "Response field is present (" + label.substring(15) + ")";
        }
        if (normalized.startsWith("array field:")) {
            return "Array contains the expected value (" + label.substring(12) + ")";
        }
        if (normalized.equals("product object")) {
            return "Product object is present";
        }
        if (normalized.equals("default search product")) {
            return "Default search result contains a product";
        }
        if (normalized.startsWith("response time")) {
            return "Response time is within the limit";
        }
        return label;
    }

    private static ProductCandidate findProduct(Object value, String path) {
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey("slug") && (map.containsKey("name") || map.containsKey("title"))
                    && map.containsKey("price")) {
                return new ProductCandidate(map, path);
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                ProductCandidate found = findProduct(entry.getValue(), path + "." + entry.getKey());
                if (found != null) {
                    return found;
                }
            }
        } else if (value instanceof Iterable<?> iterable) {
            int index = 0;
            for (Object item : iterable) {
                ProductCandidate found = findProduct(item, path + "[" + index++ + "]");
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Map<?, ?> findBasketProduct(List<?> products, String productSlug) {
        for (Object value : products) {
            if (value instanceof Map<?, ?> product && productSlug.equals(product.get("slug"))) {
                return product;
            }
        }
        return null;
    }

    private static void verifyNumericPrice(String path, Object price) {
        check("Field presence: " + path, "non-null", safeValue(price),
                () -> Assert.assertNotNull(price, path + " must be present and non-null"));
        check("Field type: " + path, "Number or numeric String", typeName(price),
                () -> Assert.assertTrue(price instanceof Number || price instanceof String,
                        "Expected " + path + " to be a Number or numeric String, but got "
                                + typeName(price)));

        check("Field value: " + path, "non-null numeric value", safeValue(price), () -> {
            Assert.assertNotNull(price, path + " must be present and non-null");
            if (price instanceof String text) {
                Assert.assertFalse(text.isBlank(), path + " must not be blank");
            }
            try {
                new BigDecimal(String.valueOf(price));
            } catch (NumberFormatException exception) {
                Assert.fail(path + " must be parseable as a decimal number; actual value was: "
                        + safeValue(price), exception);
            }
        });
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private static String safeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map<?, ?> || value instanceof List<?> || value instanceof ArrayList<?>) {
            return "<structured value>";
        }
        String text = String.valueOf(value);
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static boolean isSensitivePath(String path) {
        String normalized = path.toLowerCase();
        return normalized.contains("token")
                || normalized.contains("password")
                || normalized.equals("code")
                || normalized.endsWith(".code")
                || normalized.contains("cookie")
                || normalized.contains("authorization");
    }

    private static void printSummary() {
        System.out.println("\n[API SUMMARY] total=" + TOTAL.get()
                + ", passed=" + PASSED.get() + ", failed=" + FAILED.get());
        if (!FAILED_TESTS.isEmpty()) {
            System.out.println("[API SUMMARY] failed tests: " + String.join(", ", FAILED_TESTS));
        }
    }

    private record ProductCandidate(Map<?, ?> product, String path) {
    }
}
