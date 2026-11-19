package com.ascent.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * 密码哈希工具：PBKDF2WithHmacSHA256 + 随机盐。
 *
 * <p>提示：这是 W1 的安全重点，答辩时容易被问：
 * <ul>
 *   <li>为什么不能存明文？—— 数据库泄露后直接拿到所有密码。</li>
 *   <li>为什么要加盐？—— 相同密码得到不同哈希，防止彩虹表。</li>
 *   <li>为什么用 PBKDF2 而不是 MD5/SHA？—— 慢哈希，暴力破解成本高。</li>
 *   <li>为什么用 MessageDigest.isEqual？—— 常量时间比较，避免计时攻击。</li>
 * </ul>
 *
 * <p>课程设计参数：迭代 120000 次、盐 16 字节、输出 256 位。
 * 真实生产环境还需要根据机器性能调整迭代次数。</p>
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // 工具类不允许实例化
    }

    /** 生成 Base64 编码的随机盐。 */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** 用盐计算密码哈希，返回 Base64 字符串。 */
    public static String hash(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("password/salt 不能为空");
        }
        char[] chars = password.toCharArray();
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] encoded = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // 当前 JDK 一定支持 PBKDF2WithHmacSHA256；这里属于环境错误
            throw new IllegalStateException("密码哈希失败", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(chars, '\0');
        }
    }

    /** 校验密码；比较使用常量时间方法。 */
    public static boolean verify(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        String actual = hash(password, salt);
        return MessageDigest.isEqual(
                actual.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expectedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
