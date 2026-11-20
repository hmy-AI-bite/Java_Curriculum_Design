package com.ascent.shared.dto;

import java.io.Serializable;

/**
 * 登录成功后返回的会话信息。
 *
 * <p>
 * 之后所有需要登录的请求都要带上 {@link #token}。服务端根据 token 反查用户，
 * <b>不要</b>再让客户端传 userId 或 authority——那样普通用户可以直接冒充管理员。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class LoginResult implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 会话令牌，服务端生成。 */
	private String token;

	/** 当前用户。 */
	private UserDto user;

	/**
	 * 默认构造，反序列化需要。
	 */
	public LoginResult() {
	}

	/**
	 * 全参数构造。
	 *
	 * @param token 会话令牌
	 * @param user  当前用户
	 */
	public LoginResult(String token, UserDto user) {
		this.token = token;
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public UserDto getUser() {
		return user;
	}

	public void setUser(UserDto user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "LoginResult{token=" + (token == null ? "null" : token.substring(0, Math.min(8, token.length())) + "...")
				+ ", user=" + user + "}";
	}
}
