package com.ascent.shared;

/**
 * 错误码常量。
 *
 * <p>
 * 用字符串常量而不是枚举：跨版本序列化更稳，客户端加新码也不用改共享类。
 * 错误码是协议的一部分，<b>新增可以，改名必须两端同步发版</b>。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public final class ErrorCode {

	/** 账号或密码错误。 */
	public static final String AUTH_FAILED = "AUTH_FAILED";

	/** 未登录或 token 失效。 */
	public static final String UNAUTHENTICATED = "UNAUTHENTICATED";

	/** 没有权限执行该操作。 */
	public static final String PERMISSION_DENIED = "PERMISSION_DENIED";

	/** 注册时用户名已存在。 */
	public static final String USER_EXISTS = "USER_EXISTS";

	/** 请求参数不合法（空账号、空密码、数量非正数等）。 */
	public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

	/** 商品不存在。 */
	public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";

	/** 库存不足。 */
	public static final String STOCK_NOT_ENOUGH = "STOCK_NOT_ENOUGH";

	/** 订单不存在。 */
	public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

	/** 购物车为空。 */
	public static final String EMPTY_CART = "EMPTY_CART";

	/** 数据库错误。 */
	public static final String DB_ERROR = "DB_ERROR";

	/** 服务端内部错误。 */
	public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

	/** 未知操作码。 */
	public static final String UNKNOWN_OP = "UNKNOWN_OP";

	/** 客户端本地错误：网络不通（不会由服务端返回）。 */
	public static final String NETWORK_ERROR = "NETWORK_ERROR";

	/** 客户端本地错误：请求超时（不会由服务端返回）。 */
	public static final String TIMEOUT = "TIMEOUT";

	/**
	 * 工具类不允许实例化。
	 */
	private ErrorCode() {
	}
}
