package com.ascent.db;

import com.ascent.common.Role;
import com.ascent.service.PasswordUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * 把旧版 UTF-8 文本数据迁移到 SQLite。
 *
 * <p>提示：这是 W1 最能体现“I/O + 编码 + JDBC”的一段代码：
 * <ul>
 *   <li>旧文件已统一为 UTF-8，读取时仍显式指定 StandardCharsets.UTF_8，
 *       不依赖系统默认编码；</li>
 *   <li>商品价格从“元”转成“分”，避免浮点误差；</li>
 *   <li>旧用户密码是明文，迁移时立即 hash，数据库里不留明文；</li>
 *   <li>整个迁移放在一个事务里，失败就回滚，避免只迁一半。</li>
 * </ul>
 *
 * <p>幂等：表里已有数据就跳过，重复启动不会重复插入。</p>
 */
public final class DataMigration {

    private static final Logger LOG = Logger.getLogger(DataMigration.class.getName());
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final Database database;
    private final Path productFile;
    private final Path userFile;

    public DataMigration(Database database, Path productFile, Path userFile) {
        this.database = database;
        this.productFile = productFile;
        this.userFile = userFile;
    }

    public void migrateIfEmpty() throws SQLException, IOException {
        migrateUsers();
        migrateProducts();
    }

    private void migrateUsers() throws SQLException, IOException {
        if (count("users") > 0) {
            LOG.info("users 表已有数据，跳过用户迁移");
            return;
        }
        if (userFile == null || !Files.exists(userFile)) {
            LOG.warning("找不到旧用户文件: " + userFile + "，跳过用户迁移");
            return;
        }

        String sql = "INSERT OR IGNORE INTO users (username, password_hash, salt, role) VALUES (?, ?, ?, ?)";
        int count = 0;
        try (Connection connection = database.open();
             BufferedReader reader = Files.newBufferedReader(userFile, UTF_8);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(line, ",");
                    if (st.countTokens() != 3) {
                        throw new IOException("user.db 第 " + lineNo + " 行不是 3 列: " + line);
                    }
                    String username = st.nextToken().trim();
                    String plainPassword = st.nextToken().trim();
                    int authority = Integer.parseInt(st.nextToken().trim());

                    String salt = PasswordUtil.newSalt();
                    ps.setString(1, username);
                    ps.setString(2, PasswordUtil.hash(plainPassword, salt));
                    ps.setString(3, salt);
                    // 旧数据 0=普通用户；这里把 1 也按管理员处理，保留原语义
                    ps.setString(4, authority == 1 ? Role.ADMIN.name() : Role.USER.name());
                    ps.addBatch();
                    count++;
                }
                ps.executeBatch();
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        LOG.info("用户迁移完成: " + count + " 条");
    }

    private void migrateProducts() throws SQLException, IOException {
        if (count("products") > 0) {
            LOG.info("products 表已有数据，跳过商品迁移");
            return;
        }
        if (productFile == null || !Files.exists(productFile)) {
            LOG.warning("找不到旧商品文件: " + productFile + "，跳过商品迁移");
            return;
        }

        String sql = "INSERT INTO products (name, cas, image, formula, price_cents, stock, category) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        int count = 0;
        try (Connection connection = database.open();
             BufferedReader reader = Files.newBufferedReader(productFile, UTF_8);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(line, ",");
                    if (st.countTokens() != 7) {
                        throw new IOException("product.db 第 " + lineNo + " 行不是 7 列: " + line);
                    }
                    String name = st.nextToken().trim();
                    String cas = st.nextToken().trim();
                    String image = st.nextToken().trim();
                    String formula = st.nextToken().trim();
                    String price = st.nextToken().trim();
                    String stock = st.nextToken().trim();
                    String category = st.nextToken().trim();

                    ps.setString(1, name);
                    ps.setString(2, cas);
                    ps.setString(3, image);
                    ps.setString(4, formula);
                    ps.setLong(5, toCents(price));
                    // 旧数据的库存可能带单位，例如 "280g"、"100ml"；
                    // 这里只取前面的整数，单位不进入数据库（W2 如有需要再加 stock_unit 列）
                    ps.setInt(6, parseStock(stock));
                    ps.setString(7, category);
                    ps.addBatch();
                    count++;
                }
                ps.executeBatch();
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        LOG.info("商品迁移完成: " + count + " 条");
    }

    /**
     * 元 -> 分。
     *
     * <p>提示：用 BigDecimal 而不是 Double.parseDouble，避免 19.99 * 100 = 1998.9999 的问题。</p>
     */
    private long toCents(String yuan) {
        return new BigDecimal(yuan)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * 解析旧数据的库存：允许 "280"、"280g"、"100ml" 这类写法，取前面的数字。
     *
     * <p>提示：如果旧数据格式不可控，应该在迁移时打印行号并跳过/报错，
     * 不要静默变成 0，否则会出现“有商品但库存为 0”的怪数据。</p>
     */
    private int parseStock(String stock) {
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("^(\\d+)").matcher(stock.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("无法解析库存: " + stock);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private long count(String table) throws SQLException {
        // table 只来自本类常量，不来自用户输入；真实项目也不要这样拼表名
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
