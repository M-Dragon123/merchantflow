# 清理所有后端/前端进程，避免僵尸 Vite/后端实例导致旧状态串扰
# 用法：powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
# 清理后请重新启动：后端 dev-backend.ps1，前端 frontend 下 npm run dev
Write-Host '停止 java / node 进程...' -ForegroundColor Cyan
Get-Process java, node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
$left = Get-Process java, node -ErrorAction SilentlyContinue
if ($left) {
  Write-Host "仍有残留进程：$($left.Id -join ',')，请手动关闭相关终端窗口。" -ForegroundColor Yellow
} else {
  Write-Host '已全部停止。请重新启动后端与前端。' -ForegroundColor Green
}
