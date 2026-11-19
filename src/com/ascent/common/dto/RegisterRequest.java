package com.ascent.common.dto;

import java.io.Serializable;

/**
 * 注册请求参数。
 *
 * <p>提示：服务端必须重新校验用户名/密码长度和重复用户名，
 * 不能只依赖客户端校验。</p>
 */
public final class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "RegisterRequest{username='" + username + "'}";
    }
}
