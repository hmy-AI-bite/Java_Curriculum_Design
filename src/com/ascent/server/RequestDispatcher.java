package com.ascent.server;

import com.ascent.common.AppException;
import com.ascent.common.ErrorCode;
import com.ascent.common.OpCode;
import com.ascent.common.dto.LoginRequest;
import com.ascent.common.dto.RegisterRequest;
import com.ascent.common.dto.UserDTO;
import com.ascent.common.protocol.Request;
import com.ascent.common.protocol.Response;
import com.ascent.service.AuthService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 请求分发器：把操作码翻译成 Service 调用。
 *
 * <p>提示：W1 只实现 LOGIN/REGISTER；
 * W2 在这里加 LIST_PRODUCTS/CREATE_ORDER 等 case，
 * 并在管理员操作前加 {@code if (!session.isAdmin())}。</p>
 *
 * <p>异常处理原则：业务异常返回可读 message；
 * 未知异常只返回“服务器内部错误”，堆栈写日志。</p>
 */
public final class RequestDispatcher {

    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());

    private final AuthService authService;

    public RequestDispatcher(AuthService authService) {
        this.authService = authService;
    }

    public Response dispatch(Request request, ClientSession session) {
        if (request == null) {
            return Response.error(ErrorCode.VALIDATION, "请求不能为空");
        }

        try {
            switch (request.getOp()) {
                case OpCode.LOGIN:
                    return handleLogin(request.getPayload(), session);
                case OpCode.REGISTER:
                    return handleRegister(request.getPayload());
                default:
                    // W2 的扩展点：商品、订单、管理端操作都从这里加
                    return Response.error(ErrorCode.NOT_IMPLEMENTED,
                            "操作码 " + request.getOp() + " 暂未实现（W2）");
            }
        } catch (AppException e) {
            return Response.error(e.getCode(), e.getMessage());
        } catch (ClassCastException e) {
            return Response.error(ErrorCode.VALIDATION, "请求参数类型不正确");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "处理请求失败: " + request, e);
            return Response.error(ErrorCode.SERVER_ERROR, "服务器内部错误");
        }
    }

    private Response handleLogin(Object payload, ClientSession session) throws AppException {
        if (!(payload instanceof LoginRequest)) {
            return Response.error(ErrorCode.VALIDATION, "登录参数类型不正确");
        }
        UserDTO user = authService.login((LoginRequest) payload);
        session.login(user);
        return Response.ok(user);
    }

    private Response handleRegister(Object payload) throws AppException {
        if (!(payload instanceof RegisterRequest)) {
            return Response.error(ErrorCode.VALIDATION, "注册参数类型不正确");
        }
        UserDTO user = authService.register((RegisterRequest) payload);
        return Response.ok("注册成功", user);
    }
}
