package com.ascent.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.ascent.shared.dto.CartItemDto;
import com.ascent.shared.dto.LoginResult;
import com.ascent.shared.dto.ProductDto;
import com.ascent.shared.dto.UserDto;

/**
 * 客户端会话：保存登录令牌、当前用户和购物车。
 *
 * <p>
 * 它替代了原来那个 {@code static} 的购物车。每个登录成功的窗口持有一个
 * {@code Session}，退出或注销就丢弃，换账号自然互不干扰。
 * </p>
 *
 * <p>
 * 本类只在 Swing 事件分发线程里使用，不需要做线程同步。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public class Session {

	/** 登录结果，包含 token 和用户。 */
	private final LoginResult login;

	/** 购物车。 */
	private final List<CartItemDto> cart = new ArrayList<CartItemDto>();

	/**
	 * 构造会话。
	 *
	 * @param login 登录结果，不可为 null
	 */
	public Session(LoginResult login) {
		if (login == null || login.getToken() == null) {
			throw new IllegalArgumentException("登录结果不能为空");
		}
		this.login = login;
	}

	/**
	 * 会话令牌。
	 *
	 * @return token
	 */
	public String token() {
		return login.getToken();
	}

	/**
	 * 当前用户。
	 *
	 * @return 用户
	 */
	public UserDto user() {
		return login.getUser();
	}

	/**
	 * 当前用户主键。
	 *
	 * @return 用户 id
	 */
	public int userId() {
		return login.getUser() == null ? 0 : login.getUser().getId();
	}

	/**
	 * 是否管理员。
	 *
	 * @return 管理员返回 true
	 */
	public boolean isAdmin() {
		return login.getUser() != null && login.getUser().isAdmin();
	}

	// ---------------------------------------------------------------- 购物车

	/**
	 * 把商品加入购物车。同一商品再次加入时只增加数量。
	 *
	 * @param product 商品
	 */
	public void addProduct(ProductDto product) {
		if (product == null || product.getId() <= 0) {
			return;
		}
		for (CartItemDto item : cart) {
			if (item.getProductId() == product.getId()) {
				item.setQuantity(item.getQuantity() + 1);
				return;
			}
		}
		cart.add(CartItemDto.of(product));
	}

	/**
	 * 修改某商品的数量。
	 *
	 * @param productId 商品主键
	 * @param quantity  新数量
	 */
	public void updateQuantity(int productId, int quantity) {
		for (CartItemDto item : cart) {
			if (item.getProductId() == productId) {
				item.setQuantity(quantity);
				return;
			}
		}
	}

	/**
	 * 从购物车移除某商品。
	 *
	 * @param productId 商品主键
	 */
	public void removeProduct(int productId) {
		for (int i = cart.size() - 1; i >= 0; i--) {
			if (cart.get(i).getProductId() == productId) {
				cart.remove(i);
			}
		}
	}

	/**
	 * 购物车内容。
	 *
	 * @return 购物车行的副本，外部修改不影响会话
	 */
	public List<CartItemDto> cartItems() {
		return new ArrayList<CartItemDto>(cart);
	}

	/**
	 * 购物车总金额。
	 *
	 * @return 总金额
	 */
	public BigDecimal cartTotal() {
		BigDecimal total = BigDecimal.ZERO;
		for (CartItemDto item : cart) {
			total = total.add(item.subtotal());
		}
		return total;
	}

	/**
	 * 购物车总金额文本。
	 *
	 * @return 保留两位小数的金额
	 */
	public String cartTotalText() {
		return cartTotal().setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * 购物车总件数。
	 *
	 * @return 件数
	 */
	public int cartCount() {
		int count = 0;
		for (CartItemDto item : cart) {
			count += item.getQuantity();
		}
		return count;
	}

	/**
	 * 购物车是否为空。
	 *
	 * @return 为空返回 true
	 */
	public boolean isCartEmpty() {
		return cart.isEmpty();
	}

	/**
	 * 清空购物车。下单成功后调用。
	 */
	public void clearCart() {
		cart.clear();
	}
}
