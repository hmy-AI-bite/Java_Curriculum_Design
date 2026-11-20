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
import com.ascent.client.SocketAuthController;

/**
 * 用户注册窗体。
 *
 * <p>
 * 校验在本地先做一遍（空账号、空密码、两次不一致），通过后才发请求；
 * 请求走 {@link AuthController}，服务端返回 {@code USER_EXISTS} 时提示换账号。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class RegistFrame extends JFrame {

	/** 账号。 */
	private JTextField userText;

	/** 密码。 */
	private JPasswordField password;

	/** 重复密码。 */
	private JPasswordField repassword;

	/** 提示。 */
	private JLabel tip;

	/** 注册控制器。 */
	private AuthController auth;

	/** 注册按钮。 */
	private JButton registButton;

	/** 返回按钮。 */
	private JButton exitButton;

	/** 是否请求中。 */
	private boolean busy;

	/**
	 * 默认构造。
	 */
	public RegistFrame() {
		this(null);
	}

	/**
	 * 带初始账号的构造。
	 *
	 * @param initialUsername 初始账号
	 */
	public RegistFrame(String initialUsername) {
		setTitle("AscentSys - 用户注册");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(true);

		buildGui();
		if (initialUsername != null) {
			userText.setText(initialUsername);
		}
		addWindowListener(new WindowCloser());

		pack();
		setMinimumSize(new Dimension(400, 340));
		setLocationRelativeTo(null);

		if (initialUsername != null && !initialUsername.isEmpty()) {
			password.requestFocusInWindow();
		} else {
			userText.requestFocusInWindow();
		}
	}

	/**
	 * 构建界面。
	 */
	private void buildGui() {
		JPanel root = new JPanel(new BorderLayout(0, 12));
		root.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 26));
		root.add(buildHeader(), BorderLayout.NORTH);
		root.add(buildForm(), BorderLayout.CENTER);
		root.add(buildButtons(), BorderLayout.SOUTH);
		setContentPane(root);
	}

	/**
	 * 标题。
	 *
	 * @return 标题面板
	 */
	private JPanel buildHeader() {
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("注册新用户");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
		title.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel subtitle = new JLabel("账号不能重复，密码不能为空");
		subtitle.setForeground(new Color(0x66, 0x66, 0x66));
		subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

		header.add(title);
		header.add(Box.createVerticalStrut(6));
		header.add(subtitle);
		return header;
	}

	/**
	 * 表单。
	 *
	 * @return 表单面板
	 */
	private JPanel buildForm() {
		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 6, 6, 6);
		gbc.anchor = GridBagConstraints.WEST;

		userText = new JTextField(18);
		password = new JPasswordField(18);
		repassword = new JPasswordField(18);
		UiTheme.polish(userText);
		UiTheme.polish(password);
		UiTheme.polish(repassword);

		tip = new JLabel(" ");
		tip.setForeground(new Color(0x66, 0x66, 0x66));

		addRow(form, gbc, 0, new JLabel("用户账号"), userText);
		addRow(form, gbc, 1, new JLabel("用户密码"), password);
		addRow(form, gbc, 2, new JLabel("重复密码"), repassword);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		form.add(tip, gbc);
		return form;
	}

	/**
	 * 加一行。
	 *
	 * @param form  表单
	 * @param gbc   约束
	 * @param row   行号
	 * @param label 标签
	 * @param field 输入框
	 */
	private void addRow(JPanel form, GridBagConstraints gbc, int row, JLabel label,
			JTextField field) {
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		form.add(label, gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		form.add(field, gbc);
	}

	/**
	 * 按钮。
	 *
	 * @return 按钮面板
	 */
	private JPanel buildButtons() {
		JPanel buttons = new JPanel();
		registButton = new JButton("注册");
		exitButton = new JButton("返回登录");
		UiTheme.polish(registButton);
		UiTheme.polish(exitButton);
		registButton.setPreferredSize(new Dimension(88, 30));
		buttons.add(registButton);
		buttons.add(exitButton);

		registButton.addActionListener(new RegistActionListener());
		exitButton.addActionListener(new ExitActionListener());
		return buttons;
	}

	/**
	 * 提示。
	 *
	 * @param message 文本
	 * @param error   是否错误样式
	 */
	private void showTip(String message, boolean error) {
		tip.setText(message == null || message.isEmpty() ? " " : message);
		tip.setForeground(error ? new Color(0xC0, 0x39, 0x2B) : new Color(0x2E, 0x7D, 0x32));
	}

	/**
	 * 忙碌状态。
	 *
	 * @param running 是否请求中
	 */
	private void setBusy(boolean running) {
		busy = running;
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
	 * 本地校验。
	 *
	 * @param username 账号
	 * @param pwd      密码
	 * @param confirm  重复密码
	 * @return 错误提示，通过返回 null
	 */
	private String validate(String username, String pwd, String confirm) {
		if (username.isEmpty()) {
			return "用户账号不能为空。";
		}
		if (pwd.isEmpty()) {
			return "用户密码不能为空。";
		}
		if (confirm.isEmpty()) {
			return "请再次输入密码。";
		}
		if (!pwd.equals(confirm)) {
			return "两次输入的密码不一致。";
		}
		return null;
	}

	/**
	 * 执行注册。
	 */
	private void doRegister() {
		if (busy) {
			return;
		}
		final String username = userText.getText().trim();
		final String pwd = new String(password.getPassword());
		final String confirm = new String(repassword.getPassword());

		String problem = validate(username, pwd, confirm);
		if (problem != null) {
			showTip(problem, true);
			return;
		}

		showTip("正在提交注册...", false);
		setBusy(true);

		AsyncRunner.run(new AsyncRunner.Task<Boolean>() {
			public Boolean run() throws Exception {
				closeController();
				auth = new SocketAuthController();
				return auth.register(username, pwd);
			}
		}, new AsyncRunner.Success<Boolean>() {
			public void run(Boolean created) {
				setBusy(false);
				closeController();
				if (created == null || !created) {
					showTip("用户名已存在，请换一个。", true);
					userText.requestFocusInWindow();
					userText.selectAll();
					return;
				}
				showTip("注册成功，请返回登录。", false);
				password.setText("");
				repassword.setText("");
				registButton.setEnabled(false);
			}
		}, new AsyncRunner.Failure() {
			public void run(Exception error) {
				setBusy(false);
				closeController();
				showTip(Errors.friendly(error), true);
			}
		}, null);
	}

	/**
	 * 关闭窗口。
	 */
	private void close() {
		closeController();
		dispose();
	}

	/**
	 * 注册按钮。
	 */
	class RegistActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			doRegister();
		}
	}

	/**
	 * 返回按钮。
	 */
	class ExitActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			close();
		}
	}

	/**
	 * 关闭窗口。
	 */
	class WindowCloser extends WindowAdapter {
		public void windowClosing(WindowEvent event) {
			close();
		}
	}
}
