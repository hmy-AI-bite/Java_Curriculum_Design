package com.ascent.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.RoundingMode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.ascent.shared.dto.OrderDto;
import com.ascent.shared.dto.OrderLineDto;

/**
 * 下单成功回执。
 *
 * <p>
 * 展示真实的订单号、件数、合计和明细行，数据来自服务端返回的 {@link OrderDto}。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class ShoppingMessageDialog extends JDialog {

	/** 订单。 */
	private final OrderDto order;

	/**
	 * 构造。
	 *
	 * @param parent 父窗口
	 * @param order  订单
	 */
	public ShoppingMessageDialog(Frame parent, OrderDto order) {
		super(parent, "下单成功", true);
		this.order = order;
		buildGui();
		setResizable(false);
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * 构建界面。
	 */
	private void buildGui() {
		JPanel root = new JPanel(new BorderLayout(0, 12));
		root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
		root.add(buildHeader(), BorderLayout.NORTH);
		root.add(buildDetail(), BorderLayout.CENTER);
		root.add(buildButtons(), BorderLayout.SOUTH);
		setContentPane(root);
	}

	/**
	 * 顶部。
	 *
	 * @return 面板
	 */
	private JPanel buildHeader() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JLabel check = new JLabel("下单成功");
		check.setFont(check.getFont().deriveFont(Font.BOLD, 17f));
		check.setForeground(new Color(0x2E, 0x7D, 0x32));
		check.setAlignmentX(CENTER_ALIGNMENT);

		JLabel hint = new JLabel("订单已提交，谢谢惠顾！");
		hint.setForeground(new Color(0x66, 0x66, 0x66));
		hint.setAlignmentX(CENTER_ALIGNMENT);

		panel.add(check);
		panel.add(Box.createVerticalStrut(6));
		panel.add(hint);
		return panel;
	}

	/**
	 * 订单信息。
	 *
	 * @return 面板
	 */
	private JPanel buildDetail() {
		JPanel grid = new JPanel(new GridBagLayout());
		grid.setBorder(BorderFactory.createTitledBorder("订单信息"));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(3, 6, 3, 6);
		gbc.gridx = 0;
		gbc.gridy = 0;

		addRow(grid, gbc, "订单号", order.getOrderNo());
		addRow(grid, gbc, "下单时间", order.getCreatedAt());
		addRow(grid, gbc, "商品件数", order.totalQuantity() + " 件");
		addRow(grid, gbc, "合计金额", "￥"
				+ order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());

		JTextArea lines = new JTextArea(buildLineText(), 3, 28);
		lines.setEditable(false);
		lines.setBackground(grid.getBackground());
		lines.setBorder(BorderFactory.createTitledBorder("明细"));
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		grid.add(new JScrollPane(lines), gbc);

		return grid;
	}

	/**
	 * 明细文本。
	 *
	 * @return 文本
	 */
	private String buildLineText() {
		StringBuilder builder = new StringBuilder();
		for (OrderLineDto line : order.getLines()) {
			builder.append(line.getProductName()).append("    ￥")
					.append(line.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toPlainString())
					.append(" × ").append(line.getQuantity())
					.append(" = ￥").append(line.subtotalText()).append('\n');
		}
		return builder.toString();
	}

	/**
	 * 加一行。
	 *
	 * @param grid  网格
	 * @param gbc   约束
	 * @param name  名称
	 * @param value 值
	 */
	private void addRow(JPanel grid, GridBagConstraints gbc, String name, String value) {
		JLabel key = new JLabel(name + "：");
		key.setForeground(new Color(0x66, 0x66, 0x66));
		grid.add(key, gbc);
		gbc.gridx = 1;
		grid.add(new JLabel(value == null ? "-" : value), gbc);
		gbc.gridx = 0;
		gbc.gridy++;
	}

	/**
	 * 按钮。
	 *
	 * @return 面板
	 */
	private JPanel buildButtons() {
		JPanel panel = new JPanel();
		JButton okButton = new JButton("确定");
		UiTheme.polish(okButton);
		okButton.setPreferredSize(new Dimension(88, 30));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setVisible(false);
			}
		});
		panel.add(okButton);
		return panel;
	}
}
