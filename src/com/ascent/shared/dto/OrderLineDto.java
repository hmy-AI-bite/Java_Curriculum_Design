package com.ascent.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单里的一行商品。
 *
 * <p>
 * {@link #productName} 和 {@link #unitPrice} 是<b>下单那一刻的快照</b>。
 * 商品以后改名或调价，历史订单仍然保持原样，这是订单系统的基本要求。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class OrderLineDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 商品主键。 */
	private int productId;

	/** 下单时的商品名。 */
	private String productName;

	/** 下单时的单价。 */
	private BigDecimal unitPrice = BigDecimal.ZERO;

	/** 数量。 */
	private int quantity;

	/**
	 * 默认构造，反序列化需要。
	 */
	public OrderLineDto() {
	}

	/**
	 * 全参数构造。
	 *
	 * @param productId   商品主键
	 * @param productName 下单时商品名
	 * @param unitPrice   下单时单价
	 * @param quantity    数量
	 */
	public OrderLineDto(int productId, String productName, BigDecimal unitPrice, int quantity) {
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
		this.quantity = quantity;
	}

	/**
	 * 小计。
	 *
	 * @return 单价 × 数量
	 */
	public BigDecimal subtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	/**
	 * 小计文本，保留两位小数。
	 *
	 * @return 例如 2200.00
	 */
	public String subtotalText() {
		return subtotal().setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return productName + " x" + quantity + " = " + subtotalText();
	}
}
