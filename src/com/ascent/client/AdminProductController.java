package com.ascent.client;

import java.io.IOException;

import com.ascent.shared.ApiException;
import com.ascent.shared.dto.ProductDto;

/**
 * 管理端商品维护控制器（P1，可删减）。<b>所有方法都是阻塞的</b>。
 *
 * <p>
 * 是否管理员由服务端根据 token 判断，客户端传什么 authority 都没用；
 * 非管理员调用会收到 {@code PERMISSION_DENIED}。
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public interface AdminProductController {

	/**
	 * 新增商品。
	 *
	 * @param token   会话令牌
	 * @param product 商品，id 由服务端生成
	 * @return 写入后的商品（含 id）
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败，如无权限、参数非法
	 */
	ProductDto create(String token, ProductDto product) throws IOException, ApiException;

	/**
	 * 修改商品。
	 *
	 * @param token   会话令牌
	 * @param product 商品，id 必须存在
	 * @return 是否成功
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败，如无权限、商品不存在
	 */
	boolean update(String token, ProductDto product) throws IOException, ApiException;

	/**
	 * 删除商品。
	 *
	 * @param token     会话令牌
	 * @param productId 商品主键
	 * @return 是否成功
	 * @throws IOException  网络故障
	 * @throws ApiException 业务失败，如无权限、商品不存在
	 */
	boolean delete(String token, int productId) throws IOException, ApiException;

	/**
	 * 关闭底层连接。
	 */
	void close();
}
