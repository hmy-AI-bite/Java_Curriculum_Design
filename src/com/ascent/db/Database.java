package com.ascent.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 连接工厂。
 *
 * <p>提示：SQLite 是“嵌入式数据库”，不需要安装服务器，数据库就是一个文件。
 * 每次 DAO 操作都 open/close 一个连接，简单可靠；不要让多个线程共用一个 Connection。</p>
 *
 * <p>W1 只做最小封装；W2 如果要优化，可以引入连接池或把
 * {@code PRAGMA journal_mode=WAL} 打开，但课程设计阶段不建议过度设计。</p>
 */
public final class Database {

    private final String url;

    public Database(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            throw new IllegalArgumentException("db.path 不能为空");
        }
        if (":memory:".equals(dbPath)) {
            this.url = "jdbc:sqlite::memory:";
            return;
        }

        Path path = Paths.get(dbPath).toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException("无法创建数据库目录: " + parent, e);
            }
        }
        // Windows 路径的反斜杠在 JDBC URL 里不保险，统一转成 /
        this.url = "jdbc:sqlite:" + path.toString().replace('\\', '/');
    }

    /**
     * 打开连接，并开启外键约束。
     *
     * <p>提示：SQLite 默认不启用外键，必须在每个连接上执行
     * {@code PRAGMA foreign_keys = ON}，否则订单明细可以引用不存在的订单。</p>
     */
    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public String getUrl() {
        return url;
    }
}
