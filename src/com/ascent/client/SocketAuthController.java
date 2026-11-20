package com.ascent.client;

import java.io.IOException;

import com.ascent.shared.ApiException;
import com.ascent.shared.Protocol;
import com.ascent.shared.Result;
import com.ascent.shared.dto.LoginResult;

/**
 * {@link AuthController} 的 socket 实现。
 *
 * @author ascent
 * @version 2.0
 */
public class SocketAuthController implements AuthController {

	/** 连接。 */
	private final SocketConnection connection;

	/**
	 * 连接默认服务器。
	 */
	public SocketAuthController() {
		this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
	}

	/**
	 * 连接指定服务器。
	 *
	 * @param host 地址
	 * @param port 端口
	 */
	public SocketAuthController(String host, int port) {
		connection = new SocketConnection(host, port);
	}

	public LoginResult login(String username, String password) throws IOException, ApiException {
		Result<?> result = connection.call(Protocol.OP_LOGIN, username, password);
		return SocketConnection.require(result.getData(), LoginResult.class);
	}

	public boolean register(String username, String password) throws IOException, ApiException {
		Result<?> result = connection.call(Protocol.OP_REGISTER, username, password);
		return Boolean.TRUE.equals(SocketConnection.require(result.getData(), Boolean.class));
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
