package com.ascent.ui;

import javax.swing.SwingUtilities;

/**
 * 客户端启动类。
 *
 * <p>
 * 启动顺序：先统一外观和字体渲染设置，再在事件分发线程上创建登录窗口。
 * </p>
 *
 * @author ascent
 * @version 1.1
 */
public class Ascentsys {

	/**
	 * 启动客户端的主方法。
	 *
	 * @param args 无
	 */
	public static void main(String[] args) {
		// 开启字体抗锯齿，中文在 Swing 里会清晰很多
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UiTheme.apply();
				LoginFrame loginFrame = new LoginFrame();
				loginFrame.setVisible(true);
			}
		});
	}
}
