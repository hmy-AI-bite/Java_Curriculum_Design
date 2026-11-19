package com.ascent.server;

import com.ascent.common.Role;
import com.ascent.dao.UserDao;
import com.ascent.db.DataMigration;
import com.ascent.db.Database;
import com.ascent.db.SchemaInitializer;
import com.ascent.service.AuthService;
import com.ascent.service.PasswordUtil;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * 服务端启动入口（W1）。
 *
 * <p>启动顺序：
 * <ol>
 *   <li>读 app.properties；</li>
 *   <li>建表；</li>
 *   <li>第一次启动时把旧 UTF-8 文本迁到 SQLite；</li>
 *   <li>确保有管理员账号；</li>
 *   <li>监听 5150，每个连接交给线程池。</li>
 * </ol>
 *
 * <p>提示：W1 只绑回环地址（127.0.0.1），避免把没有鉴权的服务暴露到局域网；
 * 如果确实需要别人连，再改成 0.0.0.0，并先完成权限控制。</p>
 */
public final class ServerMain {

    private static final Logger LOG = Logger.getLogger(ServerMain.class.getName());
    private static final int THREADS = 8;

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        Database database = new Database(config.getDbPath());

        new SchemaInitializer(database).initialize();
        new DataMigration(
                database,
                Paths.get(config.getLegacyProduct()),
                Paths.get(config.getLegacyUser()))
                .migrateIfEmpty();
        seedAdmin(database);

        AuthService authService = new AuthService(new UserDao(database));
        RequestDispatcher dispatcher = new RequestDispatcher(authService);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        // 明确绑定 IPv4 回环，避免某些系统 getLoopbackAddress() 返回 ::1
        // 导致客户端用 127.0.0.1 连不上
        try (ServerSocket serverSocket = new ServerSocket(
                config.getPort(), 50, InetAddress.getByName("127.0.0.1"))) {

            LOG.info("AscentSys 服务端已启动: 127.0.0.1:" + config.getPort());
            LOG.info("数据库: " + database.getUrl());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("正在关闭服务端...");
                try {
                    serverSocket.close();
                } catch (Exception ignored) {
                    // 关闭阶段忽略
                }
                pool.shutdownNow();
            }));

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                pool.submit(new ClientHandler(socket, dispatcher));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 首次启动创建管理员 admin/admin123。
     *
     * <p>提示：这是课程设计的演示账号；真实项目应该让用户首次登录后强制改密码。
     * seed 只负责“没有 admin 时创建”，不会覆盖已有密码。</p>
     */
    private static void seedAdmin(Database database) throws SQLException {
        UserDao userDao = new UserDao(database);
        if (userDao.findByUsername("admin").isPresent()) {
            return;
        }
        String salt = PasswordUtil.newSalt();
        userDao.insert("admin", PasswordUtil.hash("admin123", salt), salt, Role.ADMIN);
        LOG.warning("已创建默认管理员: admin / admin123（请在实际使用前修改）");
    }
}
