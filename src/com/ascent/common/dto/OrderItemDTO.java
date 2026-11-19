package com.ascent.common.dto;

import java.io.Serializable;

/**
 * 订单明细行（W2）。
 *
 * <p>提示：客户端只传 productId 和 quantity；
 * 单价、总价必须由服务端根据数据库里的价格重新计算。</p>
 */
public final class OrderItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long productId;
    private final int quantity;

    public OrderItemDTO(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "OrderItemDTO{productId=" + productId + ", quantity=" + quantity + '}';
    }
}
