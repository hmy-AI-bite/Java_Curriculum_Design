package com.ascent.common.dto;

import java.io.Serializable;

/**
 * 商品 DTO（W1 先定契约，W2 由 ProductService/ProductDao 实现）。
 *
 * <p>提示：priceCents 用“分”存，客户端展示时自己除以 100；
 * 不要把 double 传来传去，金额计算统一用 long/BigDecimal。</p>
 */
public final class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String name;
    private final String cas;
    private final String image;
    private final String formula;
    private final long priceCents;
    private final int stock;
    private final String category;

    public ProductDTO(long id, String name, String cas, String image, String formula,
                      long priceCents, int stock, String category) {
        this.id = id;
        this.name = name;
        this.cas = cas;
        this.image = image;
        this.formula = formula;
        this.priceCents = priceCents;
        this.stock = stock;
        this.category = category;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCas() {
        return cas;
    }

    public String getImage() {
        return image;
    }

    public String getFormula() {
        return formula;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public int getStock() {
        return stock;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "ProductDTO{id=" + id + ", name='" + name + "', priceCents=" + priceCents
                + ", stock=" + stock + ", category='" + category + "'}";
    }
}
