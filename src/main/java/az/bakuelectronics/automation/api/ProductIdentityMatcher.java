package az.bakuelectronics.automation.api;

import az.bakuelectronics.automation.models.ProductApiModel;

import java.util.Locale;


public final class ProductIdentityMatcher {

    private ProductIdentityMatcher() {
    }

    public static boolean matches(ProductApiModel apiProduct, ProductApiModel uiProduct) {
        if (apiProduct == null || uiProduct == null) {
            return false;
        }
        if (sameNonBlank(apiProduct.getSlug(), uiProduct.getSlug())) {
            return true;
        }
        if (sameNonBlank(apiProduct.getProductCode(), uiProduct.getProductCode())) {
            return true;
        }
        if (apiProduct.getId() != null || uiProduct.getId() != null) {
            return apiProduct.getId() != null && apiProduct.getId().equals(uiProduct.getId());
        }
        if (!isBlank(apiProduct.getProductCode()) || !isBlank(uiProduct.getProductCode())
                || !isBlank(apiProduct.getSlug()) || !isBlank(uiProduct.getSlug())) {
            return false;
        }
        return sameNormalizedName(apiProduct.getName(), uiProduct.getName());
    }

    public static String matchingIdentifier(ProductApiModel product) {
        if (product == null) {
            return "<none>";
        }
        if (!isBlank(product.getSlug())) {
            return "slug=" + product.getSlug();
        }
        if (!isBlank(product.getProductCode())) {
            return "sku=" + product.getProductCode();
        }
        if (product.getId() != null) {
            return "id=" + product.getId();
        }
        return "name=" + normalize(product.getName());
    }

    private static boolean sameNonBlank(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean sameNormalizedName(String left, String right) {
        return !isBlank(left) && !isBlank(right) && normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
