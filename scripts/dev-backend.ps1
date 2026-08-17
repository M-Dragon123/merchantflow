# 使用仓库内置便携 JDK/Maven 启动后端（开发模式）
# 前置：MySQL 已启动且已建库，例如 `docker compose up -d mysql`（在项目根目录执行）。
# 用法：powershell -ExecutionPolicy Bypass -File .\scripts\dev-backend.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jdk = Join-Path $root '.tools\jdk\jdk-21.0.12+8'
$mavenBin = Join-Path $root '.tools\maven\apache-maven-3.9.11\bin'

if (-not (Test-Path (Join-Path $jdk 'bin\java.exe'))) { Write-Host "未找到便携 JDK：$jdk" -ForegroundColor Red; exit 1 }
if (-not (Test-Path (Join-Path $mavenBin 'mvn.cmd'))) { Write-Host "未找到便携 Maven：$mavenBin" -ForegroundColor Red; exit 1 }

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$mavenBin;$env:Path"
# 走本机 127.0.0.1:7897 代理下载依赖（若无代理也无妨，Java 会回退直连）
$env:MAVEN_OPTS = '-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'

Write-Host "JAVA_HOME=$jdk" -ForegroundColor Cyan
Write-Host "启动后端 http://localhost:8080 （Ctrl+C 停止）..." -ForegroundColor Cyan
Push-Location (Join-Path $root 'backend')
try {
  & (Join-Path $mavenBin 'mvn.cmd') spring-boot:run
} finally {
  Pop-Location
}
