package com.ascent.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.ascent.client.OrderController;
import com.ascent.client.ProductController;
import com.ascent.client.Session;
import com.ascent.shared.dto.ProductDto;

/**
 * 商品面板。
 *
 * <p>
 * 顶部选分类和搜索，中间表格，右侧预览，底部操作按钮。所有数据都来自
 * {@link ProductController}，购物车状态来自 {@link Session}。
 * </p>
 *
 * <p>
 * 搜索走后端（{@code OP_SEARCH_PRODUCTS}），回车或点“搜索”触发；
 * 关键字清空后自动回到当前分类。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class ProductPanel extends JPanel {

	/** 父窗口。 */
	private final MainFrame parentFrame;

	/** 商品控制器。 */
	private final ProductController controller;

	/** 订单控制器。 */
	private final OrderController orderController;

	/** 会话。 */
	private final Session session;

	/** 分类下拉。 */
	private JComboBox<String> categoryComboBox;

	/** 表格。 */
	private JTable productTable;

	/** 表格模型。 */
	private ProductTableModel tableModel;

	/** 搜索框。 */
	private JTextField searchField;

	/** 状态文字。 */
	private JLabel statusLabel;

	/** 预览面板。 */
	private ProductPreviewPanel previewPanel;

	/** 详情按钮。 */
	private JButton detailsButton;

	/** 清空筛选按钮。 */
	private JButton clearButton;

	/** 退出按钮。 */
	private JButton exitButton;

	/** 购物车按钮。 */
	private JButton shoppingButton;

	/** 当前展示的商品。 */
	private List<ProductDto> products = new ArrayList<ProductDto>();

	/** 是否请求中。 */
	private boolean busy;

	/**
	 * 构造商品面板。
	 *
	 * @param parentFrame     主窗口
	 * @param controller      商品控制器
	 * @param orderController 订单控制器
	 */
	public ProductPanel(MainFrame parentFrame, ProductController controller,
			OrderController orderController) {
		this.parentFrame = parentFrame;
		this.controller = controller;
		this.orderController = orderController;
		this.session = parentFrame.session;
		buildGui();
		statusLabel.setText("正在连接数据服务器...");
	}

	/**
	 * 构建界面。
	 */
	private void buildGui() {
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(buildTopBar(), BorderLayout.NORTH);
		add(buildCenter(), BorderLayout.CENTER);
		add(buildBottomBar(), BorderLayout.SOUTH);
		updateButtonState();
	}

	/**
	 * 顶部工具条。
	 *
	 * @return 工具条
	 */
	private JPanel buildTopBar() {
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

		categoryComboBox = new JComboBox<String>();
		categoryComboBox.setPreferredSize(new Dimension(150, 28));
		categoryComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (!busy) {
					reloadProducts();
				}
			}
		});

		searchField = new JTextField(16);
		UiTheme.polish(searchField);
		searchField.setToolTipText("输入药品名称或 CAS 号，回车搜索");
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doSearch();
			}
		});

		JButton searchButton = new JButton("搜索");
		UiTheme.polish(searchButton);
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doSearch();
			}
		});

		JButton refreshButton = new JButton("刷新");
		UiTheme.polish(refreshButton);
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				loadCategories();
			}
		});

		top.add(new JLabel("商品分类"));
		top.add(categoryComboBox);
		top.add(Box.createHorizontalStrut(10));
		top.add(new JLabel("搜索"));
		top.add(searchField);
		top.add(searchButton);
		top.add(Box.createHorizontalStrut(10));
		top.add(refreshButton);
		return top;
	}

	/**
	 * 中间区域。
	 *
	 * @return 面板
	 */
	private JPanel buildCenter() {
		JPanel center = new JPanel(new BorderLayout(0, 4));

		statusLabel = new JLabel(" ");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
		statusLabel.setForeground(new Color(0x66, 0x66, 0x66));
		center.add(statusLabel, BorderLayout.NORTH);

		tableModel = new ProductTableModel();
		productTable = new JTable(tableModel);
		productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		productTable.setRowHeight(26);
		productTable.setFillsViewportHeight(true);
		productTable.setAutoCreateRowSorter(true);
		productTable.getTableHeader().setReorderingAllowed(false);
		productTable.getSelectionModel().addListSelectionListener(new ProductSelectionListener());

		previewPanel = new ProductPreviewPanel();

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(productTable), new JScrollPane(previewPanel));
		split.setResizeWeight(0.68);
		split.setBorder(null);
		split.setDividerSize(6);
		center.add(split, BorderLayout.CENTER);
		return center;
	}

	/**
	 * 底部按钮。
	 *
	 * @return 按钮面板
	 */
	private JPanel buildBottomBar() {
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		shoppingButton = new JButton("查看购物车");
		detailsButton = new JButton("查看详情");
		clearButton = new JButton("清空筛选");
		exitButton = new JButton("退出");
		for (JButton button : new JButton[] { shoppingButton, detailsButton, clearButton, exitButton }) {
			UiTheme.polish(button);
			bottom.add(button);
		}

		shoppingButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openShoppingCart();
			}
		});
		detailsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				showDetails();
			}
		});
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				searchField.setText("");
				if (categoryComboBox.getItemCount() > 0) {
					categoryComboBox.setSelectedIndex(0);
				}
				reloadProducts();
			}
		});
		exitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				parentFrame.exit();
			}
		});
		return bottom;
	}

	// ------------------------------------------------------------ 数据

	/**
	 * 加载分类，完成后自动加载商品。
	 */
	public void loadCategories() {
		if (busy) {
			return;
		}
		setBusyState(true, "正在加载商品分类...");
		AsyncRunner.run(new AsyncRunner.Task<List<String>>() {
			public List<String> run() throws Exception {
				return controller.loadCategories();
			}
		}, new AsyncRunner.Success<List<String>>() {
			public void run(List<String> categories) {
				setBusyState(false);
				fillCategories(categories);
				statusLabel.setText("共 " + (categories == null ? 0 : categories.size()) + " 个分类");
			}
		}, new AsyncRunner.Failure() {
			public void run(Exception error) {
				setBusyState(false);
				statusLabel.setText("加载分类失败：" + Errors.friendly(error));
			}
		}, null);
	}

	/**
	 * 填充分类下拉，期间摘掉监听器避免重复请求。
	 *
	 * @param categories 分类
	 */
	private void fillCategories(List<String> categories) {
		ActionListener[] listeners = categoryComboBox.getActionListeners();
		for (ActionListener listener : listeners) {
			categoryComboBox.removeActionListener(listener);
		}
		categoryComboBox.removeAllItems();
		categoryComboBox.addItem(ProductController.ALL_CATEGORIES);
		if (categories != null) {
			for (String category : categories) {
				categoryComboBox.addItem(category);
			}
		}
		categoryComboBox.setSelectedIndex(0);
		for (ActionListener listener : listeners) {
			categoryComboBox.addActionListener(listener);
		}
		reloadProducts();
	}

	/**
	 * 按当前分类重新加载。
	 */
	private void reloadProducts() {
		Object selected = categoryComboBox.getSelectedItem();
		String category = selected == null ? ProductController.ALL_CATEGORIES : selected.toString();
		loadProducts(category);
	}

	/**
	 * 加载商品。
	 *
	 * @param category 分类
	 */
	public void loadProducts(final String category) {
		if (busy) {
			return;
		}
		setBusyState(true, "正在加载商品...");
		AsyncRunner.run(new AsyncRunner.Task<List<ProductDto>>() {
			public List<ProductDto> run() throws Exception {
				return controller.loadProducts(category);
			}
		}, new AsyncRunner.Success<List<ProductDto>>() {
			public void run(List<ProductDto> list) {
				setBusyState(false);
				showProducts(list, "共 " + (list == null ? 0 : list.size()) + " 个商品");
			}
		}, new AsyncRunner.Failure() {
			public void run(Exception error) {
				setBusyState(false);
				showProducts(new ArrayList<ProductDto>(), "加载失败：" + Errors.friendly(error));
			}
		}, null);
	}

	/**
	 * 执行搜索。
	 */
	private void doSearch() {
		if (busy) {
			return;
		}
		final String keyword = searchField.getText().trim();
		if (keyword.isEmpty()) {
			reloadProducts();
			return;
		}
		setBusyState(true, "正在搜索“" + keyword + "”...");
		AsyncRunner.run(new AsyncRunner.Task<List<ProductDto>>() {
			public List<ProductDto> run() throws Exception {
				return controller.searchProducts(keyword);
			}
		}, new AsyncRunner.Success<List<ProductDto>>() {
			public void run(List<ProductDto> list) {
				setBusyState(false);
				showProducts(list, "搜索到 " + (list == null ? 0 : list.size()) + " 个商品");
			}
		}, new AsyncRunner.Failure() {
			public void run(Exception error) {
				setBusyState(false);
				statusLabel.setText("搜索失败：" + Errors.friendly(error));
			}
		}, null);
	}

	/**
	 * 刷新表格。
	 *
	 * @param list   商品
	 * @param status 状态文字
	 */
	private void showProducts(List<ProductDto> list, String status) {
		products = list == null ? new ArrayList<ProductDto>() : list;
		tableModel.setProducts(products);
		statusLabel.setText(status);
		if (!products.isEmpty()) {
			productTable.setRowSelectionInterval(0, 0);
		} else {
			previewPanel.clear();
		}
		updateButtonState();
	}

	/**
	 * 当前选中商品。
	 *
	 * @return 商品，未选中返回 null
	 */
	private ProductDto selectedProduct() {
		int viewRow = productTable.getSelectedRow();
		if (viewRow < 0) {
			return null;
		}
		int modelRow = productTable.convertRowIndexToModel(viewRow);
		if (modelRow < 0 || modelRow >= products.size()) {
			return null;
		}
		return products.get(modelRow);
	}

	/**
	 * 打开详情。
	 */
	private void showDetails() {
		ProductDto product = selectedProduct();
		if (product == null) {
			return;
		}
		ProductDetailsDialog dialog = new ProductDetailsDialog(parentFrame, product, session);
		dialog.setVisible(true);
		if (dialog.isAddedToCart()) {
			parentFrame.refreshStatus();
			updateButtonState();
		}
	}

	/**
	 * 打开购物车。
	 */
	public void openShoppingCart() {
		ShoppingCartDialog dialog = new ShoppingCartDialog(parentFrame, session, orderController);
		dialog.setVisible(true);
		parentFrame.refreshStatus();
		updateButtonState();
	}

	/**
	 * 设置忙碌状态。
	 *
	 * @param running 是否忙碌
	 * @param message 状态文字
	 */
	private void setBusyState(boolean running, String message) {
		busy = running;
		if (statusLabel != null && message != null) {
			statusLabel.setText(message);
		}
		setCursor(Cursor.getPredefinedCursor(running ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
		updateButtonState();
	}

	/**
	 * 设置忙碌状态。
	 *
	 * @param running 是否忙碌
	 */
	private void setBusyState(boolean running) {
		setBusyState(running, null);
	}

	/**
	 * 更新按钮可用性。
	 */
	private void updateButtonState() {
		boolean selected = productTable != null && productTable.getSelectedRow() >= 0;
		if (detailsButton != null) {
			detailsButton.setEnabled(selected && !busy);
		}
		if (clearButton != null) {
			clearButton.setEnabled(!busy);
		}
		if (shoppingButton != null) {
			shoppingButton.setEnabled(!busy && session != null && !session.isCartEmpty());
		}
		if (exitButton != null) {
			exitButton.setEnabled(!busy);
		}
	}

	/**
	 * 表格选中变化。
	 */
	class ProductSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (event.getValueIsAdjusting()) {
				return;
			}
			updateButtonState();
			ProductDto product = selectedProduct();
			if (product == null) {
				previewPanel.clear();
			} else {
				previewPanel.showProduct(product);
			}
		}
	}

	/**
	 * 表格模型。
	 */
	static class ProductTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		/** 列名。 */
		private static final String[] COLUMNS = { "药品名称", "CAS 号", "规格", "价格", "库存", "分类" };

		/** 数据。 */
		private List<ProductDto> rows = new ArrayList<ProductDto>();

		/**
		 * 设置数据。
		 *
		 * @param products 商品
		 */
		void setProducts(List<ProductDto> products) {
			rows = new ArrayList<ProductDto>(products);
			fireTableDataChanged();
		}

		public int getRowCount() {
			return rows.size();
		}

		public int getColumnCount() {
			return COLUMNS.length;
		}

		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			ProductDto p = rows.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return p.getName();
			case 1:
				return p.getCas();
			case 2:
				return p.getSpec();
			case 3:
				return "￥" + p.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
			case 4:
				return p.getStock();
			default:
				return p.getCategory();
			}
		}
	}

	/**
	 * 右侧预览。
	 */
	static class ProductPreviewPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		/** 内容。 */
		private final JPanel content = new JPanel();

		/**
		 * 构造。
		 */
		ProductPreviewPanel() {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			setPreferredSize(new Dimension(260, 100));

			content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
			JScrollPane scroll = new JScrollPane(content);
			scroll.setBorder(null);
			add(scroll, BorderLayout.CENTER);
			clear();
		}

		/**
		 * 展示商品。
		 *
		 * @param product 商品
		 */
		void showProduct(ProductDto product) {
			content.removeAll();

			JLabel name = new JLabel(product.getName());
			name.setFont(name.getFont().deriveFont(Font.BOLD, 15f));
			name.setAlignmentX(LEFT_ALIGNMENT);
			content.add(name);
			content.add(Box.createVerticalStrut(6));

			JPanel grid = new JPanel(new GridBagLayout());
			grid.setAlignmentX(LEFT_ALIGNMENT);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(2, 0, 2, 8);
			gbc.gridx = 0;
			gbc.gridy = 0;

			addField(grid, gbc, "CAS 号", product.getCas());
			addField(grid, gbc, "分子式", product.getFormula());
			addField(grid, gbc, "规格", product.getSpec());
			addField(grid, gbc, "价格", "￥" + product.getPrice()
					.setScale(2, RoundingMode.HALF_UP).toPlainString());
			addField(grid, gbc, "库存", product.getStock() + " 件");
			addField(grid, gbc, "分类", product.getCategory());
			content.add(grid);

			URL imageUrl = getClass().getResource("/images/" + product.getStructure());
			JLabel picture;
			if (imageUrl == null) {
				picture = new JLabel("（暂无结构图）");
				picture.setForeground(new Color(0x99, 0x99, 0x99));
			} else {
				picture = new JLabel(new ImageIcon(imageUrl));
				picture.setToolTipText(product.getName());
			}
			picture.setAlignmentX(LEFT_ALIGNMENT);
			content.add(Box.createVerticalStrut(8));
			content.add(picture);

			content.revalidate();
			content.repaint();
		}

		/**
		 * 加一行字段。
		 *
		 * @param grid  网格
		 * @param gbc   约束
		 * @param name  字段名
		 * @param value 值
		 */
		private void addField(JPanel grid, GridBagConstraints gbc, String name, String value) {
			JLabel key = new JLabel(name + "：");
			key.setForeground(new Color(0x66, 0x66, 0x66));
			JLabel val = new JLabel(value == null || value.trim().isEmpty() ? "-" : value);
			grid.add(key, gbc);
			gbc.gridx = 1;
			grid.add(val, gbc);
			gbc.gridx = 0;
			gbc.gridy++;
		}

		/**
		 * 清空。
		 */
		void clear() {
			content.removeAll();
			JLabel empty = new JLabel("选择左侧商品查看详情");
			empty.setForeground(new Color(0x99, 0x99, 0x99));
			content.add(empty);
			content.revalidate();
			content.repaint();
		}
	}
}
