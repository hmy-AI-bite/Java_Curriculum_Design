package com.ascent.service;

import com.ascent.common.AppException;
import com.ascent.common.ErrorCode;
import com.ascent.common.Role;
import com.ascent.common.dto.LoginRequest;
import com.ascent.common.dto.RegisterRequest;
import com.ascent.common.dto.UserDTO;
import com.ascent.dao.UserDao;
import com.ascent.dao.UserRecord;

import java.sql.SQLException;
import java.util.Optional;

/**
 * 登录/注册业务逻辑。
 *
 * <p>提示：Service 层负责“校验 + 业务规则 + 事务边界”，
 * 不负责 Socket、不负责 Swing、也不直接拼 SQL。</p>
 *
 * <p>W1 的验收点：
 * <ul>
 *   <li>登录失败不能提示“用户名不存在”，统一提示“账号或密码错误”。</li>
 *   <li>注册必须服务端再校验长度和重复用户名。</li>
 *   <li>密码只以 PBKDF2 哈希 + 盐的形式入库。</li>
 * </ul>
 */
public final class AuthService {

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 20;
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 64;

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserDTO login(LoginRequest request) throws AppException {
        if (request == null
                || isBlank(request.getUsername())
                || request.getPassword() == null
                || request.getPassword().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION, "请输入账号和密码");
        }

        String username = request.getUsername().trim();
        try {
            Optional<UserRecord> found = userDao.findByUsername(username);
            if (!found.isPresent()) {
                // 故意不区分“用户不存在”和“密码错误”
                throw new AppException(ErrorCode.AUTH_FAILED, "账号或密码错误");
            }
            UserRecord user = found.get();
            if (!PasswordUtil.verify(request.getPassword(), user.getSalt(), user.getPasswordHash())) {
                throw new AppException(ErrorCode.AUTH_FAILED, "账号或密码错误");
            }
            return toDto(user);
        } catch (SQLException e) {
            throw new AppException(ErrorCode.SERVER_ERROR, "数据库访问失败", e);
        }
    }

    public UserDTO register(RegisterRequest request) throws AppException {
        String username = (request == null || request.getUsername() == null)
                ? "" : request.getUsername().trim();
        String password = (request == null) ? null : request.getPassword();

        validateUsername(username);
        validatePassword(password);

        try {
            if (userDao.exists(username)) {
                throw new AppException(ErrorCode.VALIDATION, "用户名已存在");
            }
            String salt = PasswordUtil.newSalt();
            String hash = PasswordUtil.hash(password, salt);
            long id = userDao.insert(username, hash, salt, Role.USER);
            return new UserDTO(id, username, Role.USER);
        } catch (SQLException e) {
            // 并发注册时可能撞上 UNIQUE 约束，这里做一次兜底提示
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new AppException(ErrorCode.VALIDATION, "用户名已存在");
            }
            throw new AppException(ErrorCode.SERVER_ERROR, "数据库访问失败", e);
        }
    }

    private void validateUsername(String username) throws AppException {
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            throw new AppException(ErrorCode.VALIDATION,
                    "用户名长度应为 " + USERNAME_MIN + "~" + USERNAME_MAX + " 个字符");
        }
        if (username.contains(",") || username.contains(" ")) {
            throw new AppException(ErrorCode.VALIDATION, "用户名不能包含逗号或空格");
        }
    }

    private void validatePassword(String password) throws AppException {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new AppException(ErrorCode.VALIDATION,
                    "密码长度应为 " + PASSWORD_MIN + "~" + PASSWORD_MAX + " 个字符");
        }
    }

    private UserDTO toDto(UserRecord record) {
        return new UserDTO(record.getId(), record.getUsername(), record.getRole());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
