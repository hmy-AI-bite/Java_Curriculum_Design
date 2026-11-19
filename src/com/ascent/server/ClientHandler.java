package com.ascent.server;

import com.ascent.common.ErrorCode;
import com.ascent.common.protocol.Request;
import com.ascent.common.protocol.Response;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 一个客户端连接的处理器。
 *
 * <p>提示：这个类只做“读请求 -> 交给 Dispatcher -> 写响应”，
 * 不写业务逻辑，也不直接碰数据库。</p>
 *
 * <p>对象流注意点：
 * <ul>
 *   <li>先创建 ObjectOutputStream 并 flush，再创建 ObjectInputStream，
 *       否则双方都可能卡在构造器里等对方发 header；</li>
 *   <li>一个连接内请求是串行的，不要用多个线程同时读写同一对 ObjectStream；</li>
 *   <li>客户端断开时 readObject 会抛 EOFException，这是正常退出。</li>
 * </ul>
 */
public final class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final ClientSession session = new ClientSession();

    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        LOG.info("客户端连接: " + remote);

        try (Socket s = socket;
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.flush(); // 关键：先把对象流 header 发出去

            while (!s.isClosed()) {
                Object obj;
                try {
                    obj = in.readObject();
                } catch (EOFException e) {
                    break; // 客户端正常关闭
                }

                if (!(obj instanceof Request)) {
                    out.writeObject(Response.error(ErrorCode.VALIDATION, "请求格式错误"));
                    out.flush();
                    continue;
                }

                Response response = dispatcher.dispatch((Request) obj, session);
                out.writeObject(response);
                out.flush();
            }
        } catch (SocketException e) {
            LOG.info("客户端断开: " + remote + " (" + e.getMessage() + ")");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "连接处理异常: " + remote, e);
        } finally {
            LOG.info("连接关闭: " + remote);
        }
    }
}
