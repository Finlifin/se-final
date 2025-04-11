# FlixBackend

FlixBackend 是一个基于 Elixir/Phoenix 框架构建的电商后端，采用模块化设计，支持 REST API 和实时通信，主要架构特点如下：

- **技术栈**  
  - Elixir 语言，Phoenix 框架  
  - Ecto 作为 ORM，连接 PostgreSQL 数据库  
  - Guardian 进行 JWT 用户认证  
  - Phoenix Channels 支持 WebSocket 实时通信  

- **目录结构**  
  - flix_backend：核心业务逻辑  
    - `controllers`：REST API 控制器，如订单、支付、消息、用户资料、公告等  
    - messaging.ex：封装消息通知逻辑，支持广播  
    - api_response.ex：统一 API 响应格式  
  - config：环境配置  
  - static：静态资源，含前端 JS、CSS  
  - test：测试代码  
  - docs：API 文档和业务流程说明  

- **主要业务模块**  
  - **用户认证**：通过 Guardian 实现，支持短信验证码和密码登录  
  - **订单系统**：支持订单创建、查询、状态更新、取消，详见 `FlixBackendWeb.OrderController`  
  - **支付系统**：支持多种支付方式、配送方式、费用计算，详见 `FlixBackendWeb.PaymentController`  
  - **消息系统**：用户间私信、系统通知，支持未读统计、同步，详见 `FlixBackendWeb.MessageController` 和 `FlixBackend.Messaging`  
  - **公告系统**：系统公告管理，详见 `FlixBackendWeb.AnnouncementController`  
  - **用户资料**：用户信息、商家信息、余额、收藏、历史等，详见 `FlixBackendWeb.ProfileController`  
  - **学校校区管理**：支持多校区，分页查询，详见 `FlixBackendWeb.CampusController`  

- **API设计**  
  - RESTful 风格，路径清晰，支持分页、过滤  
  - 认证接口 `/api/v1/auth`  
  - 订单 `/api/v1/orders`  
  - 支付 `/api/v1/payment`  
  - 消息 `/api/v1/messages`  
  - 公告 `/api/v1/announcements`  
  - 用户资料 `/api/v1/profile`  
  - 学校校区 `/api/v1/schools` `/api/v1/campuses`  

- **实时通信**  
  - 使用 Phoenix Channels 实现用户消息推送  
  - 订单、消息等事件通过广播通知相关用户  

- **安全设计**  
  - 认证中间件保护敏感接口  
  - 权限校验确保用户只能访问自己的资源  
  - 统一错误响应，避免信息泄露  

整体上，FlixBackend 是一个典型的基于 Phoenix 的电商后端，结构清晰，功能丰富，支持 REST API 和实时通信，适合中大型电商平台使用。


FlixBackend 的架构采用典型的 Phoenix + Ecto 分层设计，具体范式如下：

### 1. JSON View 层
- 负责将控制器传递的业务数据转换为统一的 JSON 响应格式。
- 统一封装成功、失败、错误信息，避免泄露内部细节。
- 例如 `FlixBackendWeb.ErrorView`、`FlixBackendWeb.ChangesetView`。
- 视图层不包含业务逻辑，只负责格式化。

### 2. Controller 层
- 负责接收 HTTP 请求，参数校验，调用业务逻辑，返回 JSON。
- 处理权限认证、参数解析、分页、过滤。
- 组织调用 Service 层或直接操作 Repo。
- 统一异常处理，调用 View 层渲染响应。
- 例子：`FlixBackendWeb.OrderController` 负责订单的增删改查。

### 3. Service 层
- 封装复杂的业务逻辑和事务。
- 组合多表操作，保证数据一致性。
- 复用性强，供多个 Controller 调用。
- 例如 `CampusService.list_campuses/4` 实现分页查询。

### 4. Entity 层（Schema）
- 定义数据库表结构映射，使用 `Ecto.Schema`。
- 定义字段、类型、关联关系（belongs_to、has_many）。
- 定义变更集（changeset）进行数据校验。
- 例子：
  - `User` 用户
  - `Product` 商品
  - `Order` 订单
  - `Message` 消息

---

### Entity 层的 Migration 机制
- **迁移文件**位于 migrations，每个迁移对应一次数据库结构变更。
- 使用 `mix ecto.gen.migration` 生成，内容是 `Ecto.Migration` 脚本。
- 支持：
  - 创建/删除表
  - 添加/删除字段
  - 添加索引、唯一约束
  - 定义外键关联
  - 创建枚举类型（Postgres）
- 通过 `mix ecto.migrate` 执行迁移，自动管理版本。
- 设计上，FlixBackend 使用 UUID 作为主键，支持数组字段（如收藏、已购商品），并大量使用索引优化查询性能。
- 迁移中也定义了枚举类型（如消息类型、状态），保证数据一致性。

---

### 总结
- **View**：统一 JSON 格式
- **Controller**：请求入口，参数校验，权限，调用业务
- **Service**：复杂业务逻辑，事务
- **Entity**：数据模型，校验
- **Migration**：数据库结构演进，支持 UUID、索引、枚举

这种架构清晰分层，方便维护和扩展。


FlixBackend 的 Entity 层结合 Ecto Schema 和 Migration，设计了清晰的数据库结构，主要特点如下：

### 1. 主键设计
- **广泛使用 UUID (`:binary_id`) 作为主键**，保证分布式唯一性，避免自增冲突。
- 例如用户表 `users` 的主键是 `uid`，订单表 `orders` 的主键是 `order_id`。

### 2. 表结构设计
- **用户表 (`users`)**  
  - 主键 `uid`，类型 UUID  
  - 基本信息：手机号、用户名、头像、余额  
  - 数组字段：  
    - `published_product_ids`、`sold_product_ids`、`purchased_product_ids`、`favorite_product_ids`，类型 `{:array, :binary_id}`，方便存储关联ID集合  
  - 关联字段：`school_id`、`campus_id`，引用学校和校区

- **账户表 (`accounts`)**  
  - 主键 UUID  
  - 关联用户 `user_id`，引用 `users.uid`  
  - 手机号、密码哈希、角色

- **商品表 (`products`)**  
  - 主键 UUID  
  - 卖家ID `seller_id`，引用 `users.uid`  
  - 标题、描述、价格、图片数组、分类、状态（枚举）、浏览数、收藏数  
  - 支持标签数组 `tags` 和配送方式数组 `available_delivery_methods`

- **订单表 (`orders`)**  
  - 主键 UUID  
  - 买家ID、卖家ID，引用 `users.uid`  
  - 商品ID，引用 `products.id`  
  - 价格、时间戳、状态（枚举）  
  - 配送方式、地址、时间、费用、支付方式、支付时间

- **消息表 (`messages`)**  
  - 主键 UUID  
  - 发送者、接收者 UUID  
  - 内容为 JSON map，支持多类型内容  
  - 内容类型、消息类型、状态均为枚举  
  - 关联引用ID（如订单、商品）

- **收藏表 (`favorites`)**  
  - 主键 UUID  
  - 用户ID、商品ID，唯一组合索引避免重复收藏

- **学校/校区表 (`schools`、`campuses`)**  
  - 主键 UUID  
  - 学校有唯一代码，校区关联学校

### 3. 关联关系
- 使用 `belongs_to` 和 `has_many` 定义实体间关系  
- 通过外键约束保证数据一致性  
- 例如订单关联买家、卖家、商品；用户关联学校、校区

### 4. 数组字段
- 用户收藏、已购、已售商品ID存为数组，方便快速查询和更新  
- 商品标签、配送方式也用数组字段

### 5. 枚举类型
- 使用 PostgreSQL 的 ENUM 定义消息类型、内容类型、状态等  
- 通过自定义 Ecto.Type 实现类型转换，保证类型安全

### 6. Migration 机制
- 迁移文件中定义表结构、索引、唯一约束、外键  
- 支持 UUID 主键、数组字段、枚举类型  
- 通过 `mix ecto.migrate` 自动管理版本，方便演进

---

### 总结
FlixBackend 的 Entity 层设计合理，利用 UUID、数组字段、枚举和外键，构建了一个**高一致性、易扩展、性能优化**的数据库结构，满足电商平台复杂业务需求。

FlixBackend 中商品与订单的生命周期如下：

---

## 商品(Product)生命周期

**主要状态：**

- `available`（在售）
- `sold`（已售出）
- `deleted`（已删除）

**生命周期流程：**

1. **发布商品**  
   卖家通过接口发布商品，状态为 `available`，可被买家浏览。

2. **浏览商品**  
   买家通过商品列表或详情接口查看商品信息。

3. **下单购买**  
   买家对状态为 `available` 的商品发起订单。

4. **商品售出**  
   订单完成支付并确认收货后，商品状态更新为 `sold`，不可再次购买。

5. **删除商品**  
   卖家可将商品标记为 `deleted`，前端不再展示。

---

## 订单(Order)生命周期

**订单状态（枚举，详见 #file:README.md 和 API 文档）：**

- `pending`：订单已创建，未选择支付方式  
- `payment_pending`：已选择支付方式，待支付  
- `paid`：支付完成，等待卖家发货  
- `shipping`：卖家已发货，等待买家收货  
- `completed`：买家确认收货，交易完成  
- `cancelled`：订单取消  
- `refunded`：订单退款

**生命周期流程：**

1. **创建订单**  
   买家选择商品，创建订单，初始状态为 `pending`。

2. **选择支付和配送方式**  
   买家选择支付方式和配送方式，订单状态变为 `payment_pending`。

3. **支付完成**  
   买家完成支付，订单状态更新为 `paid`。

4. **卖家发货**  
   卖家确认发货，订单状态更新为 `shipping`。

5. **买家确认收货**  
   买家确认收货，订单状态更新为 `completed`，交易结束。

6. **取消订单**  
   买家或卖家在未发货或未支付前取消订单，状态变为 `cancelled`。

7. **退款**  
   卖家在已支付或已发货后发起退款，状态变为 `refunded`。

---

## 关系总结

- **商品状态**主要受订单完成情况影响，售出后变为 `sold`。
- **订单状态**体现了支付、发货、收货、取消、退款等业务流程。
- 订单与商品通过 `product_id` 关联，订单完成后商品状态同步更新。

---

## 用于绘制图示的建议

- **顺序图**  
  买家浏览商品 → 创建订单(pending) → 选择支付(payment_pending) → 支付完成(paid) → 卖家发货(shipping) → 买家确认(completed)  
  中途可取消(cancelled)或退款(refunded)

- **状态图**  
  订单状态转换图，突出 pending → payment_pending → paid → shipping → completed 的主流程  
  及任意节点的取消(cancelled)、退款(refunded)的分支

- **商品状态图**  
  available → sold  
  或 available → deleted

---

这符合 FlixBackend 的业务逻辑和数据库设计。