# 商家管家 MerchantFlow

面向中小商家的多端订单与库存管理系统。整体方案、数据库设计、接口/页面清单与开发里程碑见 [docs/PLAN.md](docs/PLAN.md)；**部署步骤见 [DEPLOY.md](DEPLOY.md)**；**常用命令与本地/Docker 切换见 [docs/COMMANDS.md](docs/COMMANDS.md)**。当前进度：M1–M8 与阶段 2（AI 运营助手）全部完成。

## 本地启动

1. 复制 `.env.example` 为 `.env`，按需调整密码和 JWT 密钥。
2. 前端：`cd frontend && npm install && npm run dev`
3. 后端：在安装 Java 21 和 Maven 3.9+ 后运行 `cd backend && mvn spring-boot:run`。
   - 若本机未安装 JDK/Maven，可直接使用仓库内便携工具链：`D:\agent\VibeCoding\.tools\jdk\jdk-21.0.12+8` 与 `D:\agent\VibeCoding\.tools\maven\apache-maven-3.9.11`（设置 `JAVA_HOME` 与 `PATH` 后即可用 `mvn`）。
4. 全栈容器：`docker compose up --build`。

访问前端 `http://localhost:5173`，后端健康检查 `http://localhost:8080/api/v1/health`，接口文档 `http://localhost:8080/swagger-ui.html`。

## 设计决策

- 采用前后端分离单仓库；前端同一套 Vue 页面通过布局与断点适配 PC、平板和手机。
- 数据库演进和演示数据均使用 Flyway 版本化迁移，禁止应用启动时随机造数。
- 第一阶段只提供公开健康检查；JWT 认证与权限控制在阶段 2 实现。
- 退款完成不自动回补库存，后续必须由明确的退货入库流程产生可追溯库存流水。
- 销售额口径：以 `sales_order.paid_at`（付款时间，V6 迁移新增）统计已支付订单金额，排除已取消与已退款完成订单；销售趋势按天聚合并补零，热销排行按近 30 天已支付订单销量。
- 工作台图表采用零依赖 SVG 实现（未引入 ECharts），避免增加前端依赖；如需更复杂图表可后续替换。
- 前端路由按角色拦截（`meta.roles`），按钮级权限由 `hasRole()` 控制；服务端 `@PreAuthorize` 仍是最终防线。
- 订单列表接口升级为分页查询（订单号/客户姓名/手机号关键字 + 状态 + 创建时间范围）。
- 扫码入口设计：输入框回车即查询（与扫码枪行为一致），接口为 `GET /api/v1/orders/by-no/{orderNo}`；未来接摄像头只替换输入源。
- 移动端为仓库模式：`/m/*` 四 Tab（待处理/订单/库存/我的），与 PC 管理后台同一套代码；手机端底部导航自动切换为仓库 Tab，桌面侧边栏提供「仓库模式」入口，无需维护两套应用。
- 商品不做物理删除（仅支持停用）：订单、库存、盘点均引用 SKU，物理删除会破坏流水追溯；盘点单创建时固化 SKU 清单，为历史快照，不受后续商品变更影响。
- AI 助手默认使用内置规则引擎 Provider（零外部依赖、可演示），应答基于只读工具结果组装；接入真实大模型只需新增 `AssistantProvider` 实现并通过 `merchantflow.assistant.provider` 切换，接口与工具集不变。助手只有只读查询权限，库存调整建议必须由用户二次确认后走标准调整接口（记录操作人）。

## 阶段 2：AI 运营助手

- `POST /api/v1/assistant/chat`（管理员/运营）：自然语言 → 意图路由（补货/热销/经营/待发货/异常）→ 执行只读工具（复用库存预警、热销排行、经营汇总、待发货、异常订单查询）→ 规则引擎组装中文回复。
- 补货意图返回**建议卡片**（含建议入库量），前端二次确认后调用既有 `POST /api/v1/inventory/adjustments` 执行，操作人与原因写入库存流水——助手本身绝不写库。
- 前端 `/assistant` 对话页：气泡聊天 + 快捷问题 + 建议卡片「一键补货」。
- 验证脚本：`scripts/verify-assistant.ps1`（登录与 403 权限、五类意图问答、一键补货入库与流水断言）。

## 演示账号

四类角色均使用密码 `MerchantFlow@2026`：`admin`、`operator`、`warehouse`、`viewer`。

## 当前接口

- `POST /api/v1/auth/login`：账号密码换取 8 小时 JWT。
- `GET /api/v1/auth/me`：以 Bearer Token 获取当前用户与角色。
- `/api/v1/admin/**`：预留给管理员接口，服务端强制要求 `ADMIN` 角色。

## 阶段 3：商品与库存接口

- `GET/POST /api/v1/categories`：查看或维护商品分类。
- `GET/POST /api/v1/products`：查看 SKU 库存概览或新建商品与初始库存。
- `GET /api/v1/inventory`、`/alerts`、`/transactions`：库存快照、预警与最近 100 条流水。
- `POST /api/v1/inventory/adjustments`：入库、出库和调整；仅管理员与仓库员可写入。

### 库存并发设计

库存变动采用带条件的原子更新：只有在 `available_qty + delta >= 0` 时才更新库存并递增版本号。若受影响行数不是 1，事务会失败并返回冲突错误；成功变更后，在同一事务内写入库存流水。因此并发请求不能将可用库存扣减为负数。

## 阶段 4：订单接口

- `GET/POST /api/v1/orders`：筛选订单或创建待付款订单；创建时锁定库存。
- `POST /api/v1/orders/{id}/pay`：模拟确认付款，消耗锁定库存并写出库流水。
- `POST /api/v1/orders/{id}/ship`、`/complete`、`/cancel`、`/refund`、`/refund/complete`：受服务端状态机和角色共同控制的生命周期操作。

## 阶段 5：工作台与报表

- `GET /api/v1/dashboard/summary`：今日订单数、今日销售额、待发货数、库存预警数。
- `GET /api/v1/dashboard/sales-trend?days=14`、`/top-products?days=30`、`/anomalies`：趋势、热销、异常提醒。
- `GET /api/v1/orders`：升级为分页 + 关键字（订单号/客户姓名/手机号）+ 状态 + 时间范围筛选。
- `GET /api/v1/orders/{id}/logs`、`GET /api/v1/orders/by-no/{orderNo}`：操作日志与扫码/手输查单。
- 前端：工作台仪表盘（统计卡 + 零依赖 SVG 趋势图 + 热销排行 + 异常提醒）、订单筛选/分页/详情抽屉（含操作日志时间线）、路由与按钮级角色权限。

### M5 验证方式

1. 启动 MySQL 与后端、前端（见上文）。
2. 运行端到端脚本：`powershell -ExecutionPolicy Bypass -File .\scripts\verify-m5.ps1`（覆盖四角色登录、仪表盘、订单检索、建单→付款→发货→详情→日志→by-no、权限 403 断言）。
3. 浏览器手测：`admin/operator/warehouse/viewer`（密码 `MerchantFlow@2026`）四账号登录，核对菜单差异（viewer 无商品/库存菜单）、工作台数据、订单筛选与详情、异常订单跳转。

## 阶段 6：移动端仓库模式

- 布局：`/m` 仓库模式外壳（`WarehouseShell`），底部四 Tab —— `/m` 待处理、`/m/orders` 订单、`/m/inventory` 库存、`/m/me` 我的；手机端（<760px）底部导航自动切换为仓库 Tab，桌面侧边栏新增「仓库模式」入口。
- 待处理：今日待发货卡片、扫码/手输订单号查询（回车即查，模拟扫码枪）、一键发货大按钮。
- 订单：状态筛选 chips、卡片列表、点按详情（明细 + 操作记录 + 按角色的操作按钮）、加载更多。
- 库存：低库存预警卡片、大表单入库/出库/调整（原因必填，写入流水）。
- 我的：用户卡片与角色徽章、使用说明、退出登录。
- 权限：库存 Tab 与发货/调整按钮按角色显示；服务端仍为最终防线。

### M6 验证方式

浏览器 F12 切换到手机模式（或把窗口拉窄到 <760px）：
1. `warehouse` 登录 → 底部四 Tab → 待处理页输入订单号回车 → 出现订单 → 「立即发货」；
2. 订单 Tab 状态筛选与详情弹层、库存 Tab 调整、我的 Tab 退出；
3. `viewer` 登录 → 无「库存」Tab，待处理无发货按钮；

## 阶段 7：管理后台

- **员工管理**（仅管理员）：`GET/POST /api/v1/users`、`PUT /users/{id}/status|roles`；停用账号无法登录，禁止停用自己。
- **客户管理**（管理员/运营）：`GET /api/v1/customers`（姓名/手机号搜索 + 分页 + 订单数）、`GET /customers/{id}`（最近 20 笔订单）。
- **库存盘点**（管理员/仓库员）：V7 迁移新增 `stocktake`/`stocktake_item`；`POST /api/v1/stocktakes` 按当前库存生成草稿 → `PUT .../items/{skuId}` 录入实盘 → `POST .../{id}/complete` 时差异统一生成 ADJUSTMENT 库存流水（原子更新防负数）→ 或取消作废。
- **商品完善**：`PUT /api/v1/products/{skuId}` 编辑名称/售价/成本/上下架；商品页开放「新建商品」（建档即入初始库存）。

### M7 验证方式

1. `admin` 登录 → 「员工管理」新建员工（如 `xiaoli`）→ 修改角色 → 用新账号登录验证权限；尝试停用自己应被拒绝。
2. `warehouse` 登录 → 「库存盘点」发起盘点 → 修改某 SKU 实盘数（造出差异）→ 完成盘点 → 「库存中心」流水出现 ADJUSTMENT 记录、库存相应增减。
3. `admin` 登录 → 「客户管理」搜索/查看客户最近订单；「商品」页新建商品、编辑价格与上下架。
4. 桌面端点侧边栏「仓库模式」进入同一套移动布局。

## 阶段 8：PWA 与部署收尾

- **PWA**：图标（192/512/maskable/apple-touch）与 manifest 已就位；`npm run build` 生成 service worker；浏览器地址栏出现安装按钮即可安装到桌面。
- **构建优化**：vite 手动分包（vue 全家桶 / element-plus 拆为独立 chunk），消除单 chunk 过大告警。
- **404 页面**：未知路由统一兜底。
- **演示数据**：V8 迁移新增 3 个分类、3 个商品、4 个客户与 10 笔近 30 天历史订单（金额/库存/流水一致），工作台趋势、热销排行、待发货开箱即有数据。
- **验证脚本**：`scripts/verify-m5.ps1`（16 项）、`scripts/verify-m7.ps1`（18 项），均为 UTF-8 安全（PS 5.1 兼容）。

### 部署（Docker Compose 全栈）

`docker compose up --build -d` 一键启动 MySQL + 后端 + 前端（Nginx 托管 + 反代 `/api`），已在本地实测通过（登录、工作台含 V8 演示数据、订单、仓库模式均正常）。注意：

- 容器内 MySQL 已统一 `Asia/Shanghai` 时区（`TZ` + `--default-time-zone`），保证「今日订单」等按天统计与业务一致；
- 首次启动后端约需 15 秒（Flyway 建表 + 灌演示数据），期间访问返回 502 属正常；
- 回到本地开发前先 `docker compose down`（释放 3306/8080/5173），再启动便携 MySQL 与 `dev-backend.ps1`；`down -v` 会清空容器数据库。

若本机 Docker Desktop 的 WSL2 引擎报 `0x800705aa`（资源不足/虚拟化组件未启用），需在管理员 PowerShell 执行后重启：

```powershell
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
# 重启后
wsl --update
wsl --set-default-version 2
```

不依赖 Docker 的本地开发仍可用：`scripts/setup-mysql-portable.ps1`（便携 MySQL）+ `scripts/dev-backend.ps1`（便携 JDK/Maven）。
