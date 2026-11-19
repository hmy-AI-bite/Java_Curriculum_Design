package com.ascent.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W1 安全点测试：哈希、盐、校验。
 */
class PasswordUtilTest {

    @Test
    void hashAndVerify() {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash("secret123", salt);

        assertTrue(PasswordUtil.verify("secret123", salt, hash));
        assertFalse(PasswordUtil.verify("wrong-password", salt, hash));
    }

    @Test
    void samePasswordWithDifferentSaltGivesDifferentHash() {
        String salt1 = PasswordUtil.newSalt();
        String salt2 = PasswordUtil.newSalt();
        String hash1 = PasswordUtil.hash("secret123", salt1);
        String hash2 = PasswordUtil.hash("secret123", salt2);

        assertNotEquals(salt1, salt2);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void nullInputsAreRejected() {
        assertFalse(PasswordUtil.verify(null, "salt", "hash"));
        assertFalse(PasswordUtil.verify("pwd", null, "hash"));
        assertFalse(PasswordUtil.verify("pwd", "salt", null));
    }
}
