package com.ascent.dao;

import com.ascent.common.Role;

/**
 * DAO 内部使用的用户行对象（包含密码哈希和盐，只允许服务端使用）。
 *
 * <p>提示：不要把这个对象直接返回给客户端，返回 UserDTO。</p>
 */
public final class UserRecord {

    private final long id;
    private final String username;
    private final String passwordHash;
    private final String salt;
    private final Role role;

    public UserRecord(long id, String username, String passwordHash, String salt, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public Role getRole() {
        return role;
    }
}
