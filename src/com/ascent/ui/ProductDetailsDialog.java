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
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.ascent.client.Session;
import com.ascent.shared.dto.ProductDto;

/**
 * 商品详情对话框。
 *
 * <p>
 * “加入购物车”直接作用于传入的 {@link Session}，不再自己 new 一个购物车。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
@SuppressWarnings("serial")
public class ProductDetailsDialog extends JDialog {

	/** 商品。 */
	private final ProductDto product;

	/** 会话。 */
	private final Session session;

	/** 是否已加入购物车。 */
	private boolean addedToCart;

	/**
	 * 构造。
	 *
	 * @param parent  父窗口
	 * @param product 商品
	 * @param session 会话
	 */
	public ProductDetailsDialog(Frame parent, ProductDto product, Session session) {
		super(parent, "药品详细信息 - " + product.getName(), true);
		this.product = product;
		this.session = session;

		buildGui();
		setMinimumSize(new Dimension(520, 340));
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * 是否已加入购物车。
	 *
	 * @return 加入过返回 true
	 */
	public boolean isAddedToCart() {
		return addedToCart;
	}

	/**
	 * 构建界面。
	 */
	private void buildGui() {
		JPanel root = new JPanel(new BorderLayout(12, 12));
		root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
		root.add(buildInfoPanel(), BorderLayout.CENTER);
		root.add(buildImagePanel(), BorderLayout.EAST);
		root.add(buildButtonPanel(), BorderLayout.SOUTH);
		setContentPane(root);
	}

	/**
	 * 左侧信息。
	 *
	 * @return 信息面板
	 */
	private JPanel buildInfoPanel() {
		JPanel info = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(4, 0, 4, 12);
		gbc.gridx = 0;
		gbc.gridy = 0;

		addField(info, gbc, "产品名", product.getName(), true);
		addField(info, gbc, "CAS 号", product.getCas(), false);
		addField(info, gbc, "分子式", product.getFormula(), false);
		addField(info, gbc, "规格", product.getSpec(), false);
		addField(info, gbc, "单价", "￥" + product.getPrice()
				.setScale(2, RoundingMode.HALF_UP).toPlainString(), false);
		addField(info, gbc, "库存", product.getStock() + " 件", false);
		addField(info, gbc, "分类", product.getCategory(), false);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(info, BorderLayout.NORTH);
		return wrapper;
	}

	/**
	 * 加一行。
	 *
	 * @param grid   网格
	 * @param gbc    约束
	 * @param name   名称
	 * @param value  值
	 * @param strong 是否加粗
	 */
	private void addField(JPanel grid, GridBagConstraints gbc, String name, String value,
			boolean strong) {
		JLabel key = new JLabel(name + "：");
		key.setForeground(new Color(0x66, 0x66, 0x66));
		JLabel val = new JLabel(value == null || value.trim().isEmpty() ? "-" : value);
		if (strong) {
			val.setFont(val.getFont().deriveFont(Font.BOLD, 15f));
		}
		grid.add(key, gbc);
		gbc.gridx = 1;
		grid.add(val, gbc);
		gbc.gridx = 0;
		gbc.gridy++;
	}

	/**
	 * 右侧结构图。
	 *
	 * @return 图片面板
	 */
	private JPanel buildImagePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("结构图"));

		JLabel picture;
		URL imageUrl = getClass().getResource("/images/" + product.getStructure());
		if (imageUrl == null) {
			picture = new JLabel("（暂无结构图）");
			picture.setForeground(new Color(0x99, 0x99, 0x99));
		} else {
			picture = new JLabel(new ImageIcon(imageUrl));
			picture.setToolTipText(product.getName());
		}
		picture.setAlignmentX(CENTER_ALIGNMENT);

		JScrollPane scroll = new JScrollPane(picture);
		scroll.setBorder(null);
		scroll.setPreferredSize(new Dimension(220, 220));

		panel.add(Box.createVerticalStrut(4));
		panel.add(scroll);
		return panel;
	}

	/**
	 * 底部按钮。
	 *
	 * @return 按钮面板
	 */
	private JPanel buildButtonPanel() {
		JPanel buttons = new JPanel();
		JButton closeButton = new JButton("关闭");
		JButton addButton = new JButton("加入购物车");
		UiTheme.polish(closeButton);
		UiTheme.polish(addButton);
		addButton.setPreferredSize(new Dimension(110, 30));

		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setVisible(false);
			}
		});
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (session != null) {
					session.addProduct(product);
					addedToCart = true;
				}
				setVisible(false);
			}
		});

		buttons.add(closeButton);
		buttons.add(addButton);
		return buttons;
	}
}
