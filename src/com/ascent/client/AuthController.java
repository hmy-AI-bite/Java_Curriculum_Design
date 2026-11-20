package com.ascent.client;

import java.io.IOException;

import com.ascent.shared.ApiException;
import com.ascent.shared.dto.LoginResult;

/**
 * 登录 / 注册控制器。
 *
 * <p>
 * <b>所有方法都是阻塞的</b>，内部会走 socket。调用方必须放到后台线程里执行，
 * 例如复用现有的 {@code AsyncRunner}：
 * </p>
 *
 * <pre>
 * AsyncRunner.run(
 *     () -&gt; authController.login(user, pwd),
 *     result -&gt; openMainFrame(new Session(result)),
 *     error -&gt; showTip(error), null);
 * </pre>
 *
 * <p>
 * 窗口里不允许再出现 {@code new UserDataClient()} 这类代码，这是本次 MVC 改造
 * 的硬性约定。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public interface AuthController {

	/**
	 * 登录。
	 *
	 * @param username 账号
	 * @param password 密码
	 * @return 登录结果（token + 用户）
	 * @throws IOException  网络故障，界面提示“连接失败”
	 * @throws ApiException 业务失败，如账号密码错误
	 */
	LoginResult login(String username, String password) throws IOException, ApiException;

	/**
	 * 注册。
	 *
	 * @param username 账号
	 * @param password 密码
	 * @return 注册成功返回 true；用户名已存在抛 {@link ApiException}
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败，如用户名已存在、参数为空
	 */
	boolean register(String username, String password) throws IOException, ApiException;

	/**
	 * 关闭底层连接。窗口关闭时必须调用。
	 */
	void close();
}
