package com.ascent.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ascent.client.OrderController;
import com.ascent.client.ProductController;
import com.ascent.client.Session;
import com.ascent.client.SocketOrderController;
import com.ascent.client.SocketProductController;

/**
 * 主窗口。
 *
 * <p>
 * 持有登录后的 {@link Session} 和两个控制器（商品、订单），向下传给商品面板。
 * 注销或退出时统一关闭控制器，避免连接泄漏。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {

	/** 页签。 */
	protected JTabbedPane tabbedPane;

	/** 商品面板。 */
	protected ProductPanel productPanel;

	/** 当前会话。 */
	protected final Session session;

	/** 商品控制器。 */
	protected final ProductController productController;

	/** 订单控制器。 */
	protected final OrderController orderController;

	/** 状态栏。 */
	protected JLabel statusLabel;

	/**
	 * 构造主窗口。
	 *
	 * @param session 登录会话
	 */
	public MainFrame(Session session) {
		if (session == null) {
			throw new IllegalArgumentException("会话不能为空");
		}
		this.session = session;
		this.productController = new SocketProductController();
		this.orderController = new SocketOrderController();

		setTitle("欢迎使用 AscentSys 药品销售管理系统");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(760, 520));

		buildGui();
		buildMenu();

		setSize(900, 620);
		setLocationRelativeTo(null);
		addWindowListener(new WindowCloser());

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				productPanel.loadCategories();
			}
		});
	}

	/**
	 * 构建界面。
	 */
	protected void buildGui() {
		JPanel root = new JPanel(new BorderLayout());

		tabbedPane = new JTabbedPane();
		productPanel = new ProductPanel(this, productController, orderController);
		tabbedPane.addTab("药品", productPanel);
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

		root.add(tabbedPane, BorderLayout.CENTER);
		root.add(buildStatusBar(), BorderLayout.SOUTH);
		setContentPane(root);
	}

	/**
	 * 状态栏。
	 *
	 * @return 状态栏
	 */
	protected JPanel buildStatusBar() {
		JPanel bar = new JPanel(new BorderLayout());
		bar.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD0, 0xD0, 0xD0)),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));

		statusLabel = new JLabel();
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
		statusLabel.setForeground(new Color(0x55, 0x55, 0x55));
		bar.add(statusLabel, BorderLayout.WEST);

		JLabel hint = new JLabel("服务端：localhost:5150   协议 v2");
		hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12f));
		hint.setForeground(new Color(0x99, 0x99, 0x99));
		bar.add(hint, BorderLayout.EAST);

		refreshStatus();
		return bar;
	}

	/**
	 * 刷新状态栏。
	 */
	public void refreshStatus() {
		if (statusLabel == null) {
			return;
		}
		statusLabel.setText("当前用户：" + session.user().getUsername()
				+ (session.isAdmin() ? "（管理员）" : "")
				+ "    购物车：" + session.cartCount() + " 件");
	}

	/**
	 * 菜单栏。
	 */
	protected void buildMenu() {
		JMenuBar myMenuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("文件");
		fileMenu.setMnemonic('f');

		JMenuItem refreshMenuItem = new JMenuItem("刷新商品列表");
		refreshMenuItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
		refreshMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				productPanel.loadCategories();
			}
		});
		fileMenu.add(refreshMenuItem);

		JMenuItem cartMenuItem = new JMenuItem("查看购物车");
		cartMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl L"));
		cartMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				productPanel.openShoppingCart();
			}
		});
		fileMenu.add(cartMenuItem);

		fileMenu.addSeparator();

		JMenuItem logoutMenuItem = new JMenuItem("注销并返回登录");
		logoutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				logout();
			}
		});
		fileMenu.add(logoutMenuItem);

		JMenuItem exitMenuItem = new JMenuItem("退出");
		exitMenuItem.setMnemonic('x');
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl X"));
		exitMenuItem.addActionListener(new ExitActionListener());
		fileMenu.add(exitMenuItem);

		myMenuBar.add(fileMenu);
		setupLookAndFeelMenu(myMenuBar);

		JMenu helpMenu = new JMenu("帮助");
		helpMenu.setMnemonic('h');
		JMenuItem aboutMenuItem = new JMenuItem("关于");
		aboutMenuItem.setMnemonic('a');
		aboutMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl A"));
		aboutMenuItem.addActionListener(new AboutActionListener());
		helpMenu.add(aboutMenuItem);
		myMenuBar.add(helpMenu);

		setJMenuBar(myMenuBar);
	}

	/**
	 * 外观菜单。
	 *
	 * @param theMenuBar 菜单栏
	 */
	protected void setupLookAndFeelMenu(JMenuBar theMenuBar) {
		JMenu lookAndFeelMenu = new JMenu("选项");
		LookAndFeelListener listener = new LookAndFeelListener();
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			JMenuItem item = new JMenuItem(info.getName() + " 外观");
			item.setActionCommand(info.getClassName());
			item.addActionListener(listener);
			lookAndFeelMenu.add(item);
		}
		theMenuBar.add(lookAndFeelMenu);
	}

	/**
	 * 注销：关闭连接，回登录窗口。
	 */
	public void logout() {
		closeResources();
		dispose();
		LoginFrame loginFrame = new LoginFrame();
		loginFrame.setVisible(true);
	}

	/**
	 * 退出程序。
	 */
	public void exit() {
		closeResources();
		setVisible(false);
		dispose();
		System.exit(0);
	}

	/**
	 * 关闭控制器。
	 */
	private void closeResources() {
		productController.close();
		orderController.close();
	}

	/**
	 * 退出菜单。
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

	/**
	 * 外观切换。
	 */
	class LookAndFeelListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			String className = event.getActionCommand();
			if (!UiTheme.change(className, MainFrame.this)) {
				JOptionPane.showMessageDialog(MainFrame.this, "外观切换失败：" + className,
						"提示", JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	/**
	 * 关于。
	 */
	class AboutActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			String msg = "AscentSys 药品销售管理系统\nJava SE 11 + Swing 课程设计\n\n"
					+ "协议版本：" + com.ascent.shared.Protocol.VERSION + "\n"
					+ "当前用户：" + session.user().getUsername();
			JOptionPane.showMessageDialog(MainFrame.this, msg, "关于",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
