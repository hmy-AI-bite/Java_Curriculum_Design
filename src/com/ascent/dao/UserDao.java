package com.ascent.dao;

import com.ascent.common.Role;
import com.ascent.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * 用户表 DAO。
 *
 * <p>提示：所有 SQL 都用 PreparedStatement 的 ? 占位符，
 * 不要用字符串拼接，否则会有 SQL 注入。
 * try-with-resources 保证 Connection/PreparedStatement/ResultSet 自动关闭。</p>
 */
public final class UserDao {

    private final Database database;

    public UserDao(Database database) {
        this.database = database;
    }

    public Optional<UserRecord> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, salt, role FROM users WHERE username = ?";
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        }
    }

    public boolean exists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 插入用户并返回自增主键。
     *
     * <p>提示：username 有 UNIQUE 约束，重复插入会抛 SQLException；
     * Service 层应先 exists 检查，并处理并发下的唯一键冲突。</p>
     */
    public long insert(String username, String passwordHash, String salt, Role role)
            throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt, role) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, salt);
            ps.setString(4, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("插入用户后没有拿到主键");
            }
        }
    }

    public long count() throws SQLException {
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private UserRecord map(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                Role.valueOf(rs.getString("role")));
    }
}
