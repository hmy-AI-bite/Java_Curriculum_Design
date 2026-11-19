package com.ascent.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单 DTO（W2）。
 *
 * <p>提示：createdAt 用 String（格式 yyyy-MM-dd HH:mm:ss）而不是 LocalDateTime，
 * 因为 LocalDateTime 可序列化，但字符串跨版本更稳，前端也更好展示。</p>
 */
public final class OrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final long userId;
    private final long totalCents;
    private final String status;
    private final String createdAt;
    private final List<OrderItemDTO> items;

    public OrderDTO(long id, long userId, long totalCents, String status,
                    String createdAt, List<OrderItemDTO> items) {
        this.id = id;
        this.userId = userId;
        this.totalCents = totalCents;
        this.status = status;
        this.createdAt = createdAt;
        this.items = (items == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "OrderDTO{id=" + id + ", userId=" + userId + ", totalCents=" + totalCents
                + ", status='" + status + "', createdAt='" + createdAt + "'}";
    }
}
