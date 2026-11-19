package com.ascent.common;

/**
 * 统一错误码。
 *
 * <p>提示：服务端返回给客户端的 message 要能直接给用户看，
 * 但不要把 SQL 异常、堆栈、数据库路径等内部信息返回给客户端。</p>
 */
public final class ErrorCode {

    public static final int OK = 0;

    /** 账号或密码错误（不要区分“账号不存在”和“密码错误”，避免撞库） */
    public static final int AUTH_FAILED = 1001;

    /** 没有权限（例如普通用户调用管理员接口） */
    public static final int FORBIDDEN = 1002;

    /** 参数校验失败 */
    public static final int VALIDATION = 1003;

    /** 数据不存在 */
    public static final int NOT_FOUND = 1004;

    /** 库存不足（W2 下单时使用） */
    public static final int STOCK_NOT_ENOUGH = 2001;

    /** 服务端内部错误 */
    public static final int SERVER_ERROR = 5000;

    /** W1 尚未实现的操作 */
    public static final int NOT_IMPLEMENTED = 5001;

    private ErrorCode() {
        // 工具类不允许实例化
    }
}
