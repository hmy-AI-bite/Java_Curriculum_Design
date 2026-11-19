# AscentSys 药品销售管理系统（Java 课程设计）

当前主线是 **W1 后端**：Java 11 + Socket + SQLite + JUnit。

> 旧版文本服务端（`ProductDataServer`、`Handler`、`DataAccessor`、`ProductDataAccessor`）已移除。
> 旧 Swing 客户端代码暂时保留在 `src/com/ascent/ui` 和 `src/com/ascent/util`，用于迁移参考；它还没有接入新协议，当前不能直接连新服务端。

## 当前架构

```text
客户端（A 迁移中）
   │  Request(op, payload) / Response(code, message, data)
   ▼
ServerMain → ClientHandler → RequestDispatcher
                               ├─ AuthService（已完成）
                               ├─ ProductService（W2）
                               └─ OrderService（W2）
                                      │
                                      ▼
                               UserDao / ProductDao / OrderDao
                                      │
                                      ▼
                                  SQLite
```

- 服务端入口：`com.ascent.server.ServerMain`
- 默认端口：`127.0.0.1:5150`
- 数据库：`data/ascent.db`（首次启动自动建表并迁移旧文本数据）
- 旧数据：`product.db`、`user.db`（UTF-8，仅作为迁移输入）

## 技术栈

- Java SE 11（Maven `release=11`）
- Socket + `ObjectInputStream` / `ObjectOutputStream`
- SQLite + JDBC（`sqlite-jdbc`）
- PBKDF2WithHmacSHA256 + 随机盐
- JUnit 5 + Maven
- 全项目统一 UTF-8

## 项目结构

```text
AscentSys/
├─ pom.xml                      Maven 构建
├─ db/schema.sql                SQLite 建表脚本
├─ src/resources/app.properties 端口、数据库路径、旧数据路径
├─ src/com/ascent/
│  ├─ common/                   协议、DTO、错误码（共享契约）
│  ├─ server/                   启动、连接处理、请求分发、Session
│  ├─ service/                  AuthService、PasswordUtil
│  ├─ dao/                      UserDao、UserRecord
│  ├─ db/                       Database、SchemaInitializer、DataMigration
│  ├─ bean/                     旧客户端实体（待迁移）
│  ├─ ui/                       旧 Swing 客户端（待迁移）
│  └─ util/                     旧客户端通信/购物车（待迁移）
├─ test/com/ascent/             JUnit 5 测试
├─ product.db / user.db         旧数据（UTF-8，迁移输入）
├─ docs/backend-w1.md           W1 后端说明
└─ README.md
```

`data/`、`target/`、`.m2/` 已在 `.gitignore` 中排除。

## 快速开始

```bash
# 编译 + 测试
mvn test
mvn package

# 复制运行依赖
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# 启动服务端（Windows 用 ; 分隔，Linux/macOS 用 :）
java -cp "target/classes;target/dependency/*" com.ascent.server.ServerMain
```

也可以直接在 IntelliJ 里运行 `com.ascent.server.ServerMain`，Working directory 设为项目根目录。

启动时会自动：

1. 创建 `data/ascent.db` 和四张表；
2. 第一次启动把 `product.db`/`user.db` 迁移到 SQLite；
3. 创建管理员账号。

默认账号：

- 管理员：`admin / admin123`
- 旧数据用户：`user1 / 1`

## 协议

W1 只实现登录/注册；其他操作码在 `RequestDispatcher` 中返回 `NOT_IMPLEMENTED`。

| 操作码 | 含义 | 状态 |
|---|---|---|
| 1 | 登录 | 已实现 |
| 2 | 注册 | 已实现 |
| 10/11 | 商品列表/详情 | W2 |
| 20/21 | 创建订单/我的订单 | W2 |
| 30/31 | 管理员商品增删改 | W2 |

协议对象是 `Request(op, payload)` 和 `Response(code, message, data)`，详见 `docs/backend-w1.md`。

## 已实现

- [x] Maven 构建（Java 11、UTF-8、sqlite-jdbc、JUnit 5）
- [x] SQLite 四张表：`users/products/orders/order_items`
- [x] 旧 UTF-8 文本迁移：价格转分、库存去单位、密码哈希
- [x] PBKDF2WithHmacSHA256 + 随机盐
- [x] 登录/注册 Service + DAO
- [x] `Request/Response` 协议、`ClientHandler`、`RequestDispatcher`
- [x] 服务端线程池、只监听 127.0.0.1
- [x] JUnit：密码、登录/注册、迁移
- [x] 默认管理员、旧数据迁移
- [x] 移除旧版文本服务端

## 待完善

- [ ] **客户端迁移**：A 把旧 Swing 客户端接到新 `Request/Response` 协议
- [ ] **商品模块**：`ProductDao/ProductService`、列表/搜索/详情、管理员 CRUD
- [ ] **订单模块**：`OrderDao/OrderService`、库存校验、事务回滚、订单查询
- [ ] **权限**：管理员操作在服务端用 `ClientSession` 校验
- [ ] **统一入口**：`Launcher` 支持 `server/migrate/client/all`
- [ ] **可执行 jar**：Maven shade 打 fat jar
- [ ] **测试**：库存不足回滚、并发下单、权限拒绝
- [ ] **清理旧客户端**：迁移完成后删除 `ui/` 和旧 `util/` 代码

## 课程设计点

- 面向对象分层：DTO / Service / DAO
- 集合与泛型：`List<ProductDTO>`、`Map`、`JTable` 数据模型（迁移后）
- 异常处理：`AppException` + 错误码
- I/O 与编码：旧文本 UTF-8 迁移
- 多线程：服务端 `ExecutorService`、客户端 `SwingWorker`（迁移后）
- 网络：Socket + 对象序列化
- JDBC：`PreparedStatement`、事务
- 安全：PBKDF2 + 盐、服务端 Session 鉴权
- 测试与构建：JUnit 5、Maven

## 说明

- 旧版文本服务端已删除，不再通过 `ProductDataServer` 启动。
- 旧 Swing 客户端尚未迁移，当前只作为 W2 的迁移参考，不要用它连新服务端。
- 详细的后端启动、W2 TODO 和踩坑提示见 [docs/backend-w1.md](docs/backend-w1.md)。
