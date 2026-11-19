# AscentSys 药品销售管理系统（Java 课程设计）

基于 **Java SE 11 + Swing** 的 C/S 桌面应用。客户端负责登录、注册、商品浏览、商品详情和购物车；服务端通过 **TCP 5150** 提供数据。项目不依赖数据库，商品和用户数据保存在两个 GBK 编码的文本文件中。

仓库名：`Java_Curriculum_Design`　项目名：`AscentSys`

## 技术栈

- Java SE 11（源码/字节码目标 11）
- Swing / AWT 桌面界面
- Socket + `ObjectInputStream` / `ObjectOutputStream`（Java 对象序列化）
- 逗号分隔文本文件（CSV 风格）作为数据源
- Eclipse / IntelliJ IDEA 工程

## 项目结构

```text
AscentSys/
├─ src/
│  ├─ com/ascent/bean/       Product.java、User.java
│  ├─ com/ascent/ui/         Ascentsys、LoginFrame、RegistFrame、MainFrame、
│  │                         ProductPanel、ProductDetailsDialog、
│  │                         ShoppingCartDialog、ShoppingMessageDialog
│  ├─ com/ascent/util/       ProtocolPort、ProductDataServer、Handler、
│  │                         ProductDataClient、UserDataClient、
│  │                         DataAccessor、ProductDataAccessor、ShoppingCart
│  └─ images/ss.jpg          商品结构图
├─ product.db                商品数据（7 列，GBK）
├─ user.db                   用户数据（3 列）
├─ .project/.classpath       Eclipse 工程配置
├─ .settings/                Eclipse 编译设置（Java 11）
├─ bin/                      编译输出（.gitignore 忽略）
└─ README.md
```

## 运行环境

| 项目 | 要求 |
|---|---|
| JDK | 11 及以上（已在 JDK 21 / 25 验证） |
| 图形环境 | 支持 Swing 的桌面会话 |
| 网络 | 本机 5150 端口可用 |
| 第三方依赖 | 无，不需要 Maven/Gradle |
| 数据文件 | 项目根目录下的 `product.db`、`user.db` |

## 启动方式

> 源码和 `product.db` 都是 **GBK** 编码。编译必须加 `-encoding GBK`，运行必须加 `-Dfile.encoding=GBK`，否则会编译失败或中文乱码。

### 1. 获取代码

```bash
git clone https://github.com/hmy-AI-bite/Java_Curriculum_Design.git
cd Java_Curriculum_Design
```

### 2. 编译

Windows PowerShell：

```powershell
$files = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName
javac -encoding GBK --release 11 -d bin $files
New-Item -ItemType Directory -Force bin\images | Out-Null
Copy-Item src\images\* bin\images -Force
```

macOS / Linux：

```bash
mkdir -p bin/images
find src -name '*.java' -print > sources.txt
javac -encoding GBK --release 11 -d bin @sources.txt
cp -r src/images/. bin/images/
rm sources.txt
```

### 3. 启动（两个终端）

先启动服务端：

```bash
java "-Dfile.encoding=GBK" -cp bin com.ascent.util.ProductDataServer
```

看到 `服务器准备就绪!` 后，再启动客户端：

```bash
java "-Dfile.encoding=GBK" -cp bin com.ascent.ui.Ascentsys
```

> 工作目录必须是项目根目录，`product.db` 和 `user.db` 是按相对路径读取的。
> 客户端必须先能连上服务端，否则登录会提示“服务器连接失败”。

### 4. 示例账号

`user1 / 1`（更多账号见 `user.db`）。

### 5. 用 IDE 运行

**IntelliJ IDEA**

1. `Open` 项目根目录，Project SDK 选择 JDK 11+，Language level 选 11。
2. `Settings → Editor → File Encodings`，把 Project Encoding 设为 `GBK`。
3. 建两个 Application 运行配置：
   - 服务端：`com.ascent.util.ProductDataServer`
   - 客户端：`com.ascent.ui.Ascentsys`
4. 两个配置的 VM options 都加 `-Dfile.encoding=GBK`，Working directory 设为项目根目录。

**Eclipse**

1. `File → Import → Existing Projects into Workspace` 导入项目。
2. `Window → Preferences → General → Workspace → Text file encoding` 选择 `Other: GBK`。
3. 编译级别设为 Java 11，运行配置的 VM arguments 加 `-Dfile.encoding=GBK`。

## 数据文件

两个文件都是无表头的逗号分隔文本，**GBK 编码**：

| 文件 | 每行格式 | 说明 |
|---|---|---|
| `product.db` | `产品名称,CAS号,结构图文件名,分子式,价格,数量,类别` | 启动时读入内存；商品数据只读 |
| `user.db` | `账号,密码,权限` | 权限字段当前未参与判断；注册时追加一行 |

`product.db` 的“结构图文件名”对应 `src/images/` 下的图片，例如 `ss.jpg`。

## 通信协议

服务端监听 `localhost:5150`，使用 Java 对象序列化传输数据。客户端先发一个 `int` 操作码：

| 操作码 | 常量 | 请求内容 | 响应 |
|---|---|---|---|
| 100 | `OP_GET_PRODUCT_CATEGORIES` | 无 | `ArrayList<String>` 分类列表 |
| 101 | `OP_GET_PRODUCTS` | 分类名 `String` | `ArrayList<Product>` |
| 102 | `OP_GET_USERS` | 无 | `HashMap<String,User>` |
| 103 | `OP_ADD_USERS` | `User` 对象 | 无（服务端追加写入 `user.db`） |

## 现有功能

- [x] 用户登录：账号密码校验，数据来自 `user.db`
- [x] 用户注册：客户端先查重，服务端追加写入 `user.db`
- [x] 商品分类：从 `product.db` 读取分类并排序，填入下拉框
- [x] 商品列表：按分类查询，按商品名称排序
- [x] 商品详情：展示名称、CAS 号、分子式、数量、类别和结构图
- [x] 购物车：加入商品（按名称去重）、查看、清空
- [x] 提交购物车：检查数量输入框非空并弹出提示框
- [x] 主界面：Tab、菜单、Look & Feel 切换、关于、退出
- [x] 服务端：监听 5150，每个客户端连接一个 `Handler` 线程
- [x] 通信：操作码 + Java 对象序列化

## 待完善功能

- [ ] **订单与结算**：提交购物车只弹提示，没有生成订单、计算总价、扣减库存
- [ ] **商品管理**：不支持新增/修改/删除商品，`product.db` 只读
- [ ] **用户与权限**：`User.authority` 未参与权限判断；密码明文存储
- [ ] **购物车隔离**：`ShoppingCart` 使用 `static` 列表，所有窗口/用户共享，关闭客户端即丢失
- [ ] **注册校验**：两次密码一致性判断比较的是同一个字段；空密码用 `==` 比较字符串
- [ ] **菜单功能**：“打开”“保存”菜单没有绑定事件
- [ ] **价格**：`Product.price` 已解析，但详情、购物车和结算都未使用
- [ ] **网络健壮性**：无断线重连、服务端无优雅关闭、异常提示不完整
- [ ] **编码统一**：源码和 `product.db` 为 GBK，跨平台前建议统一为 UTF-8
- [ ] **工程化**：无 Maven/Gradle、无单元测试、无日志框架

## 已知问题

- 注册窗口的“重复密码”校验实际比较的是同一个密码字段，等于没有校验。
- 提交购物车提示“您购物信息已经保存，谢谢”，但实际没有保存任何数据。
- `bin/` 和 `.idea/` 不提交到仓库，克隆后需要按“启动方式”重新编译并配置 IDE。

## 说明

- 本项目为 Java 课程设计，仅用于学习交流。
- 仓库提交源码、数据文件和 Eclipse 元数据；编译产物和 IDE 私有配置由 `.gitignore` 排除。
