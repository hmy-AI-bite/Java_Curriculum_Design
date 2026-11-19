package com.ascent.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 执行 db/schema.sql，建表 + 建索引。
 *
 * <p>提示：SQL 文件在 Maven 构建时会被复制到 classpath 的 /db/schema.sql，
 * 所以这里用 getResourceAsStream 读，不能写死 Windows 绝对路径。</p>
 *
 * <p>W1 的做法是“启动时建表”，适合课程设计；
 * 真实项目应该用 Flyway/Liquibase 做版本化迁移。</p>
 */
public final class SchemaInitializer {

    private final Database database;

    public SchemaInitializer(Database database) {
        this.database = database;
    }

    public void initialize() throws SQLException {
        String script = readSchema();
        // 先去掉 -- 注释行，再按分号拆分；schema.sql 里没有存储过程，简单拆分即可
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                cleaned.append(line).append('\n');
            }
        }

        try (Connection connection = database.open();
             Statement statement = connection.createStatement()) {
            for (String sql : cleaned.toString().split(";")) {
                if (!sql.trim().isEmpty()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private String readSchema() {
        // 优先 classpath（Maven 打包后）；在 IDE 里直接 Run 时回退到项目根目录的 db/schema.sql
        InputStream in = SchemaInitializer.class.getResourceAsStream("/db/schema.sql");
        try {
            if (in == null) {
                Path fallback = Paths.get("db", "schema.sql");
                if (Files.exists(fallback)) {
                    in = Files.newInputStream(fallback);
                }
            }
            if (in == null) {
                throw new IllegalStateException(
                        "找不到 schema.sql（classpath:/db/schema.sql 或 ./db/schema.sql）");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("读取 schema.sql 失败", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // 读取失败时关闭异常不覆盖主异常
                }
            }
        }
    }
}
