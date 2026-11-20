package com.ascent.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 商品信息。
 *
 * <p>
 * 金额用 {@link BigDecimal} 表达，服务端数据库里存的是“分”（整数），
 * 避免 REAL 浮点误差。{@link #spec} 是规格文本（如 {@code 280g}），
 * 不是库存；库存单独用 {@link #stock}。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class ProductDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 主键，新增时为 0。 */
	private int id;

	/** 药品名称。 */
	private String name;

	/** CAS 号。 */
	private String cas;

	/** 结构图文件名，对应 images 目录。 */
	private String structure;

	/** 分子式。 */
	private String formula;

	/** 规格文本，例如 280g。 */
	private String spec;

	/** 单价，元。 */
	private BigDecimal price = BigDecimal.ZERO;

	/** 库存件数。 */
	private int stock;

	/** 分类。 */
	private String category;

	/**
	 * 默认构造，反序列化需要。
	 */
	public ProductDto() {
	}

	/**
	 * 返回保留两位小数的单价文本，便于界面直接显示。
	 *
	 * @return 例如 1100.00
	 */
	public String priceText() {
		return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * 按数量计算小计。
	 *
	 * @param quantity 数量
	 * @return 小计金额
	 */
	public BigDecimal subtotal(int quantity) {
		return price.multiply(BigDecimal.valueOf(Math.max(0, quantity)));
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCas() {
		return cas;
	}

	public void setCas(String cas) {
		this.cas = cas;
	}

	public String getStructure() {
		return structure;
	}

	public void setStructure(String structure) {
		this.structure = structure;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public String getSpec() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec = spec;
	}

	public BigDecimal getPrice() {
		return price;
	}

	/**
	 * 设置单价，null 按 0 处理。
	 *
	 * @param price 单价
	 */
	public void setPrice(BigDecimal price) {
		this.price = price == null ? BigDecimal.ZERO : price;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return name + "    CAS：" + cas;
	}
}
