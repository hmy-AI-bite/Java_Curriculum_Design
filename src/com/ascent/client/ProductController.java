package com.ascent.client;

import java.io.IOException;
import java.util.List;

import com.ascent.shared.ApiException;
import com.ascent.shared.dto.ProductDto;

/**
 * 商品查询控制器。<b>所有方法都是阻塞的</b>，调用方负责放进后台线程。
 *
 * @author ascent
 * @version 2.0
 */
public interface ProductController {

	/** “全部分类”的约定取值。 */
	String ALL_CATEGORIES = "全部";

	/**
	 * 查询所有分类名称，已按字典序排好。
	 *
	 * @return 分类列表
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败
	 */
	List<String> loadCategories() throws IOException, ApiException;

	/**
	 * 按分类查询商品。
	 *
	 * @param category 分类名；传 {@link #ALL_CATEGORIES} 或 null 表示全部
	 * @return 商品列表
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败
	 */
	List<ProductDto> loadProducts(String category) throws IOException, ApiException;

	/**
	 * 按关键字搜索商品，匹配名称或 CAS 号。
	 *
	 * @param keyword 关键字，空串等价于查全部
	 * @return 商品列表
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败
	 */
	List<ProductDto> searchProducts(String keyword) throws IOException, ApiException;

	/**
	 * 关闭底层连接。窗口关闭时必须调用。
	 */
	void close();
}
