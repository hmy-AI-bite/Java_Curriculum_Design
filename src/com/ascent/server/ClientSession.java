package com.ascent.server;

import com.ascent.common.Role;
import com.ascent.common.dto.UserDTO;

/**
 * 一个 Socket 连接对应的会话。
 *
 * <p>提示：登录成功后把 UserDTO 放进 Session；
 * 后续请求的权限判断看这里，不看客户端传的 role。
 * 每个 ClientHandler 一个 Session，不存在线程共享问题。</p>
 */
public final class ClientSession {

    private UserDTO user;

    public void login(UserDTO user) {
        this.user = user;
    }

    public void logout() {
        this.user = null;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public UserDTO getUser() {
        return user;
    }

    public boolean isAdmin() {
        return user != null && user.getRole() == Role.ADMIN;
    }
}
