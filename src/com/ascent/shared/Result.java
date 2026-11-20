package com.ascent.shared;

import java.io.Serializable;

/**
 * 统一响应信封。
 *
 * <p>
 * 服务端处理任何请求后都只回这一个对象，不再直接把业务对象写回 socket。
 * 好处是客户端只有一条解析路径，服务端异常也不会把连接搞断。
 * </p>
 *
 * <p>
 * 客户端和服务器必须使用同一份 {@code com.ascent.shared} 包，否则 Java
 * 反序列化会因为类名或 serialVersionUID 不同而失败。
 * </p>
 *
 * @param <T> 业务数据类型
 * @author ascent
 * @version 2.0
 */
public final class Result<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 是否成功。 */
	private final boolean ok;

	/** 成功时的数据，失败时为 null。 */
	@SuppressWarnings("serial")
	private final T data;

	/** 失败时的错误码，见 {@link ErrorCode}。 */
	private final String errorCode;

	/** 失败时的可读提示，可直接展示给用户。 */
	private final String message;

	/**
	 * 私有构造，统一走静态工厂。
	 *
	 * @param ok        是否成功
	 * @param data      数据
	 * @param errorCode 错误码
	 * @param message   提示
	 */
	private Result(boolean ok, T data, String errorCode, String message) {
		this.ok = ok;
		this.data = data;
		this.errorCode = errorCode;
		this.message = message;
	}

	/**
	 * 构造成功结果。
	 *
	 * @param data 业务数据
	 * @param <T>  数据类型
	 * @return 成功结果
	 */
	public static <T> Result<T> ok(T data) {
		return new Result<T>(true, data, null, null);
	}

	/**
	 * 构造无数据的成功结果。
	 *
	 * @return 成功结果
	 */
	public static Result<Void> ok() {
		return new Result<Void>(true, null, null, null);
	}

	/**
	 * 构造失败结果。
	 *
	 * @param errorCode 错误码，建议取 {@link ErrorCode} 中的常量
	 * @param message   可读提示
	 * @param <T>       数据类型
	 * @return 失败结果
	 */
	public static <T> Result<T> fail(String errorCode, String message) {
		return new Result<T>(false, null, errorCode, message);
	}

	/**
	 * 是否成功。
	 *
	 * @return 成功返回 true
	 */
	public boolean isOk() {
		return ok;
	}

	/**
	 * 取数据。
	 *
	 * @return 成功时的数据，失败时为 null
	 */
	public T getData() {
		return data;
	}

	/**
	 * 取错误码。
	 *
	 * @return 失败时的错误码，成功时为 null
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * 取提示。
	 *
	 * @return 失败时的提示，成功时为 null
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return ok ? "Result{ok, data=" + data + "}"
				: "Result{fail, code=" + errorCode + ", message=" + message + "}";
	}
}
