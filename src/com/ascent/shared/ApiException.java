package com.ascent.shared;

/**
 * 业务异常：携带 {@link ErrorCode} 与可直接展示给用户的提示。
 *
 * <p>
 * 客户端在收到 {@code Result.fail(...)} 时抛出它；服务端 Service 层也用同一个
 * 异常表达“库存不足”“无权限”这类可预期失败，由 Handler 统一转成
 * {@code Result.fail}。这样两端只需要一种业务异常类型。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class ApiException extends Exception {

	private static final long serialVersionUID = 1L;

	/** 错误码，取值见 {@link ErrorCode}。 */
	private final String errorCode;

	/**
	 * 构造异常。
	 *
	 * @param errorCode 错误码
	 * @param message   可读提示
	 */
	public ApiException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * 构造异常，并保留底层原因。
	 *
	 * @param errorCode 错误码
	 * @param message   可读提示
	 * @param cause     底层原因
	 */
	public ApiException(String errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * 错误码。
	 *
	 * @return 错误码
	 */
	public String getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return "ApiException{" + errorCode + ": " + getMessage() + "}";
	}
}
