package com.ascent.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.ascent.client.AuthController;
import com.ascent.client.Session;
import com.ascent.client.SocketAuthController;
import com.ascent.shared.dto.LoginResult;

/**
 * 用户登录窗体。
 *
 * <p>
 * 只依赖 {@link AuthController}，不直接碰 socket。登录请求在后台线程执行，
 * 失败可重试；成功后建立 {@link Session} 交给主窗口。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class LoginFrame extends JFrame {

	/** 账号输入框。 */
	protected JTextField userText;

	/** 密码输入框。 */
	protected JPasswordField password;

	/** 提示标签。 */
	protected JLabel tip;

	/** 登录控制器，懒创建。 */
	protected AuthController auth;

	/** 登录按钮。 */
	private JButton loginButton;

	/** 注册按钮。 */
	private JButton registButton;

	/** 退出按钮。 */
	private JButton exitButton;

	/** 是否有请求进行中。 */
	private boolean busy;

	/**
	 * 默认构造。
	 */
	public LoginFrame() {
		setTitle("AscentSys 药品销售管理系统 - 登录");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(true);

		buildGui();
		addWindowListener(new WindowCloser());

		pack();
		setMinimumSize(new Dimension(380, 300));
		setLocationRelativeTo(null);
	}

	/**
	 * 构建界面。
	 */
	private void buildGui() {
		JPanel root = new JPanel(new BorderLayout(0, 12));
		root.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

		root.add(buildHeader(), BorderLayout.NORTH);
		root.add(buildForm(), BorderLayout.CENTER);
		root.add(buildButtons(), BorderLayout.SOUTH);

		setContentPane(root);
	}

	/**
	 * 顶部标题。
	 *
	 * @return 标题面板
	 */
	private JPanel buildHeader() {
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("AscentSys 药品销售管理系统");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
		title.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel subtitle = new JLabel("请使用账号密码登录（示例账号 user1 / 1）");
		subtitle.setForeground(new Color(0x66, 0x66, 0x66));
		subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

		header.add(title);
		header.add(Box.createVerticalStrut(6));
		header.add(subtitle);
		return header;
	}

	/**
	 * 登录表单。
	 *
	 * @return 表单面板
	 */
	private JPanel buildForm() {
		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 6, 6, 6);
		gbc.anchor = GridBagConstraints.WEST;

		JLabel userLabel = new JLabel("用户账号");
		JLabel passwordLabel = new JLabel("用户密码");

		userText = new JTextField(18);
		password = new JPasswordField(18);
		UiTheme.polish(userText);
		UiTheme.polish(password);

		tip = new JLabel(" ");
		tip.setForeground(new Color(0x66, 0x66, 0x66));
		tip.setPreferredSize(new Dimension(220, 18));

		gbc.gridx = 0;
		gbc.gridy = 0;
		form.add(userLabel, gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		form.add(userText, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		form.add(passwordLabel, gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		form.add(password, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		form.add(tip, gbc);
		return form;
	}

	/**
	 * 底部按钮。
	 *
	 * @return 按钮面板
	 */
	private JPanel buildButtons() {
		JPanel buttons = new JPanel();
		loginButton = new JButton("登录");
		registButton = new JButton("注册");
		exitButton = new JButton("退出");
		for (JButton button : new JButton[] { loginButton, registButton, exitButton }) {
			UiTheme.polish(button);
			buttons.add(button);
		}
		loginButton.setPreferredSize(new Dimension(88, 30));

		loginButton.addActionListener(new LoginActionListener());
		registButton.addActionListener(new RegistActionListener());
		exitButton.addActionListener(new ExitActionListener());
		userText.addActionListener(new LoginActionListener());
		password.addActionListener(new LoginActionListener());
		return buttons;
	}

	/**
	 * 显示提示。
	 *
	 * @param message 文本
	 * @param error   是否错误样式
	 */
	private void showTip(String message, boolean error) {
		tip.setText(message == null || message.isEmpty() ? " " : message);
		tip.setForeground(error ? new Color(0xC0, 0x39, 0x2B) : new Color(0x2E, 0x7D, 0x32));
	}

	/**
	 * 切换忙碌状态。
	 *
	 * @param running 是否请求中
	 */
	private void setBusy(boolean running) {
		busy = running;
		loginButton.setEnabled(!running);
		registButton.setEnabled(!running);
		exitButton.setEnabled(!running);
		setCursor(Cursor.getPredefinedCursor(running ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
	}

	/**
	 * 关闭控制器。
	 */
	private void closeController() {
		if (auth != null) {
			auth.close();
			auth = null;
		}
	}

	/**
	 * 执行登录。
	 */
	private void doLogin() {
		if (busy) {
			return;
		}
		final String username = userText.getText().trim();
		final String pwd = new String(password.getPassword());
		if (username.isEmpty()) {
			showTip("请输入用户账号。", true);
			userText.requestFocusInWindow();
			return;
		}
		if (pwd.isEmpty()) {
			showTip("请输入用户密码。", true);
			password.requestFocusInWindow();
			return;
		}

		showTip("正在登录...", false);
		setBusy(true);

		AsyncRunner.run(new AsyncRunner.Task<LoginResult>() {
			public LoginResult run() throws Exception {
				closeController();
				auth = new SocketAuthController();
				return auth.login(username, pwd);
			}
		}, new AsyncRunner.Success<LoginResult>() {
			public void run(LoginResult result) {
				setBusy(false);
				closeController();
				openMainFrame(new Session(result));
			}
		}, new AsyncRunner.Failure() {
			public void run(Exception error) {
				setBusy(false);
				closeController();
				showTip(Errors.friendly(error), true);
				password.setText("");
				password.requestFocusInWindow();
			}
		}, null);
	}

	/**
	 * 打开主窗口。
	 *
	 * @param session 会话
	 */
	private void openMainFrame(Session session) {
		setVisible(false);
		dispose();
		MainFrame mainFrame = new MainFrame(session);
		mainFrame.setVisible(true);
	}

	/**
	 * 退出程序。
	 */
	private void exit() {
		closeController();
		dispose();
		System.exit(0);
	}

	/**
	 * 登录按钮。
	 */
	class LoginActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			doLogin();
		}
	}

	/**
	 * 注册按钮。
	 */
	class RegistActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			RegistFrame registFrame = new RegistFrame(userText.getText().trim());
			registFrame.setVisible(true);
		}
	}

	/**
	 * 退出按钮。
	 */
	class ExitActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			exit();
		}
	}

	/**
	 * 关闭窗口。
	 */
	class WindowCloser extends WindowAdapter {
		public void windowClosing(WindowEvent event) {
			exit();
		}
	}
}
