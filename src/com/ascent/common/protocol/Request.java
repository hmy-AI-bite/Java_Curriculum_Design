package com.ascent.common.protocol;

import java.io.Serializable;

/**
 * 客户端请求：一个操作码 + 一个参数对象。
 *
 * <p>提示：payload 必须是 Serializable；W1 只用到 LoginRequest/RegisterRequest。
 * 类结构一旦发布就不要随意改字段，否则客户端/服务端序列化不兼容。</p>
 */
public final class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int op;
    private final Object payload;

    public Request(int op, Object payload) {
        this.op = op;
        this.payload = payload;
    }

    public int getOp() {
        return op;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Request{op=" + op + ", payload=" + payload + '}';
    }
}
