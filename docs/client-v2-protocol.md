# AscentSys 通信协议 v2.0

> 状态：**草案冻结（W1 对齐会确认后生效）**
> 维护：成员 A（客户端）× 成员 B（服务端）共同维护
> 变更规则：**操作码只增不改**；字段改名、语义变化必须两端同时发版，并升 `VERSION`

本文档是客户端与服务端之间唯一的契约。代码里以 `com.ascent.shared` 包为准，
文档与代码不一致时以代码为准，但必须立刻同步改文档。

---

## 1. 为什么有 v2

现有实现（v1）是两个问题叠加的结果：

- 客户端直接 `new UserDataClient()` 发请求，UI 和数据层耦合；
- 服务端把业务对象裸写回 socket，没有统一的成功/失败表达，异常还会把连接搞断。

v2 做三件事：

| 变更 | 影响 |
|---|---|
| 响应统一为 `Result<T>` 信封 | 客户端只有一条解析路径，业务失败不再断连接 |
| 引入 token 会话 | 服务端按 token 反查用户，不再信任客户端传的 `userId`/`authority` |
| 数据源换 SQLite | 支持事务扣库存；协议层不变，只是新增订单相关操作码 |

**破坏性**：`102`、`103` 的返回结构变了。旧的 `100/101/104` 语义不变，只是响应包了一层 `Result`。

---

## 2. 传输与帧格式

- 传输：TCP，默认 `localhost:5150`
- 序列化：Java 原生对象序列化（`ObjectOutputStream` / `ObjectInputStream`）
- 客户端与服务器**必须使用同一份 `com.ascent.shared` 包**。本项目前后端在同一个仓库、同一个 jar 里，天然一致；如果以后拆分，shared 包要做成独立模块，禁止两边各拷一份。
- 帧格式：

```
请求：  int opCode  →  payload（0..n 个 writeObject，顺序见第 5 节）
响应：  恰好 1 个 writeObject(Result)
```

- 一个 TCP 连接上可以连续发多个请求，按顺序一一对应响应（当前实现是同步请求-响应，不支持流水线）。
- **对象流握手顺序**：服务端必须先 `new ObjectOutputStream` 并 `flush()`，再 `new ObjectInputStream`；客户端同理。顺序反了会互相阻塞。现有 `Handler` 已经按这个顺序写。
- **每次写完必须 `reset()`**：`ObjectOutputStream` 会缓存已发送对象的句柄，同一个 DTO 第二次下发时只会写一个引用，对端反序列化出来的是**第一次的旧对象**。典型症状：下单扣了库存，但界面刷新后库存数字不变。客户端和服务器在写完一批数据后都要调用 `out.reset()`；本项目已在 `SocketConnection` 和 `MockServer` 中处理。

---

## 3. 会话与鉴权

### 3.1 登录

`OP_LOGIN` 成功后，服务端生成随机 token（`UUID.randomUUID().toString()`），返回：

```java
LoginResult { String token; UserDto user; }
```

服务端在内存里维护：

```java
Map<String, SessionInfo>   // token -> { userId, authority, expireAt }
```

- 有效期 2 小时，每次成功请求滑动续期。
- 服务重启后所有 token 失效，客户端需要重新登录（错误码 `UNAUTHENTICATED`）。
- 关闭 socket **不等于**登出，token 仍然有效，直到过期。本期不做服务端主动登出（P2）。

### 3.2 鉴权规则

| 操作码区间 | 是否要 token | 服务端动作 |
|---|---|---|
| 100–199 | 否 | 直接处理 |
| 200–299 | 是 | 按 token 反查用户，查不到返回 `UNAUTHENTICATED` |
| 300–399 | 是，且必须是管理员 | 反查用户后校验 `authority == 1`，否则 `PERMISSION_DENIED` |

**硬性要求**：服务端**只信 token**。请求体里即使带了 `userId`、`authority`，一律忽略。
验收项“普通用户调管理接口被拒”就靠这一条实现——客户端伪造字段没用。

### 3.3 管理员账号

`user.db` 里没有管理员，迁移时必须种一个，否则 300 段接口没法验收：

```
账号：admin    密码：admin123     authority：1
```

README 里要注明这是演示账号，并提示首次登录后修改（本期不做改密接口，写进 P1）。

---

## 4. 响应信封

```java
public final class Result<T> implements Serializable {
    boolean ok;
    T data;              // 成功时有值
    String errorCode;    // 失败时有值，见 ErrorCode
    String message;      // 失败时的可读提示，可直接显示给用户
}
```

约定：

- 成功：`Result.ok(data)`；无数据的成功用 `Result.ok()`（`data` 为 null）。
- 失败：`Result.fail(errorCode, message)`。
- **服务端内部异常不得直接抛到 socket**：`Handler` 捕获后统一转成
  `Result.fail(INTERNAL_ERROR, "服务器内部错误")`，堆栈只写日志。
- 可预期失败（库存不足、无权限、参数非法）用 `ApiException` 抛，`Handler` 转成对应 `errorCode`。

### 错误码

| errorCode | 场景 | 客户端表现 |
|---|---|---|
| `AUTH_FAILED` | 账号或密码错误 | 提示并清空密码框 |
| `UNAUTHENTICATED` | 未登录 / token 过期 | 跳回登录窗口 |
| `PERMISSION_DENIED` | 非管理员调用管理接口 | 提示“没有权限” |
| `USER_EXISTS` | 注册重名 | 提示换账号 |
| `VALIDATION_ERROR` | 参数为空、数量非正数 | 提示具体字段 |
| `PRODUCT_NOT_FOUND` | 商品已被删除 | 提示并刷新列表 |
| `STOCK_NOT_ENOUGH` | 库存不足 | 提示具体是哪个商品，不扣库存 |
| `EMPTY_CART` | 空购物车下单 | 前端应提前拦截 |
| `ORDER_NOT_FOUND` | 订单不存在或不属于本人 | 提示 |
| `DB_ERROR` | 数据库异常 | 提示“稍后再试” |
| `UNKNOWN_OP` | 操作码未定义 | 提示版本不一致 |
| `NETWORK_ERROR` | 仅客户端使用：连不上 | 提示检查服务端 |
| `TIMEOUT` | 仅客户端使用：超时 | 提示重试 |

---

## 5. 操作码总表

| opCode | 名称 | payload 顺序（C→S） | data（S→C） | 鉴权 |
|---|---|---|---|---|
| 100 | `GET_CATEGORIES` | 无 | `List<String>` | 公开 |
| 101 | `GET_PRODUCTS` | `String category` | `List<ProductDto>` | 公开 |
| 102 | `LOGIN` | `String username, String password` | `LoginResult` | 公开 |
| 103 | `REGISTER` | `String username, String password` | `Boolean` | 公开 |
| 104 | `GET_ALL_PRODUCTS` | 无 | `List<ProductDto>` | 公开 |
| 105 | `SEARCH_PRODUCTS` | `String keyword` | `List<ProductDto>` | 公开 |
| 200 | `PLACE_ORDER` | `String token, List<CartItemDto> items` | `OrderDto` | 登录 |
| 201 | `LIST_ORDERS` | `String token` | `List<OrderDto>` | 登录 |
| 300 | `ADMIN_CREATE_PRODUCT` | `String token, ProductDto product` | `ProductDto` | 管理员 |
| 301 | `ADMIN_UPDATE_PRODUCT` | `String token, ProductDto product` | `Boolean` | 管理员 |
| 302 | `ADMIN_DELETE_PRODUCT` | `String token, int productId` | `Boolean` | 管理员 |

说明：

- `105` 的关键字为空串时，服务端返回全部商品，等价于 `104`。
- `201` 只返回**当前 token 对应用户**的订单，服务端不接受 `userId` 参数。
- `300`–`302` 属 P1，本期可以只在服务端实现、界面不做，但协议先占位，避免以后改号。

---

## 6. DTO 字段表

金额一律用 `BigDecimal`（元，两位小数）；数据库存“分”（`INTEGER`），转换在 DAO 做。

### UserDto

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| username | String | 账号 |
| authority | int | 0 普通 / 1 管理员 |

**绝不包含密码字段。**

### LoginResult

| 字段 | 类型 | 说明 |
|---|---|---|
| token | String | 会话令牌 |
| user | UserDto | 当前用户 |

### ProductDto

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键，新增时为 0 |
| name | String | 药品名称 |
| cas | String | CAS 号 |
| structure | String | 结构图文件名 |
| formula | String | 分子式 |
| spec | String | 规格，如 `280g`（**不是库存**） |
| price | BigDecimal | 单价（元） |
| stock | int | 库存件数 |
| category | String | 分类 |

### CartItemDto

| 字段 | 类型 | 说明 |
|---|---|---|
| productId | int | 商品主键，**唯一可信字段** |
| productName | String | 仅展示 |
| unitPrice | BigDecimal | 仅展示，服务端不采信 |
| quantity | int | 数量，≥1 |

### OrderDto / OrderLineDto

| OrderDto | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| orderNo | String | 订单号，`yyyyMMddHHmmssSSS` |
| userId | int | 下单用户 |
| createdAt | String | ISO-8601，如 `2026-11-19T21:08:30` |
| totalAmount | BigDecimal | 总额 |
| lines | List\<OrderLineDto\> | 明细 |

| OrderLineDto | 类型 | 说明 |
|---|---|---|
| productId | int | 商品主键 |
| productName | String | **下单时的快照** |
| unitPrice | BigDecimal | **下单时的快照** |
| quantity | int | 数量 |

明细里的名称和单价是快照，商品以后改名调价不影响历史订单。

---

## 7. 下单事务

这是 W2 的核心，也是答辩必问的一段。

### 步骤

```
1. 校验：items 非空，每行 quantity >= 1，productId > 0
2. BEGIN IMMEDIATE
3. for each item:
     SELECT id, name, price_cents, stock FROM products WHERE id = ? （拿写锁）
     不存在          → ROLLBACK，抛 PRODUCT_NOT_FOUND
     stock < qty     → ROLLBACK，抛 STOCK_NOT_ENOUGH（消息里带商品名和剩余库存）
4. total = Σ(price_cents × qty)          ← 用库里的价格，不用客户端传的
5. INSERT INTO orders(order_no, user_id, total_cents, created_at)
6. for each item:
     INSERT INTO order_items(order_id, product_id, product_name, quantity, unit_price_cents)
     UPDATE products SET stock = stock - ? WHERE id = ?
7. COMMIT
8. 返回 OrderDto
```

### 事务边界

- 第 3 步的校验和扣减必须在**同一个事务**里，否则并发下会超卖。
- 任何一步失败都要 `ROLLBACK`，保证“要么全扣，要么全不扣”。
- SQLite 用 `BEGIN IMMEDIATE` 而不是 `BEGIN`：立即拿写锁，冲突早点暴露，
  避免两个事务都在读、提交时才发现冲突。
- JDBC 里即 `connection.setAutoCommit(false)`，成功后 `commit()`，异常 `rollback()`，
  并在 `finally` 里恢复 `setAutoCommit(true)`。

### 必须写的测试（W3 JUnit）

1. 正常下单：库存正确扣减，订单和明细行数正确，总额正确。
2. 库存不足：整单回滚，库存不变，订单表不新增记录。
3. 多行里只有一行库存不足：整单回滚，另一行也不能被扣。
4. 客户端传了伪造的低价：总额仍按库里的价格算。

---

## 8. 数据库

```sql
CREATE TABLE users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username      TEXT    NOT NULL UNIQUE,
  password_hash TEXT    NOT NULL,              -- pbkdf2_sha256$...
  authority     INTEGER NOT NULL DEFAULT 0,    -- 0 普通 1 管理员
  created_at    TEXT    NOT NULL
);

CREATE TABLE products (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  name         TEXT    NOT NULL,
  cas          TEXT,
  structure    TEXT,
  formula      TEXT,
  spec         TEXT,                           -- 原 product.db 第 6 列，如 280g
  price_cents  INTEGER NOT NULL DEFAULT 0,     -- 金额存“分”，避免 REAL 误差
  stock        INTEGER NOT NULL DEFAULT 0,
  category     TEXT    NOT NULL
);

CREATE TABLE orders (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  order_no     TEXT    NOT NULL UNIQUE,
  user_id      INTEGER NOT NULL REFERENCES users(id),
  total_cents  INTEGER NOT NULL,
  created_at   TEXT    NOT NULL
);

CREATE TABLE order_items (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id         INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id       INTEGER NOT NULL REFERENCES products(id),
  product_name     TEXT    NOT NULL,          -- 下单时快照
  quantity         INTEGER NOT NULL,
  unit_price_cents INTEGER NOT NULL            -- 下单时快照
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_orders_user       ON orders(user_id);
```

**金额为什么存分**：`REAL` 存 `1100.1` 再取出来可能是 `1100.0999999`，累加多次后
总额对不上，展示和断言都会出错。存整数分、只在展示层转元。

---

## 9. 数据迁移（GBK CSV → SQLite）

一次性脚本，可以是一个 `main` 方法或独立的 `Migration` 类，跑完就归档。

### product.db

原格式：`产品名称,CAS号,结构图,公式,价格,规格,类别`，GBK，无表头。

| 列 | 映射 | 规则 |
|---|---|---|
| 1 名称 | `name` | 去空白 |
| 2 CAS | `cas` | 去空白 |
| 3 结构图 | `structure` | 去空白 |
| 4 公式 | `formula` | 去空白 |
| 5 价格 | `price_cents` | `new BigDecimal(x).movePointRight(2).intValueExact()` |
| 6 规格 | `spec` | 原样保留，如 `280g` |
| 7 类别 | `category` | 去空白 |

**库存怎么来**：原文件没有库存字段。迁移时从规格里解析开头的数字作为初始库存
（`280g` → 280），解析不出来给 `100`。写进迁移日志，方便答辩时解释。

### user.db

原格式：`账号,密码,权限`，GBK，明文密码。

- 每行算出 PBKDF2 哈希后写入 `users.password_hash`，**不保留明文**。
- `authority` 原样写入。
- 额外插入管理员：`admin / admin123 / 1`。
- 重复账号只保留第一条，其余写日志跳过。

### 编码

迁移后的 `asm.db`（SQLite）与全部源码统一 UTF-8；
`product.db` / `user.db` 只作为迁移输入保留，不再被运行时读取。

---

## 10. 密码哈希

```java
算法：PBKDF2WithHmacSHA256
迭代：100_000
盐：16 字节，SecureRandom
派生长度：256 bit
存储：pbkdf2_sha256$100000$<saltBase64>$<hashBase64>
校验：MessageDigest.isEqual(期望, 实际)   // 常量时间，防时序攻击
```

要点：

- 每个用户独立随机盐，同样的密码在库里哈希也不同。
- 迭代次数写进存储串，以后调高不用改老数据。
- 校验失败一律返回 `AUTH_FAILED`，不区分“账号不存在”和“密码错误”，避免账号枚举。

---

## 11. 客户端约定

### 11.1 控制器接口

窗口只依赖 `com.ascent.client` 下的接口：

- `AuthController`：`login` / `register` / `close`
- `ProductController`：`loadCategories` / `loadProducts` / `searchProducts` / `close`
- `OrderController`：`placeOrder` / `listMyOrders` / `close`
- `AdminProductController`：`create` / `update` / `delete` / `close`（P1）

**所有方法都是阻塞的**，内部走 socket。

### 11.2 调用方式

后台线程执行，EDT 更新界面，直接复用现有 `AsyncRunner`：

```java
AsyncRunner.run(
    () -> productController.loadProducts(category),
    list -> tableModel.setData(list),
    error -> statusLabel.setText(friendly(error)),
    () -> setBusy(false));
```

### 11.3 会话

登录成功后 `new Session(loginResult)`，由主窗口持有并向下传：

- `session.token()`：所有需要登录的请求都带它；
- `session.cartItems()`：购物车；
- `session.clearCart()`：下单成功后调用。

**禁止**再用 `static` 字段存购物车或用户——这正是 v1 的缺陷之一。

### 11.4 错误提示

`IOException` → 网络问题（“服务端未启动/连接超时”）；
`ApiException` → 直接展示 `getMessage()`，按 `getErrorCode()` 决定是否跳回登录页。

---

## 12. 迁移期策略

- W1 期间 `src/` 里的旧代码（`ProtocolPort`、`UserDataClient`、`ProductDataClient`）先保留，保证还能跑。
- `com.ascent.shared` 新增到 `contract/src`，等 B 完成 UTF-8 迁移后整体并入 `src/`。
- 旧 `ProtocolPort` 在 W2 结束时删除，`ERR` 日志里如果还出现 100–104 的旧格式，说明有地方没改干净。
- 客户端与服务端**必须同时更新**：新旧混跑会出现 `ClassCastException` 或反序列化失败，这是预期行为，不是 bug。

---

## 13. 验收清单

### W1

- [ ] `mvn package` 通过，jar 能起服务端和客户端
- [ ] 登录走 `102`，返回 `LoginResult`，界面能建 `Session`
- [ ] 注册走 `103`，重名返回 `USER_EXISTS`
- [ ] 数据库 `users.password_hash` 是 `pbkdf2_sha256$...`，不是明文
- [ ] `product.db` / `user.db` 已导入 SQLite，商品数、用户数一致
- [ ] 存在管理员账号 `admin`

### W2

- [ ] 下单成功，`products.stock` 按数量减少
- [ ] 库存不足时整单回滚：库存不变、`orders` 无新记录
- [ ] 普通用户带合法 token 调 `300` 返回 `PERMISSION_DENIED`
- [ ] 两个用户各自登录，购物车内容互不影响
- [ ] 客户端伪造 `unitPrice` 不起作用，总额按库里价格算

### W3

- [ ] `mvn test` 全绿，含第 7 节列出的 4 个事务测试
- [ ] `java -jar` 起服务端/客户端，5 分钟演示链路完整
- [ ] 断网时界面提示而不是崩

---

## 14. 变更流程

1. 任何人要加操作码，先在群里发「号段 + 名称 + payload」，确认没人占用再写。
2. 只允许**追加**新号，禁止复用已删除的号。
3. 改字段名或语义 → 升 `Protocol.VERSION`，两端同一天合入。
4. 文档与代码同一提交，代码改了文档没改视为未完成。
