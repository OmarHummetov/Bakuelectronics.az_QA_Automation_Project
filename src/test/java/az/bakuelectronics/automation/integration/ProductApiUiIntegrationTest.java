package az.bakuelectronics.automation.integration;

import az.bakuelectronics.automation.api.ApiTestReporter;
import az.bakuelectronics.automation.api.ProductApiService;
import az.bakuelectronics.automation.models.ProductApiModel;
import az.bakuelectronics.automation.pages.HomePage;
import az.bakuelectronics.automation.pages.ProductPage;
import az.bakuelectronics.automation.pages.SearchPage;
import az.bakuelectronics.automation.support.BaseTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Locale;

public class ProductApiUiIntegrationTest extends BaseTest {

    @Test(groups = {"integration", "regression"})
    public void compareApiProductWithUiProduct() {
        ApiTestReporter.run("Product API/UI Integration", () -> {
        ProductApiModel apiProduct = new ProductApiService()
                .getFirstDefaultSearchProductWithCompleteData();
        assertApiProductIsUsable(apiProduct);

        HomePage homePage = new HomePage(driver());
        SearchPage searchPage = new SearchPage(driver());
        ProductPage productPage = new ProductPage(driver());

        homePage.open()
                .dismissCampaignPopupIfPresent()
                .enterSearchQuery(apiProduct.getName())
                .submitSearch();

        searchPage.waitForSearchResultsPage();
        boolean productVisible = searchPage.isProductResultVisible(apiProduct);
        ApiTestReporter.verifyCondition(
                "UI Checks",
                "API product is visible in search results",
                "visible",
                productVisible ? "visible" : "not visible",
                productVisible,
                "API product was not found in search results. Slug: " + apiProduct.getSlug());

        searchPage.clickProductResult(apiProduct);
        productPage.waitForProductPage();

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
        ApiTestReporter.verifyUiEquals(
                "API and UI product names have matching brand and model tokens",
                normalizedApiTokens,
                normalizedUiTokens,
                "API and UI product names must have the same meaningful brand/model tokens.");
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
        String numeric = value.replaceAll("[^0-9,.-]", "");
        int comma = numeric.lastIndexOf(',');
        int dot = numeric.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            char decimalSeparator = comma > dot ? ',' : '.';
            char groupingSeparator = decimalSeparator == ',' ? '.' : ',';
            numeric = numeric.replace(String.valueOf(groupingSeparator), "")
                    .replace(decimalSeparator, '.');
        } else if (comma >= 0) {
            numeric = normalizeSingleSeparator(numeric, ',');
        } else if (dot >= 0) {
            numeric = normalizeSingleSeparator(numeric, '.');
        }
        return new BigDecimal(numeric);
    }

    private static String normalizeSingleSeparator(String value, char separator) {
        int separatorIndex = value.lastIndexOf(separator);
        int fractionalDigits = value.length() - separatorIndex - 1;
        if (fractionalDigits == 1 || fractionalDigits == 2) {
            return value.replace(separator, '.');
        }
        return value.replace(String.valueOf(separator), "");
    }
}
