package com.ascent.server.mock;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ascent.shared.ApiException;
import com.ascent.shared.ErrorCode;
import com.ascent.shared.Protocol;
import com.ascent.shared.Result;
import com.ascent.shared.dto.CartItemDto;
import com.ascent.shared.dto.LoginResult;
import com.ascent.shared.dto.OrderDto;
import com.ascent.shared.dto.OrderLineDto;
import com.ascent.shared.dto.ProductDto;
import com.ascent.shared.dto.UserDto;

/**
 * v2 协议的<b>本地模拟服务端</b>。
 *
 * <p>
 * 用途：在真正的 Service/DAO 完成之前，让客户端能独立联调。
 * 数据从 {@code product.db}（GBK）读入内存，用户硬编码 {@code user1/1} 和
 * {@code admin/admin123}，订单只存在内存里，重启即丢。
 * </p>
 *
 * <p>
 * <b>这不是最终服务端</b>。W2 的正式实现是 {@code Handler} + Service + DAO + SQLite；
 * 本类只保留到联调结束，之后删掉。它的价值是把协议跑通，并给后端一份分发顺序的参考。
 * </p>
 *
 * <p>
 * 启动：{@code java -cp bin com.ascent.server.mock.MockServer [port]}
 * </p>
 *
 * @author ascent
 * @version 2.0
 */
public final class MockServer {

	/** 商品数据文件编码。 */
	private static final Charset GBK = Charset.forName("GBK");

	/** 默认商品数据文件。 */
	private static final String PRODUCT_FILE = "product.db";

	/** 规格开头的数字。 */
	private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*(\\d+)");

	/** 商品，按 id 索引。 */
	private final Map<Integer, ProductDto> products = new LinkedHashMap<Integer, ProductDto>();

	/** 用户，按账号索引（模拟用，密码明文只存在内存）。 */
	private final Map<String, UserDto> users = new HashMap<String, UserDto>();

	/** 账号到密码。 */
	private final Map<String, String> passwords = new HashMap<String, String>();

	/** token 到用户。 */
	private final Map<String, UserDto> tokens = new ConcurrentHashMap<String, UserDto>();

	/** 订单。 */
	private final List<OrderDto> orders = Collections.synchronizedList(new ArrayList<OrderDto>());

	/** 订单序号。 */
	private final AtomicInteger orderSeq = new AtomicInteger(1);

	/**
	 * 构造并加载数据。
	 *
	 * @throws IOException 读取商品文件失败
	 */
	public MockServer() throws IOException {
		loadProducts();
		seedUsers();
	}

	/**
	 * 启动模拟服务端。
	 *
	 * @param args 可选：端口号
	 * @throws IOException 启动失败
	 */
	public static void main(String[] args) throws IOException {
		int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
		MockServer server = new MockServer();
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("MockServer 已启动：" + port + "，协议 v" + Protocol.VERSION);
			System.out.println("  商品 " + server.products.size() + " 个，账号 user1/1、admin/admin123");
			while (true) {
				Socket socket = serverSocket.accept();
				Thread worker = new Thread(server.new SessionWorker(socket));
				worker.setDaemon(true);
				worker.start();
			}
		}
	}

	// ------------------------------------------------------------ 数据

	/**
	 * 从 product.db 读商品。
	 *
	 * @throws IOException 读失败
	 */
	private void loadProducts() throws IOException {
		Path path = Paths.get(PRODUCT_FILE);
		if (!Files.exists(path)) {
			throw new IOException("找不到 " + PRODUCT_FILE + "，请在项目根目录启动");
		}
		int id = 1;
		for (String line : readLines(path)) {
			if (line.trim().isEmpty()) {
				continue;
			}
			String[] f = line.split(",", -1);
			if (f.length < 7) {
				continue;
			}
			ProductDto p = new ProductDto();
			p.setId(id++);
			p.setName(f[0].trim());
			p.setCas(f[1].trim());
			p.setStructure(f[2].trim());
			p.setFormula(f[3].trim());
			p.setPrice(parsePrice(f[4].trim()));
			p.setSpec(f[5].trim());
			p.setStock(stockFromSpec(f[5].trim()));
			p.setCategory(f[6].trim());
			products.put(p.getId(), p);
		}
	}

	/**
	 * 读取文本行，自动识别编码。
	 *
	 * <p>
	 * 数据文件在不同提交里出现过 GBK、UTF-8、UTF-16LE（带 BOM）三种编码，
	 * 这里按 BOM 判断，没有 BOM 时先试 UTF-8，失败再按 GBK 读，避免因为换编码就跑不起来。
	 * </p>
	 *
	 * @param path 文件
	 * @return 行列表
	 * @throws IOException 读失败
	 */
	private static List<String> readLines(Path path) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		Charset charset = StandardCharsets.UTF_8;
		int offset = 0;
		if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
			charset = StandardCharsets.UTF_16LE;
			offset = 2;
		} else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
			charset = StandardCharsets.UTF_16BE;
			offset = 2;
		} else if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
				&& (bytes[2] & 0xFF) == 0xBF) {
			offset = 3;
		} else {
			CharsetDecoder strict = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT);
			try {
				strict.decode(ByteBuffer.wrap(bytes));
			} catch (CharacterCodingException e) {
				charset = GBK;
			}
		}
		String text = new String(bytes, offset, bytes.length - offset, charset);
		List<String> lines = new ArrayList<String>();
		for (String line : text.split("\\R")) {
			if (!line.trim().isEmpty()) {
				lines.add(line);
			}
		}
		return lines;
	}

	/**
	 * 初始化演示账号。
	 */
	private void seedUsers() {
		addUser("user1", "1", 0);
		addUser("admin", "admin123", 1);
	}

	/**
	 * 添加用户。
	 *
	 * @param username  账号
	 * @param password  密码
	 * @param authority 权限
	 */
	private void addUser(String username, String password, int authority) {
		int id = users.size() + 1;
		users.put(username, new UserDto(id, username, authority));
		passwords.put(username, password);
	}

	/**
	 * 解析价格，失败返回 0。
	 *
	 * @param text 文本
	 * @return 金额
	 */
	private static BigDecimal parsePrice(String text) {
		try {
			return new BigDecimal(text.replace("￥", "").replace(",", ""));
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	/**
	 * 从规格解析库存。
	 *
	 * @param spec 规格
	 * @return 库存
	 */
	private static int stockFromSpec(String spec) {
		Matcher matcher = LEADING_NUMBER.matcher(spec == null ? "" : spec);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : 100;
	}

	// ------------------------------------------------------------ 连接处理

	/**
	 * 单个连接的工作线程。
	 */
	private final class SessionWorker implements Runnable {

		/** 客户端连接。 */
		private final Socket socket;

		/**
		 * 构造 worker。
		 *
		 * @param socket 客户端连接
		 */
		SessionWorker(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try (Socket client = socket) {
				ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
				out.flush();
				ObjectInputStream in = new ObjectInputStream(client.getInputStream());
				while (!client.isClosed()) {
					int opCode;
					try {
						opCode = in.readInt();
					} catch (EOFException | SocketException e) {
						break;
					}
					System.out.println("  ← " + Protocol.nameOf(opCode));
					Result<?> result;
					try {
						result = handle(opCode, in);
					} catch (ApiException e) {
						result = Result.fail(e.getErrorCode(), e.getMessage());
					} catch (Exception e) {
						System.out.println("  处理异常：" + e);
						result = Result.fail(ErrorCode.INTERNAL_ERROR, "服务器内部错误");
					}
					out.writeObject(result);
					out.flush();
					// 必须清缓存，否则同一个商品对象第二次下发会被序列化成引用，
					// 客户端看到的是第一次的库存值。
					out.reset();
				}
			} catch (IOException e) {
				System.out.println("  连接结束：" + e.getMessage());
			}
		}
	}

	/**
	 * 分发请求。
	 *
	 * @param opCode 操作码
	 * @param in     输入流
	 * @return 响应
	 * @throws Exception 处理失败
	 */
	private Result<?> handle(int opCode, ObjectInputStream in) throws Exception {
		switch (opCode) {
		case Protocol.OP_GET_CATEGORIES:
			return Result.ok(categories());
		case Protocol.OP_GET_PRODUCTS: {
			String category = (String) in.readObject();
			return Result.ok(filterByCategory(category));
		}
		case Protocol.OP_LOGIN: {
			String username = (String) in.readObject();
			String password = (String) in.readObject();
			return Result.ok(login(username, password));
		}
		case Protocol.OP_REGISTER: {
			String username = (String) in.readObject();
			String password = (String) in.readObject();
			return Result.ok(register(username, password));
		}
		case Protocol.OP_GET_ALL_PRODUCTS:
			return Result.ok(new ArrayList<ProductDto>(products.values()));
		case Protocol.OP_SEARCH_PRODUCTS: {
			String keyword = (String) in.readObject();
			return Result.ok(search(keyword));
		}
		case Protocol.OP_PLACE_ORDER: {
			String token = (String) in.readObject();
			@SuppressWarnings("unchecked")
			List<CartItemDto> items = (List<CartItemDto>) in.readObject();
			return Result.ok(placeOrder(requireUser(token), items));
		}
		case Protocol.OP_LIST_ORDERS: {
			String token = (String) in.readObject();
			return Result.ok(listOrders(requireUser(token)));
		}
		case Protocol.OP_ADMIN_CREATE_PRODUCT: {
			String token = (String) in.readObject();
			ProductDto product = (ProductDto) in.readObject();
			requireAdmin(token);
			return Result.ok(createProduct(product));
		}
		case Protocol.OP_ADMIN_UPDATE_PRODUCT: {
			String token = (String) in.readObject();
			ProductDto product = (ProductDto) in.readObject();
			requireAdmin(token);
			return Result.ok(updateProduct(product));
		}
		case Protocol.OP_ADMIN_DELETE_PRODUCT: {
			String token = (String) in.readObject();
			int productId = (Integer) in.readObject();
			requireAdmin(token);
			return Result.ok(products.remove(productId) != null);
		}
		default:
			throw new ApiException(ErrorCode.UNKNOWN_OP, "未知操作码：" + opCode);
		}
	}

	// ------------------------------------------------------------ 业务

	/**
	 * 全部分类，排序去重。
	 *
	 * @return 分类
	 */
	private List<String> categories() {
		Set<String> set = new LinkedHashSet<String>();
		for (ProductDto p : products.values()) {
			set.add(p.getCategory());
		}
		List<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		return list;
	}

	/**
	 * 按分类过滤。
	 *
	 * @param category 分类
	 * @return 商品
	 */
	private List<ProductDto> filterByCategory(String category) {
		List<ProductDto> list = new ArrayList<ProductDto>();
		for (ProductDto p : products.values()) {
			if (category != null && category.equals(p.getCategory())) {
				list.add(p);
			}
		}
		return list;
	}

	/**
	 * 关键字搜索。
	 *
	 * @param keyword 关键字
	 * @return 商品
	 */
	private List<ProductDto> search(String keyword) {
		String key = keyword == null ? "" : keyword.trim().toLowerCase();
		List<ProductDto> list = new ArrayList<ProductDto>();
		for (ProductDto p : products.values()) {
			if (key.isEmpty() || contains(p.getName(), key) || contains(p.getCas(), key)) {
				list.add(p);
			}
		}
		return list;
	}

	/**
	 * 不区分大小写包含。
	 *
	 * @param text 文本
	 * @param key  关键字
	 * @return 包含返回 true
	 */
	private static boolean contains(String text, String key) {
		return text != null && text.toLowerCase().contains(key);
	}

	/**
	 * 登录。
	 *
	 * @param username 账号
	 * @param password 密码
	 * @return 登录结果
	 * @throws ApiException 校验失败
	 */
	private LoginResult login(String username, String password) throws ApiException {
		String expected = passwords.get(username);
		if (expected == null || !expected.equals(password)) {
			throw new ApiException(ErrorCode.AUTH_FAILED, "账号或密码错误");
		}
		UserDto user = users.get(username);
		String token = UUID.randomUUID().toString();
		tokens.put(token, user);
		return new LoginResult(token, user);
	}

	/**
	 * 注册。
	 *
	 * @param username 账号
	 * @param password 密码
	 * @return 是否成功
	 * @throws ApiException 参数非法或重名
	 */
	private Boolean register(String username, String password) throws ApiException {
		if (username == null || username.trim().isEmpty()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "账号不能为空");
		}
		if (password == null || password.isEmpty()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "密码不能为空");
		}
		if (users.containsKey(username)) {
			throw new ApiException(ErrorCode.USER_EXISTS, "用户名已存在");
		}
		addUser(username, password, 0);
		return Boolean.TRUE;
	}

	/**
	 * 按 token 取用户。
	 *
	 * @param token 令牌
	 * @return 用户
	 * @throws ApiException 未登录
	 */
	private UserDto requireUser(String token) throws ApiException {
		UserDto user = tokens.get(token);
		if (user == null) {
			throw new ApiException(ErrorCode.UNAUTHENTICATED, "请重新登录");
		}
		return user;
	}

	/**
	 * 校验管理员。
	 *
	 * @param token 令牌
	 * @throws ApiException 非管理员
	 */
	private void requireAdmin(String token) throws ApiException {
		if (!requireUser(token).isAdmin()) {
			throw new ApiException(ErrorCode.PERMISSION_DENIED, "需要管理员权限");
		}
	}

	/**
	 * 下单：先整体校验库存，再统一扣减，模拟事务的“全成功或全失败”。
	 *
	 * @param user  下单用户
	 * @param items 购物车
	 * @return 订单
	 * @throws ApiException 购物车为空、商品不存在、库存不足
	 */
	private OrderDto placeOrder(UserDto user, List<CartItemDto> items) throws ApiException {
		if (items == null || items.isEmpty()) {
			throw new ApiException(ErrorCode.EMPTY_CART, "购物车为空");
		}
		for (CartItemDto item : items) {
			ProductDto product = products.get(item.getProductId());
			if (product == null) {
				throw new ApiException(ErrorCode.PRODUCT_NOT_FOUND, "商品不存在：" + item.getProductName());
			}
			if (product.getStock() < item.getQuantity()) {
				throw new ApiException(ErrorCode.STOCK_NOT_ENOUGH,
						product.getName() + " 库存不足，剩余 " + product.getStock());
			}
		}
		OrderDto order = new OrderDto();
		order.setOrderNo(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
				+ String.format("%03d", orderSeq.getAndIncrement()));
		order.setUserId(user.getId());
		order.setCreatedAt(LocalDateTime.now().withNano(0).toString());
		BigDecimal total = BigDecimal.ZERO;
		List<OrderLineDto> lines = new ArrayList<OrderLineDto>();
		for (CartItemDto item : items) {
			ProductDto product = products.get(item.getProductId());
			product.setStock(product.getStock() - item.getQuantity());
			OrderLineDto line = new OrderLineDto(product.getId(), product.getName(),
					product.getPrice(), item.getQuantity());
			lines.add(line);
			total = total.add(line.subtotal());
		}
		order.setLines(lines);
		order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
		orders.add(order);
		return order;
	}

	/**
	 * 查询当前用户订单，倒序。
	 *
	 * @param user 用户
	 * @return 订单
	 */
	private List<OrderDto> listOrders(UserDto user) {
		List<OrderDto> mine = new ArrayList<OrderDto>();
		synchronized (orders) {
			for (OrderDto order : orders) {
				if (order.getUserId() == user.getId()) {
					mine.add(order);
				}
			}
		}
		Collections.reverse(mine);
		return mine;
	}

	/**
	 * 新增商品。
	 *
	 * @param product 商品
	 * @return 写入后的商品
	 */
	private ProductDto createProduct(ProductDto product) {
		int id = products.size() + 1;
		while (products.containsKey(id)) {
			id++;
		}
		product.setId(id);
		products.put(id, product);
		return product;
	}

	/**
	 * 修改商品。
	 *
	 * @param product 商品
	 * @return 是否成功
	 */
	private Boolean updateProduct(ProductDto product) {
		if (!products.containsKey(product.getId())) {
			return Boolean.FALSE;
		}
		products.put(product.getId(), product);
		return Boolean.TRUE;
	}
}
