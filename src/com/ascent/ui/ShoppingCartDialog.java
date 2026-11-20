package com.ascent.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.ascent.client.OrderController;
import com.ascent.client.Session;
import com.ascent.shared.dto.CartItemDto;
import com.ascent.shared.dto.OrderDto;

/**
 * 购物车对话框。
 *
 * <p>
 * 数据全部来自 {@link Session}；提交时调用 {@link OrderController#placeOrder}，
 * 成功后清空会话购物车并弹出订单回执。失败（例如库存不足）保留购物车内容，
 * 让用户改数量后重试。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class ShoppingCartDialog extends JDialog {

	/** 空购物车提示。 */
	private static final String EMPTY_TIP = "购物车是空的，去商品列表里挑几件吧。";

	/** 会话。 */
	private final Session session;

	/** 订单控制器。 */
	private final OrderController orderController;

	/** 父窗口。 */
	private final Frame parentFrame;

	/** 表格。 */
	private JTable table;

	/** 模型。 */
	private CartTableModel model;

	/** 总价。 */
	private JLabel totalLabel;

	/** 空提示。 */
	private JLabel emptyTip;

	/** 内容区。 */
	private JPanel contentPanel;

	/** 清空按钮。 */
	private JButton clearButton;

	/** 提交按钮。 */
	private JButton submitButton;

	/** 关闭按钮。 */
	private JButton closeButton;

	/** 是否提交中。 */
	private boolean busy;

	/**
	 * 构造。
	 *
	 * @param parent  父窗口
	 * @param session 会话
	 * @param orderController 订单控制器
	 */
	public ShoppingCartDialog(Frame parent, Session session, OrderController orderController) {
		super(parent, "购物车", true);
		this.parentFrame = parent;
		this.session = session;
		this.orderController = orderController;

		buildGui();
		refreshView();

		setSize(680, 470);
		setMinimumSize(new Dimension(600, 360));
		setLocationRelativeTo(parent);
	}

	/**
	 * 构建界面。
	 */
	private void buildGui() {
		JPanel root = new JPanel(new BorderLayout(0, 10));
		root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

		JLabel title = new JLabel("已选商品");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
		root.add(title, BorderLayout.NORTH);

		contentPanel = new JPanel(new BorderLayout());
		model = new CartTableModel();
		table = new JTable(model);
		table.setRowHeight(28);
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		installQuantityEditor();

		emptyTip = new JLabel(EMPTY_TIP);
		emptyTip.setForeground(new Color(0x99, 0x99, 0x99));
		emptyTip.setHorizontalAlignment(JLabel.CENTER);

		root.add(contentPanel, BorderLayout.CENTER);
		root.add(buildFooter(), BorderLayout.SOUTH);
		setContentPane(root);
	}

	/**
	 * 数量列用微调框编辑。
	 */
	private void installQuantityEditor() {
		TableColumn column = table.getColumnModel().getColumn(CartTableModel.COL_QUANTITY);
		column.setCellEditor(new SpinnerEditor(new SpinnerNumberModel(1, 1, 9999, 1)));
		column.setPreferredWidth(70);
		column.setMaxWidth(90);
	}

	/**
	 * 底部。
	 *
	 * @return 底部面板
	 */
	private JPanel buildFooter() {
		JPanel footer = new JPanel(new BorderLayout());
		totalLabel = new JLabel();
		totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 15f));
		totalLabel.setForeground(new Color(0xC0, 0x39, 0x2B));
		footer.add(totalLabel, BorderLayout.WEST);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		clearButton = new JButton("清空购物车");
		closeButton = new JButton("关闭");
		submitButton = new JButton("提交订单");
		for (JButton button : new JButton[] { clearButton, closeButton, submitButton }) {
			UiTheme.polish(button);
			buttons.add(button);
		}
		submitButton.setPreferredSize(new Dimension(100, 30));
		footer.add(buttons, BorderLayout.EAST);

		clearButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				clearCart();
			}
		});
		closeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				setVisible(false);
			}
		});
		submitButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				submitOrder();
			}
		});
		return footer;
	}

	/**
	 * 清空购物车。
	 */
	private void clearCart() {
		if (busy || session.isCartEmpty()) {
			return;
		}
		int answer = JOptionPane.showConfirmDialog(this, "确定清空购物车里的所有商品吗？",
				"清空购物车", JOptionPane.YES_NO_OPTION);
		if (answer == JOptionPane.YES_OPTION) {
			session.clearCart();
			refreshView();
		}
	}

	/**
	 * 提交订单。
	 */
	private void submitOrder() {
		if (busy) {
			return;
		}
		if (session.isCartEmpty()) {
			JOptionPane.showMessageDialog(this, EMPTY_TIP, "提示", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		setBusy(true);
		totalLabel.setText("正在提交订单...");

		AsyncRunner.run(new AsyncRunner.Task<OrderDto>() {
			public OrderDto run() throws Exception {
				return orderController.placeOrder(session.token(), session.cartItems());
			}
		}, new AsyncRunner.Success<OrderDto>() {
			public void run(OrderDto order) {
				setBusy(false);
				session.clearCart();
				refreshView();
				ShoppingMessageDialog dialog = new ShoppingMessageDialog(parentFrame, order);
				dialog.setVisible(true);
				setVisible(false);
			}
		}, new AsyncRunner.Failure() {
			public void run(Exception error) {
				setBusy(false);
				refreshView();
				JOptionPane.showMessageDialog(ShoppingCartDialog.this, Errors.friendly(error),
						"提交失败", JOptionPane.WARNING_MESSAGE);
			}
		}, null);
	}

	/**
	 * 切换忙碌状态。
	 *
	 * @param running 是否提交中
	 */
	private void setBusy(boolean running) {
		busy = running;
		submitButton.setEnabled(!running);
		clearButton.setEnabled(!running);
		closeButton.setEnabled(!running);
	}

	/**
	 * 刷新视图。
	 */
	private void refreshView() {
		contentPanel.removeAll();
		if (session.isCartEmpty()) {
			contentPanel.add(emptyTip, BorderLayout.CENTER);
		} else {
			contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		}
		model.setItems(session.cartItems());
		BigDecimal total = session.cartTotal().setScale(2, RoundingMode.HALF_UP);
		totalLabel.setText("共 " + session.cartCount() + " 件，合计：￥" + total.toPlainString());
		contentPanel.revalidate();
		contentPanel.repaint();
	}

	/**
	 * 表格模型。
	 */
	private class CartTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		static final int COL_PRODUCT = 0;
		static final int COL_PRICE = 1;
		static final int COL_QUANTITY = 2;
		static final int COL_SUBTOTAL = 3;

		private final String[] columns = { "药品名称", "单价", "数量", "小计" };

		private List<CartItemDto> items = new ArrayList<CartItemDto>();

		/**
		 * 替换数据。
		 *
		 * @param newItems 数据
		 */
		void setItems(List<CartItemDto> newItems) {
			items = newItems;
			fireTableDataChanged();
		}

		public int getRowCount() {
			return items.size();
		}

		public int getColumnCount() {
			return columns.length;
		}

		public String getColumnName(int column) {
			return columns[column];
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == COL_QUANTITY;
		}

		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == COL_QUANTITY ? Integer.class : String.class;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			CartItemDto item = items.get(rowIndex);
			switch (columnIndex) {
			case COL_PRODUCT:
				return item.getProductName();
			case COL_PRICE:
				return "￥" + item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
			case COL_QUANTITY:
				return item.getQuantity();
			default:
				return "￥" + item.subtotal().setScale(2, RoundingMode.HALF_UP).toPlainString();
			}
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex != COL_QUANTITY) {
				return;
			}
			int quantity = 1;
			if (value instanceof Number) {
				quantity = ((Number) value).intValue();
			} else if (value != null) {
				try {
					quantity = Integer.parseInt(value.toString().trim());
				} catch (NumberFormatException ignored) {
					quantity = 1;
				}
			}
			CartItemDto item = items.get(rowIndex);
			session.updateQuantity(item.getProductId(), quantity);
			fireTableRowsUpdated(rowIndex, rowIndex);
			BigDecimal total = session.cartTotal().setScale(2, RoundingMode.HALF_UP);
			totalLabel.setText("共 " + session.cartCount() + " 件，合计：￥" + total.toPlainString());
		}
	}

	/**
	 * 微调框单元格编辑器。
	 */
	private static class SpinnerEditor extends javax.swing.DefaultCellEditor {

		private static final long serialVersionUID = 1L;

		/**
		 * 构造。
		 *
		 * @param numberModel 数字模型
		 */
		SpinnerEditor(SpinnerNumberModel numberModel) {
			super(new javax.swing.JTextField());
			final JSpinner spinner = new JSpinner(numberModel);
			this.editorComponent = spinner;
			spinner.addChangeListener(new javax.swing.event.ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent event) {
					stopCellEditing();
				}
			});
		}

		@Override
		public Object getCellEditorValue() {
			return ((JSpinner) editorComponent).getValue();
		}

		@Override
		public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			JSpinner spinner = (JSpinner) editorComponent;
			int current = value instanceof Number ? ((Number) value).intValue() : 1;
			spinner.setValue(Math.max(1, current));
			return spinner;
		}
	}
}
