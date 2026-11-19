package com.ascent.common.protocol;

import com.ascent.common.ErrorCode;

import java.io.Serializable;

/**
 * 服务端响应：code=0 表示成功，data 放业务数据。
 *
 * <p>提示：错误时 message 是给用户看的；不要放异常堆栈。</p>
 */
public final class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final Object data;

    private Response(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Response ok(Object data) {
        return new Response(ErrorCode.OK, "OK", data);
    }

    public static Response ok(String message, Object data) {
        return new Response(ErrorCode.OK, message, data);
    }

    public static Response error(int code, String message) {
        return new Response(code, message, null);
    }

    public boolean isOk() {
        return code == ErrorCode.OK;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Response{code=" + code + ", message='" + message + "', data=" + data + '}';
    }
}
