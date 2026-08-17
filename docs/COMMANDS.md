# 商家管家 MerchantFlow — 常用命令速查

> 部署步骤见 [../DEPLOY.md](../DEPLOY.md)。所有脚本均需在项目根目录 `D:\agent\VibeCoding` 下执行（除特别注明）。
> **非 Windows 机器**：Docker 部署命令（第 2 节）在 Linux/macOS 完全一致，仅安装 Docker 的方式不同，见 [../DEPLOY.md](../DEPLOY.md) 第 3 节；`scripts/*.ps1` 需 PowerShell Core（`brew install --cask powershell`）或在 Mac 上直接浏览器手测。

## 0. 两条部署路径总览

| 路径 | 特点 | 何时用 |
|---|---|---|
| **本地开发**（便携 MySQL + JDK/Maven） | 无需 Docker，改代码即时生效（Vite HMR） | 日常开发、调试 |
| **Docker 全栈** | 一条命令起全套、环境一致 | 部署到其他电脑/服务器、整体演示 |

两条路径不能同时占用端口（3306/8080/5173），切换前先停另一套。

---

## 1. 本地开发

```powershell
# ① 启动 MySQL（首次会自动下载安装约 250MB；之后 mysqld 已在跑则跳过）
powershell -ExecutionPolicy Bypass -File .\scripts\setup-mysql-portable.ps1

# ② 启动后端（首次拉取依赖较久）→ http://localhost:8080
powershell -ExecutionPolicy Bypass -File .\scripts\dev-backend.ps1

# ③ 启动前端 → http://localhost:5173 （必须先在 frontend 目录）
cd frontend
npm run dev

# 检查与测试
npm run lint                 # ESLint
npm run build                # 类型检查 + 生产构建
mvn test                     # 后端单元测试（在 backend 目录）
```

**停止本地开发**：
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1   # 停止 java/node（后端+前端）
Stop-Process -Name mysqld                                          # 停止便携 MySQL
```

---

## 2. Docker 全栈

```powershell
# 启动（首次构建 5~10 分钟）→ 前端 http://localhost:5173
docker compose up --build -d

# 常用管理命令
docker compose ps                       # 查看容器状态（等 mysql healthy + backend Up）
docker compose logs -f backend          # 跟踪后端日志（首次等 "Started MerchantFlowApplication"）
docker compose logs --tail 50 backend   # 查看最近 50 行
docker compose down                     # 停止（保留数据库）
docker compose down -v                  # 停止并清空数据库（下次启动重新建表+演示数据）
docker compose up -d                    # 改完配置后重新应用（仅重建变更的容器）
docker compose up --build -d            # 代码/镜像变更后重建并启动
```

---

## 3. 本地开发 ⇄ Docker 切换

```powershell
# 本地 → Docker：先释放端口，再起容器
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
Stop-Process -Name mysqld
docker compose up --build -d

# Docker → 本地：先停容器，再起本地栈
docker compose down
powershell -ExecutionPolicy Bypass -File .\scripts\setup-mysql-portable.ps1   # mysqld 未运行才需要
powershell -ExecutionPolicy Bypass -File .\scripts\dev-backend.ps1
cd frontend; npm run dev
```

> 注意：两套环境数据库相互独立（Docker 用命名卷，本地用 `.tools\mysql-data`）。本地库里你手工创建的数据不会出现在 Docker 栈中。

---

## 4. 端到端验证脚本

```powershell
# M5 核心链路（四角色登录/工作台/订单全流程/权限 403）—— 16 项
powershell -ExecutionPolicy Bypass -File .\scripts\verify-m5.ps1
# M7 管理后台（员工/盘点/客户/商品）—— 18 项
powershell -ExecutionPolicy Bypass -File .\scripts\verify-m7.ps1

# 默认连 http://localhost:8080（本地后端）；连 Docker 栈时指定前端入口：
powershell -ExecutionPolicy Bypass -File .\scripts\verify-m5.ps1 -BaseUrl http://localhost:5173
powershell -ExecutionPolicy Bypass -File .\scripts\verify-m7.ps1 -BaseUrl http://localhost:5173

# 阶段 2 AI 助手冒烟（管理员登录 → 补货问答 → 建议卡片 → 二次确认入库）
powershell -ExecutionPolicy Bypass -File .\scripts\verify-assistant.ps1
```

脚本要点：UTF-8 安全（中文请求体正确发送）、`Check` 脚本块内变量用 `$script:` 前缀共享，已在脚本内注释说明。

---

## 5. 演示账号与入口

| 账号 | 角色 | 权限要点 |
|---|---|---|
| `admin` | 管理员 | 全部功能（含员工管理、盘点、商品维护） |
| `operator` | 运营 | 商品/订单/客户/报表，无员工管理与盘点 |
| `warehouse` | 仓库员 | 待发货/发货/盘点/库存调整，无商品与客户管理 |
| `viewer` | 只读成员 | 仅工作台与订单查看 |

密码统一：`MerchantFlow@2026`

入口：
- 本地前端 `http://localhost:5173`、Docker 前端 `http://localhost:5173`
- 后端健康检查 `http://localhost:8080/api/v1/health`
- Swagger 接口文档 `http://localhost:8080/swagger-ui.html`
- 移动端仓库模式：浏览器 F12 切手机视图，或直接访问 `http://localhost:5173/m`

---

## 6. 常见排查

| 现象 | 处理 |
|---|---|
| 登录报「无法连接服务器」 | 后端没起：本地跑 `dev-backend.ps1`，Docker 跑 `docker compose up -d` |
| 页面全是「请求失败（HTTP 403）」 | 浏览器残留过期 token：`Ctrl+Shift+R` 强刷（会自动跳登录页） |
| 登录报「用户名或密码错误」 | 账号密码大小写/拼写；`warehouse` 不是 `warhouse` |
| Docker 栈 502 | 后端还在启动（约 15 秒），等 `docker compose logs backend` 出现 Started 再访问 |
| 端口被占用 | 见第 3 节切换流程，先停另一套 |
