package com.ascent.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ascent.shared.ApiException;
import com.ascent.shared.ErrorCode;
import com.ascent.shared.Protocol;
import com.ascent.shared.Result;
import com.ascent.shared.dto.ProductDto;

/**
 * {@link ProductController} 的 socket 实现。
 *
 * @author ascent
 * @version 2.0
 */
public class SocketProductController implements ProductController {

	/** 连接。 */
	private final SocketConnection connection;

	/**
	 * 连接默认服务器。
	 */
	public SocketProductController() {
		this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
	}

	/**
	 * 连接指定服务器。
	 *
	 * @param host 地址
	 * @param port 端口
	 */
	public SocketProductController(String host, int port) {
		connection = new SocketConnection(host, port);
	}

	@SuppressWarnings("unchecked")
	public List<String> loadCategories() throws IOException, ApiException {
		Result<?> result = connection.call(Protocol.OP_GET_CATEGORIES);
		return castList(result.getData());
	}

	public List<ProductDto> loadProducts(String category) throws IOException, ApiException {
		String target = category == null || category.trim().isEmpty() ? ALL_CATEGORIES : category;
		Result<?> result;
		if (ALL_CATEGORIES.equals(target)) {
			result = connection.call(Protocol.OP_GET_ALL_PRODUCTS);
		} else {
			result = connection.call(Protocol.OP_GET_PRODUCTS, target);
		}
		return castProducts(result.getData());
	}

	public List<ProductDto> searchProducts(String keyword) throws IOException, ApiException {
		String target = keyword == null ? "" : keyword;
		Result<?> result = connection.call(Protocol.OP_SEARCH_PRODUCTS, target);
		return castProducts(result.getData());
	}

	/**
	 * 把响应数据转成商品列表。
	 *
	 * @param data 响应数据
	 * @return 商品列表
	 * @throws ApiException 类型不符
	 */
	@SuppressWarnings("unchecked")
	private static List<ProductDto> castProducts(Object data) throws ApiException {
		if (!(data instanceof List)) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "响应不是商品列表");
		}
		return new ArrayList<ProductDto>((List<ProductDto>) data);
	}

	/**
	 * 把响应数据转成字符串列表。
	 *
	 * @param data 响应数据
	 * @return 字符串列表
	 * @throws ApiException 类型不符
	 */
	@SuppressWarnings("unchecked")
	private static List<String> castList(Object data) throws ApiException {
		if (!(data instanceof List)) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "响应不是分类列表");
		}
		return new ArrayList<String>((List<String>) data);
	}

	public void close() {
		connection.close();
	}

	/**
	 * 连接是否可用。
	 *
	 * @return 可用返回 true
	 */
	public boolean isConnected() {
		return connection.isConnected();
	}
}
