package com.ascent.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ascent.shared.ApiException;
import com.ascent.shared.ErrorCode;
import com.ascent.shared.Protocol;
import com.ascent.shared.Result;
import com.ascent.shared.dto.CartItemDto;
import com.ascent.shared.dto.OrderDto;

/**
 * {@link OrderController} 的 socket 实现。
 *
 * @author ascent
 * @version 2.0
 */
public class SocketOrderController implements OrderController {

	/** 连接。 */
	private final SocketConnection connection;

	/**
	 * 连接默认服务器。
	 */
	public SocketOrderController() {
		this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
	}

	/**
	 * 连接指定服务器。
	 *
	 * @param host 地址
	 * @param port 端口
	 */
	public SocketOrderController(String host, int port) {
		connection = new SocketConnection(host, port);
	}

	public OrderDto placeOrder(String token, List<CartItemDto> items)
			throws IOException, ApiException {
		Result<?> result = connection.call(Protocol.OP_PLACE_ORDER, token,
				new ArrayList<CartItemDto>(items));
		return SocketConnection.require(result.getData(), OrderDto.class);
	}

	@SuppressWarnings("unchecked")
	public List<OrderDto> listMyOrders(String token) throws IOException, ApiException {
		Result<?> result = connection.call(Protocol.OP_LIST_ORDERS, token);
		Object data = result.getData();
		if (!(data instanceof List)) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "响应不是订单列表");
		}
		return new ArrayList<OrderDto>((List<OrderDto>) data);
	}

	public void close() {
		connection.close();
	}

	/**
	 * 连接是否可用。
	 *
	 * @return 可用返回 true
	 */
	public boolean isConnected() {
		return connection.isConnected();
	}
}
