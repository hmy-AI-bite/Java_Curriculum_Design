package com.ascent.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单。
 *
 * @author ascent
 * @version 2.0
 */
public class OrderDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 订单主键。 */
	private int id;

	/** 订单号，形如 20261119210830123。 */
	private String orderNo;

	/** 下单用户主键。 */
	private int userId;

	/** 下单时间，ISO-8601 字符串，例如 2026-11-19T21:08:30。 */
	private String createdAt;

	/** 订单总额。 */
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/** 订单明细。 */
	@SuppressWarnings("serial")
	private List<OrderLineDto> lines = new ArrayList<OrderLineDto>();

	/**
	 * 默认构造，反序列化需要。
	 */
	public OrderDto() {
	}

	/**
	 * 返回保留两位小数的总额文本。
	 *
	 * @return 例如 2250.00
	 */
	public String totalText() {
		return totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * 返回商品总件数。
	 *
	 * @return 所有明细数量之和
	 */
	public int totalQuantity() {
		int total = 0;
		for (OrderLineDto line : lines) {
			total += line.getQuantity();
		}
		return total;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
	}

	public List<OrderLineDto> getLines() {
		return lines;
	}

	/**
	 * 设置明细，null 按空列表处理。
	 *
	 * @param lines 明细
	 */
	public void setLines(List<OrderLineDto> lines) {
		this.lines = lines == null ? new ArrayList<OrderLineDto>() : lines;
	}

	@Override
	public String toString() {
		return "OrderDto{orderNo=" + orderNo + ", total=" + totalText() + ", lines=" + lines.size() + "}";
	}
}
