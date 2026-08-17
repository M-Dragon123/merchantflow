# 商家管家 MerchantFlow

> 面向中小商家的多端订单、库存与经营管理系统。覆盖“建品 - 接单付款 - 拣货发货 - 库存盘点 - 经营分析”业务闭环，并提供受限的 AI 运营辅助能力。

**Java 21 · Spring Boot 3.5 · MySQL 8 · Vue 3 · TypeScript · Docker Compose · PWA**

MerchantFlow 是一项独立完成的全栈作品：从需求拆解、ER 设计、权限模型和接口清单，到前后端实现、自动化回归与 Docker 部署均由同一项目闭环交付。项目采用 AI 编程工具辅助分阶段开发，但每个模块均经过人工审查与脚本验证。

## 一分钟了解项目

~~~
商品 / SKU 建档
    ↓
创建订单并锁定库存
    ↓
付款扣减库存 → 拣货发货 → 状态与操作日志留痕
    ↓
库存盘点 / 经营分析 / 异常提醒
    ↓
AI 运营助手提出补货建议，用户确认后走标准入库流程
~~~

项目内置 4 类演示角色、演示商品、客户和近 30 天历史订单。启动后即可体验工作台趋势、热销排行、待发货、库存预警和订单全流程。

## 真实页面截图

以下截图均来自仓库内置演示数据的本地运行环境；点击图片可查看原图。

### 工作台 · 经营概览

<a href="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/dashboard.png">
  <img src="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/dashboard.png" alt="MerchantFlow 工作台：销售趋势、热销排行与异常订单提醒" width="100%" />
</a>

<sub>销售趋势、热销 Top10、异常订单提醒与待发货概览。</sub>

### 库存中心 · 预警与可追溯流水

<a href="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/inventory.png">
  <img src="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/inventory.png" alt="MerchantFlow 库存中心：快照、预警与可追溯流水" width="100%" />
</a>

<sub>库存快照、安全库存预警与变更流水，便于追溯每一次库存调整。</sub>

### AI 运营助手 · 受控业务查询

<a href="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/ai-assistant.png">
  <img src="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/ai-assistant.png" alt="MerchantFlow AI 运营助手：只读查询与补货建议边界" width="100%" />
</a>

<sub>运营问题路由至只读业务查询；涉及库存变更时，仍须在标准业务流程中确认。</sub>

### 手机仓库模式 · 待处理与订单详情

<p align="center">
  <a href="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/warehouse-mobile.png">
    <img src="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/warehouse-mobile.png" alt="MerchantFlow 手机仓库待处理页" width="42%" />
  </a>
  <a href="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/orders-mobile-detail.png">
    <img src="https://raw.githubusercontent.com/M-Dragon123/merchantflow/main/docs/images/orders-mobile-detail.png" alt="MerchantFlow 手机订单详情与操作记录" width="42%" />
  </a>
</p>

<p align="center"><sub>扫码 / 手输查单、待发货处理、订单详情与操作记录。</sub></p>

## 页面与功能展示

| 页面 / 路由 | 可以看到什么 | 对应业务价值 |
| --- | --- | --- |
| 工作台 / | 今日订单、销售额、趋势图、热销 Top10、异常订单 | 用统一的付款时间口径查看经营数据 |
| 商品管理 /products | 分类、SPU、SKU、新建 / 编辑 / 停用商品 | 以 SKU 为库存和订单追溯最小单元 |
| 库存中心 /inventory | 库存快照、预警、入出库、全量流水、盘点 | 变动前后数量、原因和操作人可追溯 |
| 订单管理 /orders | 筛选、分页、订单详情、状态操作、操作日志 | 状态机阻止非法流转，覆盖付款、发货、退款、取消 |
| 客户 / 员工 /customers、/users | 客户订单记录、员工与角色管理 | 管理员、运营、仓库员、只读成员的权限边界 |
| AI 运营助手 /assistant | 补货、热销、经营、待发货、异常查询 | 助手只读；补货建议需二次确认才写入库存 |
| 手机仓库模式 /m | 待处理、扫码 / 手输查单、发货、库存调整、我的 | 小于 760px 自动切换四 Tab，不维护第二套应用 |

### 推荐演示路径

工作台 → AI 补货建议 → 二次确认入库 → 库存流水 → 手机仓库模式扫码查单 / 发货

这条路径能在几分钟内展示产品闭环、权限意识、库存可追溯性与多端适配。

## 核心设计亮点

### 1. 库存一致性与防超卖

- 库存变动使用带条件的原子更新，只有 available_qty + delta >= 0 才允许更新。
- 下单锁库存、付款扣库存、取消释放、盘点差异调整均在事务内执行；成功后同步写入库存流水。
- 受影响行数不为 1 时事务失败并返回冲突，避免可用库存被扣成负数。

### 2. 订单状态机与可追溯审计

- 合法路径：待付款 → 待发货 → 已发货 → 已完成，并支持取消与退款链路。
- 非法状态迁移在服务层直接拒绝；每次操作记录操作人、来源状态、目标状态和备注。
- 退款完成不直接回补库存，实物退回后必须走明确的入库或盘点流程，避免账面虚高。

### 3. 多端与权限

- 管理员、运营、仓库员、只读成员 4 类角色；前端路由和按钮级权限负责体验，服务端 @PreAuthorize 负责最终授权。
- PC 管理后台与手机仓库端共用一套 Vue 应用；移动端通过布局与断点切换为四 Tab 仓库模式。
- 商品只允许停用而非物理删除，防止破坏订单、库存和盘点的历史引用。

### 4. 受限 AI 运营助手

- 自然语言问题先经意图路由，再调用补货、热销、经营、待发货和异常订单等只读业务工具。
- 默认采用内置规则引擎，零外部依赖、结果可复现；保留可插拔 Provider 接口以便后续接入大模型。
- 助手不直接写库：补货建议必须由用户二次确认，再调用标准库存调整接口并落流水。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、Spring Security + JWT、Spring Data JPA、Flyway、springdoc / Swagger |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus、PWA |
| 数据与部署 | MySQL 8、Docker Compose、Nginx |
| 工程质量 | JUnit、PowerShell 端到端脚本、ESLint、vue-tsc、Git |

## 3 分钟启动与体验

### Docker 全栈启动

~~~powershell
git clone https://github.com/M-Dragon123/merchantflow.git
cd merchantflow
Copy-Item .env.example .env
docker compose up --build -d
~~~

首次启动时，后端会执行 Flyway 建表并灌入演示数据，约需 15 秒。随后访问：

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/api/v1/health
- Swagger：http://localhost:8080/swagger-ui.html

### 演示账号

统一密码：MerchantFlow@2026

| 账号 | 角色 | 体验重点 |
| --- | --- | --- |
| admin | 管理员 | 全部功能、员工管理、盘点、商品维护 |
| operator | 运营 | 商品、订单、客户、工作台、AI 助手 |
| warehouse | 仓库员 | 待发货、库存调整、盘点、移动仓库模式 |
| viewer | 只读成员 | 工作台和订单查看，验证最小权限边界 |

本地开发、便携工具链和 Docker / 本地环境切换方式见 [常用命令](docs/COMMANDS.md) 与 [部署说明](DEPLOY.md)。

## 可复现验证

| 验证项 | 命令 / 证据 |
| --- | --- |
| 后端单元测试 | cd backend && mvn test，7 个测试类、11 个测试用例 |
| 前端检查 | cd frontend && npm run lint && npm run build |
| 订单与权限链路 | powershell -ExecutionPolicy Bypass -File .\scripts\verify-m5.ps1，16 项断言 |
| 管理后台与盘点 | powershell -ExecutionPolicy Bypass -File .\scripts\verify-m7.ps1，18 项断言 |
| AI 助手链路 | powershell -ExecutionPolicy Bypass -File .\scripts\verify-assistant.ps1，13 项断言 |

共 3 个端到端验证脚本、47 项断言，覆盖四角色登录、订单流转、权限 403、盘点差异、AI 建议确认入库和异常响应等路径。

## 架构概览

~~~mermaid
flowchart LR
    U[PC 管理后台 / 手机仓库端] --> F[Vue 3 + TypeScript]
    F --> N[Nginx]
    N --> B[Spring Boot API]
    B --> S[Spring Security + JWT + RBAC]
    B --> O[订单状态机]
    B --> I[库存服务与流水]
    B --> A[AI 助手：意图路由 + 只读工具]
    O --> D[(MySQL 8)]
    I --> D
    A --> D
    M[Flyway 迁移与演示数据] --> D
~~~

## 文档导航

- [总体方案、ER 图、接口与页面清单](docs/PLAN.md)
- [部署说明](DEPLOY.md)
- [本地 / Docker 常用命令](docs/COMMANDS.md)
- [AI 助手端到端验证脚本](scripts/verify-assistant.ps1)
- [订单与权限验证脚本](scripts/verify-m5.ps1)
- [管理后台与盘点验证脚本](scripts/verify-m7.ps1)

> docs/PLAN.md 保留了设计和迭代过程记录；项目当前可用状态以本 README、迁移文件、源码和验证脚本为准。
