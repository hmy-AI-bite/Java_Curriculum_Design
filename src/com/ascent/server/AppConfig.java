package com.ascent.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 读取 classpath 下的 /app.properties。
 *
 * <p>提示：端口、数据库路径不要硬编码在代码里；
 * 但 W1 先不搞环境变量/多环境配置，够用即可。</p>
 */
public final class AppConfig {

    private final Properties props = new Properties();

    private AppConfig() {
    }

    public static AppConfig load() throws IOException {
        AppConfig config = new AppConfig();
        // 优先 classpath；在 IDE 里直接 Run 时回退到 src/resources/app.properties
        InputStream in = AppConfig.class.getResourceAsStream("/app.properties");
        if (in == null) {
            Path fallback = Paths.get("src", "resources", "app.properties");
            if (Files.exists(fallback)) {
                in = Files.newInputStream(fallback);
            }
        }
        if (in == null) {
            throw new IOException("找不到 app.properties（classpath:/app.properties 或 ./src/resources/app.properties）");
        }
        try (InputStream stream = in) {
            config.props.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return config;
    }

    public int getPort() {
        return Integer.parseInt(get("server.port", "5150"));
    }

    public String getDbPath() {
        return get("db.path", "./data/ascent.db");
    }

    public String getLegacyProduct() {
        return get("legacy.product", "./product.db");
    }

    public String getLegacyUser() {
        return get("legacy.user", "./user.db");
    }

    private String get(String key, String defaultValue) {
        String value = props.getProperty(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }
}
