package com.ascent.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 购物车的一行。
 *
 * <p>
 * 客户端提交订单时只保证 {@link #productId} 和 {@link #quantity} 可信；
 * {@link #productName} 和 {@link #unitPrice} 只用于界面展示，服务端下单时
 * <b>必须重新从数据库读价格和库存</b>，不能直接采信客户端传的单价。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class CartItemDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 商品主键。 */
	private int productId;

	/** 商品名称，仅用于展示。 */
	private String productName;

	/** 单价，仅用于展示。 */
	private BigDecimal unitPrice = BigDecimal.ZERO;

	/** 购买数量，至少为 1。 */
	private int quantity = 1;

	/**
	 * 默认构造，反序列化需要。
	 */
	public CartItemDto() {
	}

	/**
	 * 全参数构造。
	 *
	 * @param productId   商品主键
	 * @param productName 商品名称
	 * @param unitPrice   单价
	 * @param quantity    数量
	 */
	public CartItemDto(int productId, String productName, BigDecimal unitPrice, int quantity) {
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
		this.quantity = Math.max(1, quantity);
	}

	/**
	 * 按商品信息构造一行数量为 1 的购物车数据。
	 *
	 * @param product 商品
	 * @return 购物车行
	 */
	public static CartItemDto of(ProductDto product) {
		return new CartItemDto(product.getId(), product.getName(), product.getPrice(), 1);
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
	 * @return 例如 1100.00
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

	/**
	 * 设置单价，null 按 0 处理。
	 *
	 * @param unitPrice 单价
	 */
	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	/**
	 * 设置数量，小于 1 时按 1 处理。
	 *
	 * @param quantity 数量
	 */
	public void setQuantity(int quantity) {
		this.quantity = Math.max(1, quantity);
	}

	@Override
	public String toString() {
		return productName + " x" + quantity;
	}
}
