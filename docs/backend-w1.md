# W1 后端框架说明

> 负责人：后端 B（你）
> 范围：Maven + SQLite + 登录/注册 + 协议骨架 + JUnit
> 目标：W1 结束时 `mvn test` 全绿，服务端能启动，登录/注册走 SQLite。
>
> 旧版文本服务端（`ProductDataServer`、`Handler`、`DataAccessor`、
> `ProductDataAccessor`）已删除；旧 Swing 客户端代码暂留，等 A 迁移到新协议。

## 1. 本周围绕什么做

```text
Client (A 负责)
   │  Request(op, payload) / Response(code, message, data)
   ▼
ServerMain → ClientHandler → RequestDispatcher
                               ├─ AuthService（W1）
                               ├─ ProductService（W2）
                               └─ OrderService（W2）
                                      │
                                      ▼
                               UserDao / ProductDao / OrderDao
                                      │
                                      ▼
                                  SQLite
```

W1 只实现 `LOGIN`、`REGISTER` 两个操作码；商品、订单、管理端操作在
`RequestDispatcher` 里统一返回 `NOT_IMPLEMENTED`，W2 再补。

## 2. 目录

```text
pom.xml                          Maven 构建
db/schema.sql                    建表 SQL（构建时复制到 classpath:/db）
src/resources/app.properties     端口、数据库路径、旧数据路径
src/com/ascent/common/           协议、DTO、错误码（共享契约）
src/com/ascent/server/           启动、连接处理、请求分发、Session
src/com/ascent/service/          AuthService、PasswordUtil
src/com/ascent/dao/              UserDao、UserRecord
src/com/ascent/db/               Database、SchemaInitializer、DataMigration
test/com/ascent/                 JUnit 5 测试
```

> 说明：当前仓库还是 Eclipse 的 `src/com/ascent` 结构，W1 让 Maven 以 `src`
> 为源码根，只编译 `common/server/service/dao/db` 新包；旧的 `bean/ui/util`
> 暂不参与 Maven 构建，等 A 迁移 UI 后再统一到 `src/main/java`。

## 3. 运行

### 3.1 编译/测试

```bash
mvn test
mvn package
```

如果本机没有 `mvn`，可以用 IntelliJ 自带的 Maven：

```powershell
& 'D:\IDEA\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd' test
```

> 本机沙箱下 Maven 默认仓库 `C:\Users\...\.m2` 不可写，可临时设置
> `MAVEN_OPTS="-Duser.home=<项目目录>"`，让仓库落到项目下；
> 正常电脑不需要这一步。

### 3.2 启动服务端

```bash
java -cp "target/classes;target/dependency/*" com.ascent.server.ServerMain
```

如果还没有 `target/dependency`，用 Maven 复制依赖：

```bash
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
```

Windows 用 `;` 分隔 classpath，Linux/macOS 用 `:`。

启动会自动：
1. 建表；
2. 第一次启动把 `product.db`/`user.db`（UTF-8）迁移到 `data/ascent.db`；
3. 创建管理员 `admin / admin123`（仅当不存在时）。

### 3.3 连接测试

用 A 的客户端，或者自己写一个最小 Socket 客户端：

```java
try (Socket socket = new Socket("127.0.0.1", 5150)) {
    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
    out.flush(); // 必须先 flush，再创建 ObjectInputStream
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

    out.writeObject(new Request(OpCode.LOGIN, new LoginRequest("admin", "admin123")));
    out.flush();
    Response response = (Response) in.readObject();
    System.out.println(response);
}
```

## 4. W1 已实现

- [x] Maven 构建（Java 11、UTF-8、sqlite-jdbc、JUnit 5）
- [x] SQLite 建表：`users/products/orders/order_items`
- [x] 旧 UTF-8 文本迁移：编码、价格转分、库存去单位、密码哈希
- [x] PBKDF2WithHmacSHA256 + 随机盐
- [x] 登录/注册 Service + DAO
- [x] `Request/Response` 协议、`ClientHandler`、`RequestDispatcher`
- [x] 服务端线程池、只监听 127.0.0.1
- [x] JUnit：密码、登录/注册、迁移

## 5. W2 要接的地方

1. `RequestDispatcher` 增加 `LIST_PRODUCTS/GET_PRODUCT/CREATE_ORDER/LIST_ORDERS`
   和管理员操作；管理员操作前先 `session.isAdmin()`。
2. 新增 `ProductDao/ProductService`、`OrderDao/OrderService`。
3. `OrderService` 的事务：
   ```text
   setAutoCommit(false)
   → 查价格/库存
   → UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?
   → INSERT orders / order_items
   → commit；任何一步失败 rollback
   ```
4. 加 `ProductService` 的搜索（keyword/category）和管理员 CRUD。
5. 做 `mvn package` 的可执行 jar（shade 插件）。
6. 补 JUnit：库存不足回滚、并发下单、权限拒绝。

## 6. 容易踩的坑

- **UTF-8**：旧文件已统一为 UTF-8，读取时仍显式 `StandardCharsets.UTF_8`，不要依赖系统默认编码。
- **对象流**：先创建 `ObjectOutputStream` 并 `flush()`，再创建
  `ObjectInputStream`；否则两端可能互相等待 header。
- **SQLite 外键**：每个连接都要 `PRAGMA foreign_keys = ON`。
- **金额**：数据库存分（long），不要用 double。
- **权限**：客户端传的 role 不可信，必须用服务端 `ClientSession`。
- **密码**：日志、异常信息里不要出现明文密码。
- **表名拼接**：`DataMigration.count()` 只接受本类常量，真实项目不要拼接用户输入。
