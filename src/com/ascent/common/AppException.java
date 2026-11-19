package com.ascent.common;

/**
 * 业务异常：带错误码，服务端可以直接把 code/message 放进 Response。
 *
 * <p>提示：SQLException、IOException 这类技术异常不要直接抛给客户端，
 * 在 Service 层转换成 AppException(SERVER_ERROR, "服务器开小差了")。</p>
 */
public class AppException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int code;

    public AppException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AppException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
