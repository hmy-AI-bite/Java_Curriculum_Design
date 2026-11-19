package com.ascent.db;

import com.ascent.common.Role;
import com.ascent.dao.UserDao;
import com.ascent.dao.UserRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W1 迁移测试：用 UTF-8 临时文件模拟旧数据，验证编码、价格换算、密码哈希。
 *
 * <p>提示：这个测试是“编码问题”的回归测试。
 * 如果有人把 UTF-8 改成其他编码读取，这里会失败。</p>
 */
class DataMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyUtf8Data() throws Exception {
        Path productFile = tempDir.resolve("product.db");
        Path userFile = tempDir.resolve("user.db");

        Files.write(productFile, Arrays.asList(
                "产品1,177900-48-8,ss.jpg,C12H22N2O1,1100,280g,西药",
                "产品2,177901-48-8,ss.jpg,C12H22N2O2,280,100,保健药"
        ), StandardCharsets.UTF_8);

        Files.write(userFile, Arrays.asList(
                "user1,1,0"
        ), StandardCharsets.UTF_8);

        Database database = new Database(tempDir.resolve("migrate.db").toString());
        new SchemaInitializer(database).initialize();

        new DataMigration(database, productFile, userFile).migrateIfEmpty();

        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name, price_cents, stock, category FROM products ORDER BY id")) {
            assertTrue(rs.next());
            assertEquals("产品1", rs.getString("name"));
            assertEquals(110000L, rs.getLong("price_cents"));
            assertEquals(280, rs.getInt("stock")); // "280g" 取数字部分
            assertEquals("西药", rs.getString("category"));
        }

        UserDao userDao = new UserDao(database);
        Optional<UserRecord> user = userDao.findByUsername("user1");
        assertTrue(user.isPresent());
        assertEquals(Role.USER, user.get().getRole());
        assertTrue(user.get().getPasswordHash().length() > 20);
    }
}
