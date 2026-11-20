package com.ascent.shared.dto;

import java.io.Serializable;

/**
 * 用户信息，<b>不包含密码</b>。
 *
 * <p>
 * 这个类会跨网络传输并可能被显示在界面上，绝对不要往里加 password 字段。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class UserDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 用户主键。 */
	private int id;

	/** 登录账号。 */
	private String username;

	/** 权限：0 普通用户，1 管理员。 */
	private int authority;

	/**
	 * 默认构造，反序列化需要。
	 */
	public UserDto() {
	}

	/**
	 * 全参数构造。
	 *
	 * @param id        主键
	 * @param username  账号
	 * @param authority 权限
	 */
	public UserDto(int id, String username, int authority) {
		this.id = id;
		this.username = username;
		this.authority = authority;
	}

	/**
	 * 是否管理员。
	 *
	 * @return 管理员返回 true
	 */
	public boolean isAdmin() {
		return authority == 1;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getAuthority() {
		return authority;
	}

	public void setAuthority(int authority) {
		this.authority = authority;
	}

	@Override
	public String toString() {
		return "UserDto{id=" + id + ", username=" + username + ", authority=" + authority + "}";
	}
}
