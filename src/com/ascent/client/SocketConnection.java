package com.ascent.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.ascent.shared.ApiException;
import com.ascent.shared.ErrorCode;
import com.ascent.shared.Protocol;
import com.ascent.shared.Result;

/**
 * v2 协议的 socket 连接：发操作码 + 参数，收 {@link Result}。
 *
 * <p>
 * 设计取舍：
 * </p>
 * <ul>
 * <li>所有方法 {@code synchronized}：一个控制器只被一个后台任务使用，串行化足够；</li>
 * <li>连接懒建立，断线后下次调用自动重连；</li>
 * <li>业务失败（{@code Result.fail}）抛 {@link ApiException}，<b>不断开连接</b>，
 * 因为服务端状态是好的；</li>
 * <li>IO 异常断开连接并抛出，界面提示“连接失败”，下次重试会重新连；</li>
 * <li>读超时由 {@code soTimeout} 控制，避免界面永远等待。</li>
 * </ul>
 *
 * @author ascent
 * @version 2.0
 */
public class SocketConnection {

	/** 服务器地址。 */
	private final String host;

	/** 服务器端口。 */
	private final int port;

	/** socket。 */
	private Socket socket;

	/** 输出流。 */
	private ObjectOutputStream out;

	/** 输入流。 */
	private ObjectInputStream in;

	/**
	 * 连接默认服务器。
	 */
	public SocketConnection() {
		this(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);
	}

	/**
	 * 连接指定服务器。
	 *
	 * @param host 地址
	 * @param port 端口
	 */
	public SocketConnection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 发一次请求并等待响应。
	 *
	 * @param opCode  操作码
	 * @param payload 参数，按协议约定顺序
	 * @return 服务端返回的结果（一定是成功的，失败已转成异常）
	 * @throws IOException  网络故障，连接已关闭
	 * @throws ApiException 业务失败，连接仍可用
	 */
	public synchronized Result<?> call(int opCode, Object... payload)
			throws IOException, ApiException {
		ensureConnected();
		try {
			// 清空对象句柄缓存：否则同一个 DTO 第二次发送会被写成引用，
			// 对端拿到的是第一次的旧值（例如库存永远不更新）。
			out.reset();
			out.writeInt(opCode);
			for (Object item : payload) {
				out.writeObject(item);
			}
			out.flush();

			Object response = in.readObject();
			if (!(response instanceof Result)) {
				throw new IOException("协议错误：响应不是 Result，实际是 "
						+ (response == null ? "null" : response.getClass().getName()));
			}
			Result<?> result = (Result<?>) response;
			if (!result.isOk()) {
				throw new ApiException(result.getErrorCode(), result.getMessage());
			}
			return result;
		} catch (ClassNotFoundException e) {
			closeQuietly();
			throw new IOException("无法识别的响应类型", e);
		} catch (SocketTimeoutException e) {
			closeQuietly();
			throw new IOException("请求超时（" + Protocol.READ_TIMEOUT_MILLIS + "ms）", e);
		} catch (IOException e) {
			closeQuietly();
			throw e;
		}
	}

	/**
	 * 确保连接可用，必要时重连。
	 *
	 * @throws IOException 连接失败
	 */
	private void ensureConnected() throws IOException {
		if (socket != null && socket.isConnected() && !socket.isClosed()) {
			return;
		}
		closeQuietly();
		Socket fresh = new Socket();
		try {
			fresh.connect(new InetSocketAddress(host, port), Protocol.CONNECT_TIMEOUT_MILLIS);
			fresh.setSoTimeout(Protocol.READ_TIMEOUT_MILLIS);
			fresh.setTcpNoDelay(true);
			// 顺序不能换：先写流头再读流头，两端对称
			ObjectOutputStream freshOut = new ObjectOutputStream(fresh.getOutputStream());
			freshOut.flush();
			ObjectInputStream freshIn = new ObjectInputStream(fresh.getInputStream());
			socket = fresh;
			out = freshOut;
			in = freshIn;
		} catch (IOException e) {
			try {
				fresh.close();
			} catch (IOException ignored) {
				// 忽略
			}
			throw new IOException("无法连接服务器 " + host + ":" + port, e);
		}
	}

	/**
	 * 关闭连接。重复调用安全。
	 */
	public synchronized void close() {
		closeQuietly();
	}

	/**
	 * 连接是否可用。
	 *
	 * @return 可用返回 true
	 */
	public synchronized boolean isConnected() {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	/**
	 * 静默关闭，忽略异常。
	 */
	private void closeQuietly() {
		if (in != null) {
			try {
				in.close();
			} catch (IOException ignored) {
				// 忽略
			}
			in = null;
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException ignored) {
				// 忽略
			}
			out = null;
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException ignored) {
				// 忽略
			}
			socket = null;
		}
	}

	/**
	 * 把结果里的数据转成期望类型。
	 *
	 * @param result 结果
	 * @param type   期望类型
	 * @param <T>    类型
	 * @return 数据
	 * @throws ApiException 类型不符
	 */
	static <T> T require(Object data, Class<T> type) throws ApiException {
		if (!type.isInstance(data)) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR,
					"响应类型不符，期望 " + type.getSimpleName() + "，实际 "
							+ (data == null ? "null" : data.getClass().getSimpleName()));
		}
		return type.cast(data);
	}
}
