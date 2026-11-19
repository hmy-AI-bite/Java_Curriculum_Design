package com.ascent.service;

import com.ascent.common.AppException;
import com.ascent.common.ErrorCode;
import com.ascent.common.Role;
import com.ascent.common.dto.LoginRequest;
import com.ascent.common.dto.RegisterRequest;
import com.ascent.common.dto.UserDTO;
import com.ascent.dao.UserDao;
import com.ascent.dao.UserRecord;
import com.ascent.db.Database;
import com.ascent.db.SchemaInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W1 登录/注册测试：真实 SQLite（临时文件），不是 mock。
 */
class AuthServiceTest {

    @TempDir
    Path tempDir;

    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        Database database = new Database(tempDir.resolve("auth-test.db").toString());
        new SchemaInitializer(database).initialize();
        userDao = new UserDao(database);
        authService = new AuthService(userDao);
    }

    @Test
    void registerThenLogin() throws Exception {
        UserDTO registered = authService.register(new RegisterRequest("alice", "secret123"));
        assertEquals("alice", registered.getUsername());
        assertEquals(Role.USER, registered.getRole());

        UserDTO logged = authService.login(new LoginRequest("alice", "secret123"));
        assertEquals(registered.getId(), logged.getId());
    }

    @Test
    void passwordIsNotStoredInPlainText() throws Exception {
        authService.register(new RegisterRequest("bob", "secret123"));
        Optional<UserRecord> record = userDao.findByUsername("bob");

        assertTrue(record.isPresent());
        assertNotEquals("secret123", record.get().getPasswordHash());
        assertFalse(record.get().getSalt().isEmpty());
        assertTrue(PasswordUtil.verify("secret123", record.get().getSalt(),
                record.get().getPasswordHash()));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        authService.register(new RegisterRequest("carol", "secret123"));

        AppException ex = assertThrows(AppException.class,
                () -> authService.register(new RegisterRequest("carol", "another123")));
        assertEquals(ErrorCode.VALIDATION, ex.getCode());
    }

    @Test
    void wrongPasswordGivesGenericAuthError() throws Exception {
        authService.register(new RegisterRequest("dave", "secret123"));

        AppException ex = assertThrows(AppException.class,
                () -> authService.login(new LoginRequest("dave", "wrong")));
        assertEquals(ErrorCode.AUTH_FAILED, ex.getCode());
        assertEquals("账号或密码错误", ex.getMessage());
    }

    @Test
    void unknownUserGivesSameGenericError() {
        AppException ex = assertThrows(AppException.class,
                () -> authService.login(new LoginRequest("nobody", "secret123")));
        assertEquals(ErrorCode.AUTH_FAILED, ex.getCode());
        assertEquals("账号或密码错误", ex.getMessage());
    }

    @Test
    void shortPasswordIsRejected() {
        AppException ex = assertThrows(AppException.class,
                () -> authService.register(new RegisterRequest("eve", "123")));
        assertEquals(ErrorCode.VALIDATION, ex.getCode());
    }
}
