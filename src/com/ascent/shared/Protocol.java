package com.ascent.shared;

/**
 * 协议常量：操作码与规则。
 *
 * <p>
 * 协议帧：客户端先写 {@code int opCode}，再按 {@link #payloadShape(int)} 约定的
 * 顺序写对象；服务端处理完只回一个 {@link Result}。
 * </p>
 *
 * <p>
 * 操作码分段：100~199 公开，200~299 需要登录，300~399 需要管理员。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public final class Protocol {

	/** 协议版本，写进日志和文档，便于排查两端不一致。 */
	public static final String VERSION = "2.0";

	/** 默认服务器地址。 */
	public static final String DEFAULT_HOST = "localhost";

	/** 默认服务器端口。 */
	public static final int DEFAULT_PORT = 5150;

	/** 连接超时（毫秒）。 */
	public static final int CONNECT_TIMEOUT_MILLIS = 3_000;

	/** 读超时（毫秒），超时按网络故障处理。 */
	public static final int READ_TIMEOUT_MILLIS = 10_000;

	/** 查分类：无 payload，返回 {@code List<String>}。 */
	public static final int OP_GET_CATEGORIES = 100;

	/** 按分类查商品：{@code String category}，返回 {@code List<ProductDto>}。 */
	public static final int OP_GET_PRODUCTS = 101;

	/** 登录：{@code String username, String password}，返回 {@code LoginResult}。 */
	public static final int OP_LOGIN = 102;

	/** 注册：{@code String username, String password}，返回 {@code Boolean}。 */
	public static final int OP_REGISTER = 103;

	/** 查全部商品：无 payload，返回 {@code List<ProductDto>}。 */
	public static final int OP_GET_ALL_PRODUCTS = 104;

	/** 搜索商品：{@code String keyword}，返回 {@code List<ProductDto>}。 */
	public static final int OP_SEARCH_PRODUCTS = 105;

	/** 下订单：{@code String token, List<CartItemDto> items}，返回 {@code OrderDto}。 */
	public static final int OP_PLACE_ORDER = 200;

	/** 我的订单：{@code String token}，返回 {@code List<OrderDto>}。 */
	public static final int OP_LIST_ORDERS = 201;

	/** 管理端新增商品：{@code String token, ProductDto product}，返回 {@code ProductDto}。 */
	public static final int OP_ADMIN_CREATE_PRODUCT = 300;

	/** 管理端修改商品：{@code String token, ProductDto product}，返回 {@code Boolean}。 */
	public static final int OP_ADMIN_UPDATE_PRODUCT = 301;

	/** 管理端删除商品：{@code String token, int productId}，返回 {@code Boolean}。 */
	public static final int OP_ADMIN_DELETE_PRODUCT = 302;

	/**
	 * 工具类不允许实例化。
	 */
	private Protocol() {
	}

	/**
	 * 是否属于公开接口（不需要登录）。
	 *
	 * @param opCode 操作码
	 * @return 公开接口返回 true
	 */
	public static boolean isPublic(int opCode) {
		return opCode >= 100 && opCode < 200;
	}

	/**
	 * 是否属于登录后接口。
	 *
	 * @param opCode 操作码
	 * @return 需要登录返回 true
	 */
	public static boolean requiresLogin(int opCode) {
		return opCode >= 200 && opCode < 300;
	}

	/**
	 * 是否属于管理员接口。
	 *
	 * @param opCode 操作码
	 * @return 需要管理员权限返回 true
	 */
	public static boolean requiresAdmin(int opCode) {
		return opCode >= 300 && opCode < 400;
	}

	/**
	 * 操作码是否已定义。
	 *
	 * @param opCode 操作码
	 * @return 已定义返回 true
	 */
	public static boolean isKnown(int opCode) {
		return isPublic(opCode) || requiresLogin(opCode) || requiresAdmin(opCode);
	}

	/**
	 * 返回操作码对应的名字，用于日志。
	 *
	 * @param opCode 操作码
	 * @return 可读名称
	 */
	public static String nameOf(int opCode) {
		switch (opCode) {
		case OP_GET_CATEGORIES:
			return "GET_CATEGORIES";
		case OP_GET_PRODUCTS:
			return "GET_PRODUCTS";
		case OP_LOGIN:
			return "LOGIN";
		case OP_REGISTER:
			return "REGISTER";
		case OP_GET_ALL_PRODUCTS:
			return "GET_ALL_PRODUCTS";
		case OP_SEARCH_PRODUCTS:
			return "SEARCH_PRODUCTS";
		case OP_PLACE_ORDER:
			return "PLACE_ORDER";
		case OP_LIST_ORDERS:
			return "LIST_ORDERS";
		case OP_ADMIN_CREATE_PRODUCT:
			return "ADMIN_CREATE_PRODUCT";
		case OP_ADMIN_UPDATE_PRODUCT:
			return "ADMIN_UPDATE_PRODUCT";
		case OP_ADMIN_DELETE_PRODUCT:
			return "ADMIN_DELETE_PRODUCT";
		default:
			return "UNKNOWN(" + opCode + ")";
		}
	}
}
