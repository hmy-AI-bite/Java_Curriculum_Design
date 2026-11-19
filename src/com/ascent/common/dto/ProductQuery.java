package com.ascent.common.dto;

import java.io.Serializable;

/**
 * 商品列表/搜索条件（W2）。
 *
 * <p>提示：keyword 允许为空；空字符串表示不过滤。
 * 不要直接把 keyword 拼进 SQL，要用 PreparedStatement 的 ? 参数。</p>
 */
public final class ProductQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String keyword;
    private final String category;

    public ProductQuery(String keyword, String category) {
        this.keyword = keyword;
        this.category = category;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "ProductQuery{keyword='" + keyword + "', category='" + category + "'}";
    }
}
