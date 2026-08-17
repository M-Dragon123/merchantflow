# 商家管家 MerchantFlow — 总体方案与开发里程碑

> 版本：2026-08 阶段规划 · 配套 README：根目录 `README.md`
> 工作方式：不一次性生成全部代码；按里程碑推进，每个里程碑完成后说明改动文件、启动方式、验证方式与下一步。

---

## 1. 现状盘点（基于仓库实际代码核实）

| 里程碑 | 内容 | 状态 | 证据 |
|---|---|---|---|
| M1 工程骨架 | Spring Boot 3.5.5 + Java 21、Vue 3 + TS + Vite 7、Docker Compose、Nginx | ✅ 已实现 | `backend/pom.xml`、`frontend/vite.config.ts`、`docker-compose.yml`、`deploy/nginx/default.conf` |
| M2 认证与权限 | JWT 登录/me、Security 过滤器、角色控制、BCrypt 演示账号 | ✅ 已实现 | `security/`、`config/SecurityConfig.java`、`auth/AuthController.java`、V3 迁移 |
| M3 商品与库存 | 分类/SPU/SKU 增查、库存快照/预警/流水/调整、防超卖原子扣减 | ✅ 已实现 | `product/`、`inventory/`、V1/V4 迁移 |
| M4 订单状态机 | 建单锁库存、付款扣库存、发货/完成/取消/退款、操作日志 | ✅ 已实现 | `order/`、V5 迁移 |
| M5 经营数据与工作台 | 仪表盘统计、趋势图、热销排行、异常提醒；订单筛选分页与详情 | ✅ 已实现 | `dashboard/`、V6 迁移、`PageResult`、`WelcomeView` 仪表盘、`OrderView` 筛选/分页/详情、前端角色权限 |
| M6 移动端仓库模式 | 待处理/订单/库存/我的四 Tab、扫码入口、大按钮 | ✅ 已实现 | `WarehouseShell` 底部四 Tab、`warehouse/` 四页、`ScanInput`、AppShell 移动端导航切换、路由 `/m/*` |
| M7 管理后台 | 员工/角色管理、客户管理、库存盘点、商品编辑完善 | ✅ 已实现 | `stocktake/` 模块 + V7 迁移、`UserController`、`CustomerController`、商品 PUT、`UsersView`/`CustomersView`/`StocktakeView`、商品页新建/编辑 |
| M8 PWA 与部署收尾 | PWA 图标与安装验证、空/错/加载态巡检、Docker 全链路验证 | ✅ 已实现（Docker 全链路需本机修复 WSL 后执行） | PWA 图标 + manifest、vite 分包、404 页、V8 演示数据、`verify-m5/m7` 脚本、部署说明 |
| 阶段 2 AI 助手 | 受限工具查询 + 建议 + 二次确认 | ✅ 已实现 | `assistant/` 模块（IntentRouter + Provider 接口 + 规则引擎实现）、`AssistantView` 对话页、只读工具集 + 补货建议二次确认 |

**环境事实**：`node v24.19.0` ✅、`frontend/node_modules` 已安装 ✅、`npm run lint` ✅、`npm run build` ✅；本机未全局安装 JDK/Maven，但仓库内置便携工具链（`.tools/jdk` 21.0.12、`.tools/maven` 3.9.11）可直接编译并已跑通 `mvn test`（11 个用例全绿）；Docker 命令在当前会话被沙箱拦截，需授权后使用。

---

## 2. 技术栈与目标目录结构

技术栈：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + vite-plugin-pwa（前端）；Java 21 + Spring Boot 3 + Spring Security + Spring Data JPA + Flyway + springdoc（后端）；MySQL 8；Nginx 静态托管前端并反代 `/api`。

```
merchantflow/
├── README.md                  # 启动说明、演示账号、设计决策
├── .env.example
├── docker-compose.yml
├── docs/PLAN.md               # ← 本方案
├── backend/
│   ├── pom.xml  Dockerfile
│   └── src/
│       ├── main/java/com/merchantflow/
│       │   ├── MerchantFlowApplication.java
│       │   ├── common/        ApiResponse、GlobalExceptionHandler、PageResult（规划）
│       │   ├── config/        SecurityConfig、OpenApiConfig（规划）
│       │   ├── security/      JwtService、JwtAuthenticationFilter
│       │   ├── auth/          AuthController
│       │   ├── user/          UserAccount/Role + 员工管理（规划）
│       │   ├── product/       Category/Spu/Sku 控制器与服务
│       │   ├── inventory/     Inventory/Transaction + 盘点（规划）
│       │   ├── order/         SalesOrder/Item/操作日志/状态机
│       │   ├── customer/      （规划）客户管理
│       │   ├── dashboard/     （规划）统计聚合
│       │   └── assistant/     （阶段 2）AI 助手
│       ├── main/resources/
│       │   ├── application.yml / application-docker.yml
│       │   └── db/migration/  V1..V8 现有迁移 + 阶段 2（无新增表）
│       └── test/java/...
├── frontend/
│   ├── package.json  vite.config.ts  eslint.config.ts  .prettierrc.json
│   ├── public/                # PWA 图标（规划）
│   └── src/
│       ├── api/               client.ts + 分模块 api（规划）
│       ├── stores/            auth.ts + app.ts（仓库模式切换，规划）
│       ├── router/            index.ts（meta.roles 前端权限，规划）
│       ├── layouts/           AppShell.vue（PC 侧边栏）+ WarehouseShell.vue（移动四 Tab，规划）
│       ├── views/             现有 5 页 + Dashboard/订单详情/客户/员工/盘点/AI（规划）
│       ├── components/        图表、扫码输入框、通用空状态（规划）
│       └── styles/index.css
└── deploy/nginx/default.conf
```

---

## 3. 数据库设计

### 3.1 ER 图（Mermaid）

```mermaid
erDiagram
    sys_role ||--o{ sys_user_role : "拥有"
    sys_user ||--o{ sys_user_role : "分配"
    product_category ||--o{ product_spu : "包含"
    product_spu ||--o{ product_sku : "包含"
    product_sku ||--o| inventory : "1对1库存"
    product_sku ||--o{ inventory_transaction : "产生流水"
    customer ||--o{ sales_order : "下单"
    sales_order ||--o{ sales_order_item : "包含明细"
    product_sku ||--o{ sales_order_item : "被购买"
    sales_order ||--o{ order_operation_log : "记录操作"
    product_sku ||--o{ stocktake_item : "盘点"        %% 规划
    stocktake ||--o{ stocktake_item : "包含明细"      %% 规划

    sys_user {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar name
        tinyint status
    }
    sys_role {
        bigint id PK
        varchar code UK
        varchar name
    }
    sys_user_role {
        bigint user_id PK,FK
        bigint role_id PK,FK
    }
    product_category {
        bigint id PK
        bigint parent_id
        varchar name
        int sort
    }
    product_spu {
        bigint id PK
        bigint category_id FK
        varchar name
        varchar status
    }
    product_sku {
        bigint id PK
        bigint spu_id FK
        varchar sku_code UK
        json specs_json
        decimal sale_price
        decimal cost_price
        varchar status
    }
    inventory {
        bigint sku_id PK,FK
        int available_qty
        int locked_qty
        int safety_stock
        bigint version
    }
    inventory_transaction {
        bigint id PK
        bigint sku_id FK
        varchar transaction_type
        int quantity_delta
        int before_qty
        int after_qty
        varchar reason
        varchar operator_name
    }
    customer {
        bigint id PK
        varchar name
        varchar mobile UK
    }
    sales_order {
        bigint id PK
        varchar order_no UK
        bigint customer_id FK
        varchar status
        decimal total_amount
        datetime paid_at          %% 规划：V6 迁移新增
        bigint version
    }
    sales_order_item {
        bigint id PK
        bigint order_id FK
        bigint sku_id FK
        varchar sku_name_snapshot
        decimal unit_price
        int quantity
        decimal subtotal_amount
    }
    order_operation_log {
        bigint id PK
        bigint order_id FK
        varchar action
        varchar from_status
        varchar to_status
        varchar remark
        varchar operator_name
    }
    stocktake {                   %% 规划：V6 新增
        bigint id PK
        varchar stocktake_no UK
        varchar status
        varchar operator_name
        varchar remark
    }
    stocktake_item {              %% 规划：V6 新增
        bigint id PK
        bigint stocktake_id FK
        bigint sku_id FK
        int system_qty
        int counted_qty
        int diff_qty
    }
```

### 3.2 主要表设计（现状 + 规划）

| 表 | 用途 | 关键字段 | 备注 |
|---|---|---|---|
| sys_user / sys_role / sys_user_role | 账号、角色、绑定 | password_hash(BCrypt)、status | 已建；员工管理直接用 |
| product_category / product_spu / product_sku | 分类、SPU、SKU | sku_code UK、specs_json、sale_price、cost_price、status | 已建；多规格 SPU→SKU |
| inventory | 库存 | available_qty、locked_qty、safety_stock、version | 已建；CHECK 约束非负；version 防并发 |
| inventory_transaction | 库存流水 | transaction_type(INBOUND/OUTBOUND/ADJUSTMENT)、quantity_delta、before/after_qty、reason、operator_name | 已建；全量可追溯 |
| customer | 客户 | name、mobile UK | 已建；运营维护 |
| sales_order / sales_order_item | 订单与明细 | order_no UK、status、total_amount、version、paid_at(规划) | 已建；明细含价格快照 |
| order_operation_log | 订单操作日志 | action、from/to_status、remark、operator_name | 已建 |
| stocktake / stocktake_item | 盘点单与明细 | stocktake_no、system_qty、counted_qty、diff_qty | 已建（V7 迁移）；完成盘点差异自动生成 ADJUSTMENT 流水 |

**现有迁移**：`V1__initial_schema.sql`（用户/角色/分类/SPU/SKU/库存/客户）、`V2`/`V3`（演示数据与角色账号）、`V4__inventory_transactions.sql`、`V5__orders.sql`、`V6__paid_at.sql`（`sales_order.paid_at`，销售额按付款时间口径，见设计决策 7.4）、`V7__stocktake.sql`（盘点单与明细）、`V8__demo_catalog.sql`（演示商品/客户/近 30 天历史订单）。

---

## 4. 接口清单（现状 + 规划，含角色矩阵）

角色：A=管理员 ADMIN，O=运营 OPERATOR，W=仓库员 WAREHOUSE，V=只读 VIEWER，公开=无需登录。

### 认证与系统
| 方法/路径 | 说明 | 权限 | 状态 |
|---|---|---|---|
| POST /api/v1/auth/login | 登录换取 JWT（8 小时） | 公开 | ✅ |
| GET /api/v1/auth/me | 当前用户与角色 | 登录 | ✅ |
| GET /api/v1/health | 健康检查 | 公开 | ✅ |

### 商品
| 方法/路径 | 说明 | 权限 | 状态 |
|---|---|---|---|
| GET/POST /api/v1/categories | 分类列表/新增 | 读 A/O/W；写 A/O | ✅ |
| GET/POST /api/v1/products | SKU 列表（含库存）/新建商品+初始库存 | 读 A/O/W；写 A/O | ✅ |
| PUT /api/v1/products/{skuId} | 编辑售价/成本/上下架 | A/O | 规划 |

### 库存
| 方法/路径 | 说明 | 权限 | 状态 |
|---|---|---|---|
| GET /api/v1/inventory | 库存快照 | A/O/W | ✅ |
| GET /api/v1/inventory/alerts | 低库存预警（available≤safety） | A/O/W | ✅ |
| GET /api/v1/inventory/transactions | 库存流水（近 100 条） | A/O/W | ✅ |
| POST /api/v1/inventory/adjustments | 入库/出库/调整（原因必填） | A/W | ✅ |
| POST /api/v1/stocktakes | 创建盘点单 | W/A | 规划 |
| POST /api/v1/stocktakes/{id}/complete | 完成盘点（差异自动生成 ADJUSTMENT 流水） | W/A | 规划 |
| GET /api/v1/stocktakes | 盘点单列表 | W/A | 规划 |

### 订单
| 方法/路径 | 说明 | 权限 | 状态 |
|---|---|---|---|
| GET /api/v1/orders | 列表（订单号/客户关键字 + 状态 + 时间范围 + 分页） | 全部角色 | ✅（M5 完成改造） |
| GET /api/v1/orders/{id} | 订单详情（含明细、客户名、付款时间） | 全部角色 | ✅ |
| GET /api/v1/orders/{id}/logs | 操作日志 | 全部角色 | ✅（M5 实现） |
| GET /api/v1/orders/by-no/{orderNo} | 按订单号查询（扫码枪/手动输入入口） | 全部角色 | ✅（M5 实现） |
| POST /api/v1/orders | 创建订单（锁库存） | A/O | ✅ |
| POST /api/v1/orders/{id}/pay | 确认付款（扣库存） | A/O | ✅ |
| POST /api/v1/orders/{id}/ship | 发货 | A/W | ✅ |
| POST /api/v1/orders/{id}/complete | 完成 | A/O | ✅ |
| POST /api/v1/orders/{id}/cancel | 取消（释放库存） | A/O | ✅ |
| POST /api/v1/orders/{id}/refund | 发起退款 | A/O | ✅ |
| POST /api/v1/orders/{id}/refund/complete | 退款完成 | A | ✅ |

### 经营数据（✅ M5 实现，全部角色可读）
| 方法/路径 | 说明 |
|---|---|
| GET /api/v1/dashboard/summary | 今日订单数、今日销售额、待发货数、库存预警数 |
| GET /api/v1/dashboard/sales-trend?days=14 | 近 N 天销售趋势（按付款日聚合，补零） |
| GET /api/v1/dashboard/top-products?days=30 | 热销商品 Top10（已支付订单销量） |
| GET /api/v1/dashboard/anomalies | 异常订单提醒（待付款超 24h、退款中） |

### 客户 / 员工（✅ M7 实现）
| 方法/路径 | 说明 | 权限 |
|---|---|---|
| GET /api/v1/customers?keyword=&page= | 客户列表（分页搜索） | A/O |
| GET /api/v1/customers/{id} | 客户详情与订单 | A/O |
| GET/POST /api/v1/users | 员工列表/新建（含角色） | A |
| PUT /api/v1/users/{id}/status | 启用/停用员工 | A |
| PUT /api/v1/users/{id}/roles | 调整员工角色 | A |

### AI 运营助手（✅ 阶段 2 实现）
| 方法/路径 | 说明 | 权限 |
|---|---|---|
| POST /api/v1/assistant/chat | 自然语言问答，仅只读工具集，返回建议；库存调整必须走 `/inventory/adjustments` 并二次确认 | A/O |

---

## 5. 页面清单

### PC 管理后台（AppShell：左侧深蓝侧边栏）
| 页面 | 路由 | 权限 | 状态 |
|---|---|---|---|
| 登录 | /login | 公开 | ✅ |
| 工作台 | / | 全部 | ✅ |
| 商品管理（SPU/SKU/分类） | /products | A/O/W(读) | ✅ |
| 库存中心（快照/预警/流水/盘点） | /inventory | A/O/W | ✅ |
| 订单管理（列表/筛选/创建/详情） | /orders | 全部（写按角色） | ✅ |
| 客户管理 | /customers | A/O | ✅ |
| 员工管理 | /users | A | ✅ |
| AI 运营助手 | /assistant | A/O | ✅ 阶段 2 |
| 403 无权限 / 404 | /forbidden | — | ✅ |

### 移动端仓库模式（WarehouseShell：底部四 Tab，断点 <760px 自动启用；PC 可手动切换）
| Tab | 内容 | 权限 |
|---|---|---|
| 待处理 | 今日待发货数量 + 列表 + 一键发货 | 全部可见，发货按钮 A/W |
| 订单 | 扫码/手动输入订单号查询 → 详情 → 发货/出库；订单列表 | 同上 |
| 库存 | 低库存预警列表、调整入口、最近流水 | A/O/W |
| 我的 | 当前用户/角色、退出、仓库模式说明 | 全部 |

平板（760–1024px）：沿用 PC 布局，侧边栏收缩为图标栏。

---

## 6. 关键业务规则

1. **库存并发防超卖（已实现）**：库存变动走带条件原子 SQL —— `available_qty + delta >= 0` 才更新并递增 version；受影响行数不为 1 即抛冲突。创建订单 `reserve`（available→locked）、付款 `consumeReservation`（扣减并写出库流水）、取消 `release`，均在事务内完成，流水与库存同事务写入。
2. **订单状态机（已实现）**：`PENDING_PAYMENT → PENDING_SHIPMENT(支付) → SHIPPED → COMPLETED`；`PENDING_PAYMENT → CANCELLED`；`SHIPPED → REFUNDING → REFUNDED`；非法迁移抛错；每次迁移写 `order_operation_log`。
3. **退款不回补库存（已决策）**：退款完成只改订单状态，库存回补必须走明确的入库/盘点流程，保证流水可追溯。
4. **权限矩阵**：服务端 `@PreAuthorize` 强制；前端路由 `meta.roles` 拦截 + 按钮级 `hasRole()` 控制，未授权统一跳 403。
5. **库存调整规则**：reason 必填；出库不得将可用库存扣成负数；调整记录 before/after 与操作人。
6. **销售额口径（规划）**：以 `paid_at` 为准统计已支付订单（排除已退款），趋势按日聚合。
7. **异常订单（规划）**：待付款超过 24 小时 + 退款中订单，工作台提醒。
8. **盘点（已实现）**：仓库员创建盘点单，记录账面数与实点数，完成时差异自动生成 ADJUSTMENT 流水并记录操作人。
9. **AI 助手（阶段 2）**：只读工具集（库存预警、销量、销售排行），不提供写库工具；任何库存调整建议必须由用户二次确认，走现有 `/inventory/adjustments` 并落操作日志。
10. **扫码设计（规划）**：输入框 Enter 提交即触发查询（与扫码枪行为一致），接口 `GET /api/v1/orders/by-no/{orderNo}`；未来接摄像头扫码仅替换输入源，接口不变。

---

## 7. 设计决策（README「设计决策」将同步这些条目）

7.1 前后端分离单仓库；同一套 Vue 页面经布局与断点适配 PC/平板/手机，移动端底部导航切换为仓库模式四 Tab。
7.2 数据库演进与演示数据全部用 Flyway 版本化迁移，禁止应用启动随机造数；演示账号 4 个角色（密码见 README）。
7.3 库存并发用「条件更新 + version」乐观方式，配合数据库 CHECK 约束双保险。
7.4 销售额/趋势需要付款时间，故 V6 迁移新增 `sales_order.paid_at`（付款时写入）；不新增表统计，报表全部为聚合查询。
7.5 盘点采用「盘点单 + 明细 + 差异 → 自动生成流水」的真实仓库流程，而非直接改库存。
7.6 移动端不做独立仓库版应用，同一应用内切换，减少维护成本；扫码以输入框回车模拟，接口与真实扫码器兼容。
7.7 员工管理基于现有 `sys_user` 体系扩展，停用即不可登录，不做复杂 RBAC 界面（第一阶段角色固定 4 类）。
7.8 AI 助手默认不启用外部大模型 Key，先以可插拔 Provider 接口 + 内置规则引擎实现演示效果，接入真实 LLM 只改 Provider。
7.9 **统一异常 JSON（阶段 2 补齐）**：所有 MVC 层异常（404/405/参数校验失败/JSON 解析失败/兜底 500）都在 `GlobalExceptionHandler` 转成统一 `ApiResponse` JSON；`@PreAuthorize` 的 `AccessDeniedException` 放行给 Spring Security 转 403。避免异常转发到 `/error` 后被安全链拦截成裸 401（前端会误判为登录过期并踢回登录页）。

---

## 8. 开发里程碑与验证方式

| 里程碑 | 内容 | 验证方式 |
|---|---|---|
| M1 骨架 ✅ | 三端容器、健康检查、Flyway | `docker compose up` 后 `GET /api/v1/health` |
| M2 认证 ✅ | JWT + 角色 + 演示账号 | 四账号登录；越权访问返回 403 |
| M3 商品库存 ✅ | 分类/SPU/SKU、原子扣减、流水 | 单元测试 + 接口并发扣减验证 |
| M4 订单 ✅ | 状态机 + 锁/扣/放库存 + 日志 | 单元测试 `OrderStatusTests` + 接口走完整生命周期 |
| **M5 经营数据** | 后端 dashboard 统计接口；`GET /orders` 加分页筛选；订单详情页与操作日志；前端路由/按钮权限；工作台页（统计卡 + 销售趋势图 + 热销排行 + 异常提醒） | `mvn test`；`npm run build`；四角色登录核对菜单与按钮 |
| **M6 移动端仓库模式** | WarehouseShell 四 Tab、待处理/发货大按钮、扫码输入框、库存预警 | 手机宽度断点（DevTools 模拟 + 真机）走通「扫单→发货→出库」 |
| **M7 管理后台** | 员工/角色管理、客户管理、库存盘点、商品编辑完善 | 管理员建员工→分配角色→新账号登录验证 |
| **M8 收尾** | PWA 图标/安装验证、空态/错误/加载态巡检、Docker 全链路冒烟、README 设计决策补全、测试补齐 | `docker compose up --build` 全流程演示脚本跑通；Lighthouse PWA 基础检查 |
| 阶段 2 ✅ | AI 运营助手（Provider 可插拔 + 只读工具 + 二次确认） | `verify-assistant.ps1` 13 项（意图问答、建议→入库→流水、403、400/404 异常回归） |

**每个里程碑完成后的交付说明**：改了哪些文件 / 如何本地启动 / 如何验证 / 下一步建议。

---

## 9. 环境准备与本地启动

现状：Node 24 可用（前端就绪）；本机未全局安装 JDK/Maven，但仓库内置便携工具链 `.tools/jdk`（21.0.12）与 `.tools/maven`（3.9.11）已跑通 `mvn test`；Docker 本会话需授权。

启动方式二选一（二选一即可，M5 起后端必须可运行）：

- **方案 A 本地开发**：安装 JDK 21 + Maven 3.9+，`cd backend && mvn spring-boot:run`（需要本地 MySQL 或 Docker 起 MySQL）；前端 `cd frontend && npm run dev`。
- **方案 B 全容器**：`docker compose up --build`，前端 5173 / 后端 8080 / Swagger `http://localhost:8080/swagger-ui.html`。

演示账号（密码统一 `MerchantFlow@2026`）：`admin`（管理员）、`operator`（运营）、`warehouse`（仓库员）、`viewer`（只读成员）。
