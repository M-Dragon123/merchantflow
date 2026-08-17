# 商家管家 MerchantFlow — 部署指南

> 面向"把系统部署到其他电脑 / 服务器"的完整步骤。命令速查见 [docs/COMMANDS.md](COMMANDS.md)。

---

## 1. 部署架构

```
浏览器 ──> 前端容器 (Nginx: 80) ──/api──> 后端容器 (Spring Boot: 8080) ──> MySQL 容器 (3306)
           静态文件 + 反向代理                     Flyway 自动建表 + 演示数据
```

三个服务全部由 `docker-compose.yml` 编排，一条命令启动；数据库、JWT 密钥等通过 `.env` 配置（未提供时使用内置默认值）。

## 2. 本地 Windows（Docker Desktop）部署

前置：已安装并启动 Docker Desktop（托盘图标为绿色；若 WSL2 报 `0x800705aa`，见 README「阶段 8」的管理员修复命令）。

```powershell
cd D:\agent\VibeCoding

# ① 准备环境变量（可选；不建文件则用默认值）
Copy-Item .env.example .env
# 编辑 .env：务必给生产环境设置强 JWT_SECRET 与数据库密码

# ② 构建并启动全栈（首次拉镜像 + 编译，约 5~10 分钟）
docker compose up --build -d

# ③ 等待后端就绪（Flyway 建表 + 灌演示数据约 15 秒）
docker compose ps            # 看到 mysql healthy、backend/frontend Up 即可

# ④ 验证
powershell -ExecutionPolicy Bypass -File .\scripts\verify-m5.ps1 -BaseUrl http://localhost:5173
```

访问：前端 `http://localhost:5173`，接口文档 `http://localhost:8080/swagger-ui.html`。
演示账号：`admin` / `operator` / `warehouse` / `viewer`，密码统一 `MerchantFlow@2026`（见 `.env` 可改数据库密码，但演示账号密码由迁移种子固定）。

## 3. Linux / macOS（MacBook）部署

核心思路：**换机器只差"怎么装 Docker"，装好之后命令完全一样**（Windows 多一步 WSL2 修复，Mac/Linux 都没有 WSL 问题）。

### 3.1 Linux 服务器

```bash
# ① 安装 Docker（Debian/Ubuntu；CentOS 用 yum/dnf 对应包名）
curl -fsSL https://get.docker.com | sh
sudo systemctl enable --now docker
sudo usermod -aG docker $USER && newgrp docker

# ② 获取项目代码（任选其一）
git clone <你的仓库地址> merchantflow && cd merchantflow
# 或从本机打包上传：
# tar czf merchantflow.tgz --exclude='.tools' --exclude='frontend/node_modules' --exclude='frontend/dist' --exclude='backend/target' .
# scp merchantflow.tgz user@server:/opt/ && ssh user@server 'cd /opt && tar xzf merchantflow.tgz'

# ③ 配置环境变量（生产必改）
cp .env.example .env
# JWT_SECRET=<至少32位随机串>   ← 务必修改
# MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD  ← 建议修改

# ④ 构建启动
docker compose up --build -d

# ⑤ 查看状态与日志（首次等后端启动）
docker compose ps
docker compose logs -f backend

# ⑥ 放行端口（以 5173 为例；也可改 compose 映射为 80）
sudo ufw allow 5173/tcp      # 或 cloud 安全组放行
```

访问 `http://<服务器IP>:5173`。

### 3.2 macOS（MacBook，Intel 或 Apple 芯片均可）

```bash
# ① 安装 Docker Desktop for Mac
# 方式一：brew install --cask docker
# 方式二：到 https://www.docker.com/products/docker-desktop/ 下载 .dmg 安装
# 装完启动 Docker Desktop，等状态变绿（首次可能要等它创建虚拟机）
# （Mac 没有 WSL 问题；Apple 芯片无需任何额外处理，本项目全部镜像都支持 arm64）

# ② 获取项目代码（任选其一）
git clone <你的仓库地址> merchantflow && cd merchantflow
# 或从 Windows 本机打包后 AirDrop/网盘/SSH 传过去：
# Windows 打包：tar czf merchantflow.tgz --exclude='.tools' --exclude='frontend/node_modules' --exclude='frontend/dist' --exclude='backend/target' .
# Mac 解压：tar xzf merchantflow.tgz

# ③ 配置环境变量（同 Linux，生产必改 JWT_SECRET）
cp .env.example .env

# ④ 构建启动（与 Linux 完全一致）
docker compose up --build -d

# ⑤ 状态与日志
docker compose ps
docker compose logs -f backend
```

访问 `http://localhost:5173`。

**Mac 上的验证方式**（`scripts/*.ps1` 是 PowerShell 脚本，Mac 默认没有 PowerShell）：
- 方式一：安装 PowerShell Core 后照常跑 `pwsh -File .\scripts\verify-m5.ps1`（`brew install --cask powershell`）；
- 方式二（推荐演示）：浏览器打开 `http://localhost:5173`，用 `admin / MerchantFlow@2026` 登录，对照 `docs/COMMANDS.md` 第 5 节手测。

## 4. 环境变量说明（`.env`）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_DATABASE` | merchantflow | 数据库名 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | merchantflow / merchantflow_dev_password | 应用账号（建库时创建） |
| `MYSQL_ROOT_PASSWORD` | root_dev_password | MySQL root 密码 |
| `JWT_SECRET` | 开发默认值 | **生产必须改为 ≥32 位随机串**，否则 token 可被伪造 |

`docker-compose.yml` 已内置 `TZ: Asia/Shanghai`（MySQL 另加 `--default-time-zone=+08:00`），按天统计与北京时间一致，无需额外配置。

## 5. 数据管理

- 数据存放在命名卷 `mysql-data`，`docker compose down` 不删除；`docker compose down -v` 才清空（下次启动重新建表 + 灌演示数据）。
- 备份：`docker compose exec mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" merchantflow' > backup.sql`
- 恢复：`docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" merchantflow' < backup.sql`

## 6. 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| 启动后访问 502 | 后端还在启动（Flyway 建表 + 演示数据约 15 秒），稍等后刷新；`docker compose logs backend` 确认 "Started MerchantFlowApplication" |
| `0x800705aa`（Windows） | WSL2 虚拟化组件未启用，见 README「阶段 8」管理员修复命令后重启 |
| 端口 3306/8080/5173 被占用 | 本地开发服务未停，先 `scripts/stop-all.ps1` 并 `Stop-Process -Name mysqld`，或 `docker compose down` |
| 想换前端端口 | 修改 `docker-compose.yml` 中 frontend 的 `"5173:80"` 映射，如 `"8080:80"`（注意避开后端 8080） |

## 7. 生产环境建议（演示以外的注意事项）

- 强制 HTTPS：在 Nginx 容器前加 Caddy / Nginx / 云负载均衡做 TLS 终结（本项目 Nginx 仅内网 HTTP）。
- 修改 `JWT_SECRET` 与数据库密码后再对外暴露。
- 演示账号密码（`MerchantFlow@2026`）由 Flyway 种子写入，仅适合演示；正式使用请在「员工管理」创建员工并停用演示账号。
- 后端日志目前为默认级别；需要接入集中日志/监控时按需调整 `application.yml`。
