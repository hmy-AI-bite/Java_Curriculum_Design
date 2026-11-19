package com.ascent.common.dto;

import java.io.Serializable;

/**
 * 登录请求参数。
 *
 * <p>提示：真实项目里密码可以先用 char[] 接收，校验完立即清空；
 * 课程设计用 String 也可以，但不要在日志里打印 password。</p>
 */
public final class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
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
        // 注意：不要输出 password
        return "LoginRequest{username='" + username + "'}";
    }
}
