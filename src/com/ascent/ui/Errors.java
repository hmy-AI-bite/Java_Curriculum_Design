package com.ascent.ui;

import com.ascent.shared.ApiException;

/**
 * 把异常翻译成能直接显示给用户的话。
 *
 * @author ascent
 * @version 2.0
 */
public final class Errors {

	/**
	 * 工具类不允许实例化。
	 */
	private Errors() {
	}

	/**
	 * 生成提示文本。
	 *
	 * @param error 异常
	 * @return 提示
	 */
	public static String friendly(Exception error) {
		if (error == null) {
			return "未知错误";
		}
		if (error instanceof ApiException) {
			String message = error.getMessage();
			return message == null || message.isEmpty() ? "操作失败" : message;
		}
		String message = error.getMessage() == null ? "" : error.getMessage();
		if (message.contains("请求超时")) {
			return "服务端响应超时，请稍后重试。";
		}
		if (message.contains("无法连接服务器")) {
			return "无法连接服务器，请确认已启动服务端（MockServer 或正式服务端）。";
		}
		if (message.contains("Connection refused")) {
			return "服务端未启动（localhost:5150）。";
		}
		if (message.contains("Connection reset") || message.contains("Broken pipe")) {
			return "与服务端的连接中断，请重试。";
		}
		return message.isEmpty() ? "网络异常，请稍后重试。" : message;
	}
}
