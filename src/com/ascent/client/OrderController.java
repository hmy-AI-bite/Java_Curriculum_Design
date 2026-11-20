package com.ascent.client;

import java.io.IOException;
import java.util.List;

import com.ascent.shared.ApiException;
import com.ascent.shared.dto.CartItemDto;
import com.ascent.shared.dto.OrderDto;

/**
 * 订单控制器。<b>所有方法都是阻塞的</b>，调用方负责放进后台线程。
 *
 * <p>
 * 下单只传 token 和购物车行；价格、库存由服务端重新校验，客户端传的单价
 * 不参与计算。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public interface OrderController {

	/**
	 * 提交订单。
	 *
	 * @param token 会话令牌
	 * @param items 购物车行，至少一条
	 * @return 生成的订单，含订单号和总额
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败，如库存不足（{@code STOCK_NOT_ENOUGH}）、购物车为空
	 */
	OrderDto placeOrder(String token, List<CartItemDto> items) throws IOException, ApiException;

	/**
	 * 查询当前用户的订单列表，按下单时间倒序。
	 *
	 * @param token 会话令牌
	 * @return 订单列表，没有订单时返回空列表
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败
	 */
	List<OrderDto> listMyOrders(String token) throws IOException, ApiException;

	/**
	 * 关闭底层连接。窗口关闭时必须调用。
	 */
	void close();
}
