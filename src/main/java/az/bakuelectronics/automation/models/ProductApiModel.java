package az.bakuelectronics.automation.models;

import java.math.BigDecimal;

public final class ProductApiModel {

    private final Long id;
    private final String name;
    private final String productCode;
    private final String slug;
    private final BigDecimal price;
    private final BigDecimal discount;

    public ProductApiModel(Long id, String name, String productCode, String slug,
            BigDecimal price, BigDecimal discount) {
        this.id = id;
        this.name = name;
        this.productCode = productCode;
        this.slug = slug;
        this.price = price;
        this.discount = discount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getSlug() {
        return slug;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }
}
