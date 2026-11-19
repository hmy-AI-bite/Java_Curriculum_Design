package com.ascent.common.dto;

import com.ascent.common.Role;

import java.io.Serializable;

/**
 * 返回给客户端的用户信息。
 *
 * <p>提示：绝对不要把 password / passwordHash / salt 放进 DTO，
 * 客户端不需要，也不应该拿到。</p>
 */
public final class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String username;
    private final Role role;

    public UserDTO(long id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "UserDTO{id=" + id + ", username='" + username + "', role=" + role + '}';
    }
}
