# 便携版 MySQL 8.4 一键安装并启动（无需 Docker、无需管理员、无需安装）
# 用途：为本地后端提供 MySQL；库名/账号与 backend/application.yml 默认值一致。
# 用法：powershell -ExecutionPolicy Bypass -File .\scripts\setup-mysql-portable.ps1
# 停止：Stop-Process -Name mysqld
$ErrorActionPreference = 'Stop'
# PS 5.1 默认 TLS 1.0，必须显式启用 TLS 1.2 才能访问 MySQL 官方源
try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch {}
$root = Split-Path -Parent $PSScriptRoot
$tools = Join-Path $root '.tools'
$mysqlHome = Join-Path $tools 'mysql'
$dataDir = Join-Path $tools 'mysql-data'
$zipPath = Join-Path $tools 'mysql.zip'

function Download-MySql {
  if (Test-Path $zipPath) {
    $size = (Get-Item $zipPath).Length / 1MB
    if ($size -gt 100) { Write-Host "复用已有安装包 $([math]::Round($size,1)) MB" -ForegroundColor Cyan; return }
  }
  # 依次探测 8.4.x 版本，官方源失败时自动切换国内镜像（下载约 250MB）
  # 注意：dev.mysql.com/get 对非浏览器 UA 返回 403，故统一带浏览器 UA；cdn.mysql.com 为直连 CDN
  $versions = @('8.4.8', '8.4.7', '8.4.6', '8.4.5', '8.4.4', '8.4.3', '8.4.2', '8.4.1', '8.4.0')
  $mirrors = @(
    'https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-{0}-winx64.zip',
    'https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-{0}-winx64.zip',
    'https://mirrors.aliyun.com/mysql/Downloads/MySQL-8.4/mysql-{0}-winx64.zip',
    'https://mirrors.huaweicloud.com/mysql/Downloads/MySQL-8.4/mysql-{0}-winx64.zip',
    'https://mirrors.tuna.tsinghua.edu.cn/mysql/downloads/MySQL-8.4/mysql-{0}-winx64.zip'
  )
  $headers = @{ 'User-Agent' = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36' }
  foreach ($v in $versions) {
    foreach ($tpl in $mirrors) {
      $url = $tpl -f $v
      try {
        Write-Host "尝试下载 $url ..." -ForegroundColor Cyan
        Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing -TimeoutSec 600 -Headers $headers
        $len = (Get-Item $zipPath).Length
        if ($len -gt 100MB) {
          $head = [System.IO.File]::ReadAllBytes($zipPath)[0..1]
          if ($head[0] -eq 80 -and $head[1] -eq 75) { Write-Host "下载完成：$v" -ForegroundColor Green; return }
        }
        Write-Host '  文件不完整，删除重试' -ForegroundColor Yellow
        Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
      } catch {
        Write-Host "  失败：$($_.Exception.Message)" -ForegroundColor Yellow
        Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
      }
    }
  }
  Write-Host "自动下载失败，请手动下载 https://dev.mysql.com/downloads/mysql/ 的 Windows x64 ZIP 包，重命名为 .tools\mysql.zip 后重跑本脚本。" -ForegroundColor Red
  exit 1
}

if (-not (Test-Path (Join-Path $mysqlHome 'bin\mysqld.exe')) -or -not (Test-Path (Join-Path $mysqlHome 'bin\mysql.exe'))) {
  Download-MySql
  Write-Host '解压 MySQL...' -ForegroundColor Cyan
  Expand-Archive -Path $zipPath -DestinationPath $tools -Force
  $inner = Get-ChildItem $tools -Directory | Where-Object { $_.Name -like 'mysql-8.4*' } | Select-Object -First 1
  if (-not $inner) { Write-Host '解压后未找到 mysql-8.4 目录' -ForegroundColor Red; exit 1 }
  if ($inner.Name -match 'debug|test') {
    Write-Host '检测到测试版压缩包（' + $inner.Name + '）：该包只有 mysqld-debug，没有正式服务器与客户端。' -ForegroundColor Red
    Write-Host '请重新下载正式版 ZIP：dev.mysql.com/downloads/mysql → 版本选 8.4.x LTS → 操作系统选 Microsoft Windows → 下载名为 mysql-8.4.x-winx64.zip 的 "ZIP Archive"（注意不要选带 debug-test 的条目）。' -ForegroundColor Yellow
    Write-Host '下载后覆盖 .tools\mysql.zip，然后重跑本脚本。' -ForegroundColor Yellow
    Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
    exit 1
  }
  if (Test-Path $mysqlHome) { Remove-Item $mysqlHome -Recurse -Force }
  Move-Item $inner.FullName $mysqlHome
  Write-Host "MySQL 已就位：$mysqlHome" -ForegroundColor Green
}

$mysqld = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysql = Join-Path $mysqlHome 'bin\mysql.exe'
if (-not (Test-Path $mysqld) -or -not (Test-Path $mysql)) {
  Write-Host '缺少 bin\mysqld.exe 或 bin\mysql.exe，请确认下载的是正式版 ZIP 包（不是 debug-test）' -ForegroundColor Red
  exit 1
}
$logDir = Join-Path $tools 'mysql-log'
New-Item -ItemType Directory -Force -Path $logDir, $dataDir | Out-Null

if (-not (Test-Path (Join-Path $dataDir 'mysql'))) {
  Write-Host '初始化数据目录（root 空密码）...' -ForegroundColor Cyan
  $prevEap = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  & $mysqld --initialize-insecure "--datadir=$dataDir" --console
  $initCode = $LASTEXITCODE
  $ErrorActionPreference = $prevEap
  if ($initCode -ne 0 -and -not (Test-Path (Join-Path $dataDir 'mysql'))) {
    Write-Host '初始化失败（退出码 ' + $initCode + '），请查看上方输出' -ForegroundColor Red
    exit 1
  }
}

if (-not (Get-Process -Name mysqld -ErrorAction SilentlyContinue)) {
  Write-Host '启动 mysqld (3306)...' -ForegroundColor Cyan
  Start-Process -FilePath $mysqld -ArgumentList "--datadir=$dataDir", '--port=3306', '--bind-address=127.0.0.1', "--log-error=$logDir\mysqld.err" -WindowStyle Hidden
}

Write-Host '等待 MySQL 就绪...' -ForegroundColor Cyan
$ready = $false
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
for ($i = 0; $i -lt 60; $i++) {
  Start-Sleep -Seconds 1
  & $mysql -uroot '--host=127.0.0.1' '--protocol=tcp' -e 'SELECT 1' 2>&1 | Out-Null
  if ($LASTEXITCODE -eq 0) { $ready = $true; break }
}
$ErrorActionPreference = $prevEap
if (-not $ready) {
  Write-Host 'MySQL 未在 60 秒内就绪，查看日志：' -ForegroundColor Red
  if (Test-Path (Join-Path $logDir 'mysqld.err')) { Get-Content (Join-Path $logDir 'mysqld.err') -Tail 30 }
  exit 1
}

Write-Host '创建 merchantflow 库与账号...' -ForegroundColor Cyan
$sql = @"
CREATE DATABASE IF NOT EXISTS merchantflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'merchantflow'@'localhost' IDENTIFIED BY 'merchantflow_dev_password';
CREATE USER IF NOT EXISTS 'merchantflow'@'%' IDENTIFIED BY 'merchantflow_dev_password';
GRANT ALL PRIVILEGES ON merchantflow.* TO 'merchantflow'@'localhost';
GRANT ALL PRIVILEGES ON merchantflow.* TO 'merchantflow'@'%';
FLUSH PRIVILEGES;
"@
& $mysql -uroot '--host=127.0.0.1' '--protocol=tcp' -e $sql
$provCode = $LASTEXITCODE
$ErrorActionPreference = $prevEap
if ($provCode -ne 0) { Write-Host '建库/建账号失败' -ForegroundColor Red; exit 1 }

Write-Host ''
Write-Host 'MySQL 便携版就绪：127.0.0.1:3306' -ForegroundColor Green
Write-Host "  库：merchantflow    用户：merchantflow    密码：merchantflow_dev_password" -ForegroundColor Green
Write-Host '现在可以启动后端：powershell -ExecutionPolicy Bypass -File .\scripts\dev-backend.ps1' -ForegroundColor Cyan
Write-Host '停止 MySQL：Stop-Process -Name mysqld' -ForegroundColor Yellow
