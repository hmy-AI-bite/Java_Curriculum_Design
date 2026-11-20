package com.ascent.ui;

import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/**
 * 界面风格统一入口。
 *
 * <p>
 * 只在程序启动时调用一次 {@link #apply()}，之后所有窗口都使用同一套外观；
 * 切换外观时调用 {@link #change(String, Window)} 并传入需要刷新的窗口。
 * </p>
 *
 * @author ascent
 * @version 1.1
 */
public final class UiTheme {

	/** 当前使用的外观类名，null 表示还没设置。 */
	private static String currentLookAndFeel;

	/** 工具类不允许实例化。 */
	private UiTheme() {
	}

	/**
	 * 设置默认外观：优先系统外观，其次是 Nimbus，最后跨平台外观。
	 */
	public static void apply() {
		if (!tryLookAndFeel(UIManager.getSystemLookAndFeelClassName())) {
			tryLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}
	}

	/**
	 * 切换到指定外观，并刷新给定窗口及其子组件。
	 *
	 * @param className 外观类名
	 * @param window    需要刷新的窗口，可以为 null
	 * @return 是否切换成功
	 */
	public static boolean change(String className, Window window) {
		if (!tryLookAndFeel(className)) {
			return false;
		}
		if (window != null) {
			SwingUtilities.updateComponentTreeUI(window);
		}
		return true;
	}

	/**
	 * 返回当前外观类名。
	 *
	 * @return 当前外观类名，可能为 null
	 */
	public static String getCurrentLookAndFeel() {
		return currentLookAndFeel;
	}

	/**
	 * 尝试设置外观，失败时不改变当前外观。
	 *
	 * @param className 外观类名
	 * @return 是否成功
	 */
	private static boolean tryLookAndFeel(String className) {
		if (className == null) {
			return false;
		}
		try {
			UIManager.setLookAndFeel(className);
			currentLookAndFeel = className;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 给按钮设置统一的边距和字体。
	 *
	 * @param button 目标按钮
	 */
	public static void polish(AbstractButton button) {
		button.setMargin(new Insets(4, 12, 4, 12));
		button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
	}

	/**
	 * 给输入框设置统一字体。
	 *
	 * @param field 目标输入框
	 */
	public static void polish(JTextComponent field) {
		field.setFont(field.getFont().deriveFont(Font.PLAIN, 13f));
	}
}
