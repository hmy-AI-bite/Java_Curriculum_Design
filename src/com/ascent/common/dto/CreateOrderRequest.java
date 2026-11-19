package com.ascent.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 创建订单请求（W2）。
 *
 * <p>提示：构造时做一次防御性拷贝，避免客户端外部修改 list；
 * 服务端仍然要校验 items 非空、quantity &gt; 0。</p>
 */
public final class CreateOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<OrderItemDTO> items;

    public CreateOrderRequest(List<OrderItemDTO> items) {
        this.items = (items == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "CreateOrderRequest{items=" + items + '}';
    }
}
