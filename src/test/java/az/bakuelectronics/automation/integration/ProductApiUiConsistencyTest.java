package az.bakuelectronics.automation.integration;

import az.bakuelectronics.automation.api.ProductApiService;
import az.bakuelectronics.automation.config.ConfigReader;
import az.bakuelectronics.automation.models.ProductApiModel;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.support.BaseTest;
import az.bakuelectronics.automation.support.api.ApiTestReporter;
import az.bakuelectronics.automation.utils.PriceUtils;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Locale;

public class ProductApiUiConsistencyTest extends BaseTest {

    @Test(groups = {"integration", "public-api", "read-only"})
    public void compareApiProductWithUiProduct() {
        ApiTestReporter.run("Product API/UI Product Detail Integration", () -> {
        ProductApiModel apiProduct = new ProductApiService()
                .getFirstDefaultSearchProductWithCompleteData();
        assertApiProductIsUsable(apiProduct);

        driver().get(ConfigReader.getUiBaseUrl() + "/mehsul/" + apiProduct.getSlug());
        ProductPage productPage = new ProductPage(driver());
        productPage.waitForProductPage();
        ApiTestReporter.verifyCondition(
                "UI Checks",
                "UI product route contains the API slug",
                apiProduct.getSlug(),
                driver().getCurrentUrl(),
                driver().getCurrentUrl().contains("/mehsul/" + apiProduct.getSlug()),
                "The UI detail page did not resolve to the API product slug.");

        String uiName = productPage.getProductName();
        String uiProductCode = productPage.getProductCode();
        String uiPrice = productPage.getCurrentPrice();

        System.out.println("[UI Checks] Current UI price | Actual: " + uiPrice);
        System.out.println("[UI Checks] Current UI price DOM source | Actual: "
                + productPage.getCurrentPriceDomDescription());

        if (!isBlank(apiProduct.getProductCode())) {
            ApiTestReporter.verifyUiEquals(
                    "API and UI product codes match",
                    apiProduct.getProductCode(),
                    uiProductCode,
                    "API and UI product codes must match.");
        }
        assertProductNamesMatch(apiProduct.getName(), uiName);
        BigDecimal normalizedUiPrice = normalizePrice(uiPrice);
        BigDecimal discount = apiProduct.getDiscount() == null
                ? BigDecimal.ZERO
                : apiProduct.getDiscount();
        BigDecimal expectedUiPrice = apiProduct.getPrice().subtract(discount);
        System.out.println("[Product Information] Raw API price | Actual: "
                + apiProduct.getPrice());
        System.out.println("[Product Information] API discount | Actual: " + discount);
        System.out.println("[UI Checks] Expected discounted UI price (API price - discount) | Expected: "
                + expectedUiPrice);
        System.out.println("[UI Checks] Actual UI current price | Actual: " + normalizedUiPrice);
        ApiTestReporter.verifyUiEquals(
                "Discounted/current UI price matches API price after discount",
                expectedUiPrice,
                normalizedUiPrice,
                "Discounted/current UI price must equal API price minus discount after format normalization.");
        });
    }

    private static void assertApiProductIsUsable(ProductApiModel product) {
        ApiTestReporter.verifyValuePresent("API product object is present", product);
        ApiTestReporter.verifyNonBlankValue("API product name is not blank", product.getName());
        ApiTestReporter.verifyNonBlankValue("API product technical identifier is not blank",
                product.getSlug());
        ApiTestReporter.verifyValuePresent("API product price is present", product.getPrice());
        ApiTestReporter.verifyCondition(
                "Product Information",
                "API product price is not negative",
                ">= 0",
                product.getPrice(),
                product.getPrice().compareTo(BigDecimal.ZERO) >= 0,
                "API product price must not be negative.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void assertProductNamesMatch(String apiName, String uiName) {
        Set<String> normalizedApiTokens = normalizeProductName(apiName);
        Set<String> normalizedUiTokens = normalizeProductName(uiName);

        ApiTestReporter.verifyCondition(
                "Product Information",
                "API product name contains meaningful tokens",
                "non-empty token set",
                normalizedApiTokens,
                !normalizedApiTokens.isEmpty(),
                "API product name must contain meaningful tokens.");
        ApiTestReporter.verifyCondition(
                "UI Checks",
                "UI product name contains meaningful tokens",
                "non-empty token set",
                normalizedUiTokens,
                !normalizedUiTokens.isEmpty(),
                "UI product name must contain meaningful tokens.");
        Set<String> stableApiTokens = normalizedApiTokens.stream()
                .filter(ProductApiUiConsistencyTest::isStableProductToken)
                .collect(Collectors.toSet());
        Set<String> stableUiTokens = normalizedUiTokens.stream()
                .filter(ProductApiUiConsistencyTest::isStableProductToken)
                .collect(Collectors.toSet());
        Set<String> sharedStableTokens = stableApiTokens.stream()
                .filter(stableUiTokens::contains)
                .collect(Collectors.toSet());
        ApiTestReporter.verifyCondition(
                "UI Checks",
                "API and UI product names share stable brand/model tokens",
                stableApiTokens,
                sharedStableTokens,
                !stableApiTokens.isEmpty() && !sharedStableTokens.isEmpty(),
                "API and UI product names must share at least one stable brand/model token; "
                        + "localized marketing words are not identity proof.");
    }

    private static boolean isStableProductToken(String token) {
        return token.length() >= 3 && (token.chars().anyMatch(Character::isDigit)
                || token.equals("yoshiro") || token.equals("ywm"));
    }

    private static Set<String> normalizeProductName(String value) {
        String normalized = value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        Set<String> localeOrMarketingTokens = Set.of("tv", "televizor", "ai");
        return Arrays.stream(normalized.replaceAll("[^\\p{L}\\p{N}]+", " ").split(" "))
                .filter(token -> !token.isBlank())
                .filter(token -> !localeOrMarketingTokens.contains(token))
                .collect(Collectors.toSet());
    }

    private static BigDecimal normalizePrice(String value) {
        ApiTestReporter.verifyCondition(
                "UI Checks",
                "UI current price is not blank",
                "non-blank price",
                value == null ? null : value,
                !isBlank(value),
                "UI current price must not be blank.");
        return PriceUtils.parseAzn(value);
    }
}
