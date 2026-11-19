package com.ascent.common;

/**
 * 客户端/服务端约定的操作码。
 *
 * <p>提示：操作码是协议的一部分，改动必须同时通知前端；
 * 不要复用已有编号，新增功能往后加。</p>
 */
public final class OpCode {

    /** 登录：payload = LoginRequest，data = UserDTO */
    public static final int LOGIN = 1;

    /** 注册：payload = RegisterRequest，data = UserDTO */
    public static final int REGISTER = 2;

    /** 商品列表/搜索：payload = ProductQuery，data = List<ProductDTO>（W2） */
    public static final int LIST_PRODUCTS = 10;

    /** 商品详情：payload = Integer productId，data = ProductDTO（W2） */
    public static final int GET_PRODUCT = 11;

    /** 创建订单：payload = CreateOrderRequest，data = OrderDTO（W2） */
    public static final int CREATE_ORDER = 20;

    /** 我的订单：payload = null，data = List<OrderDTO>（W2） */
    public static final int LIST_ORDERS = 21;

    /** 管理员保存商品：payload = ProductDTO，data = ProductDTO（W2） */
    public static final int ADMIN_SAVE_PRODUCT = 30;

    /** 管理员删除商品：payload = Integer productId，data = Boolean（W2） */
    public static final int ADMIN_DELETE_PRODUCT = 31;

    private OpCode() {
        // 工具类不允许实例化
    }
}
