# MerchantFlow M5 端到端验证脚本
# 前置：MySQL 已启动且已建库（如 docker compose up -d mysql）、后端已启动（localhost:8080）。
# 用法：powershell -ExecutionPolicy Bypass -File .\verify-m5.ps1 [-BaseUrl http://localhost:8080]
param(
  [string]$BaseUrl = 'http://localhost:8080'
)
$ErrorActionPreference = 'Stop'
$pass = 0
$fail = 0
function Check([string]$name, [scriptblock]$action) {
  try {
    & $action | Out-Null
    Write-Host "[PASS] $name" -ForegroundColor Green
    $script:pass++
  } catch {
    Write-Host "[FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red
    $script:fail++
  }
}
function Login([string]$username) {
  $body = @{ username = $username; password = 'MerchantFlow@2026' } | ConvertTo-Json
  $resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $body -ContentType 'application/json'
  return $resp.data
}
function Headers($token) { return @{ Authorization = "Bearer $token" } }
# PS 5.1 发送字符串请求体时按 ISO-8859-1 编码，中文会变成 ????；必须转为 UTF-8 字节数组
# PS 5.1 函数返回 byte[] 会被展开成 Object[]（请求体损坏导致 401），必须用一元逗号保持单对象返回
function JsonBody($obj) { return ,[System.Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 5)) }

Write-Host "== M5 验证（$BaseUrl）=="

# 1. 四角色登录
$admin = Login 'admin'
$operator = Login 'operator'
$warehouse = Login 'warehouse'
$viewer = Login 'viewer'
Check '四角色登录（admin/operator/warehouse/viewer）' { @($admin, $operator, $warehouse, $viewer) | ForEach-Object { if ($null -eq $_.accessToken) { throw 'token 为空' } } }
Check '管理员角色为 ADMIN' { if ($admin.user.roles -notcontains 'ADMIN') { throw '角色错误' } }

# 2. 工作台统计
Check 'dashboard/summary 返回四项指标' {
  $s = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/summary" -Headers (Headers $admin.accessToken)).data
  if ($null -eq $s.todayOrders -or $null -eq $s.todaySales -or $null -eq $s.pendingShipment -or $null -eq $s.lowStock) { throw '字段缺失' }
}
Check 'dashboard/sales-trend 近 14 天连续' {
  $t = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/sales-trend?days=14" -Headers (Headers $admin.accessToken)).data
  if ($t.Count -ne 14) { throw "期望 14 个点，实际 $($t.Count)" }
}
Check 'dashboard/top-products 与 anomalies 可访问' {
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/top-products?days=30" -Headers (Headers $admin.accessToken) | Out-Null
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/anomalies" -Headers (Headers $admin.accessToken) | Out-Null
}

# 3. 订单检索与分页
Check '订单列表分页（page=1&size=20）' {
  $r = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders?page=1&size=20" -Headers (Headers $admin.accessToken)).data
  if ($null -eq $r.total -or $null -eq $r.items) { throw '分页结构缺失' }
}
Check '订单关键字检索（订单号前缀 MF）' {
  $r = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders?keyword=MF&page=1&size=20" -Headers (Headers $admin.accessToken)).data
  if ($r.items | Where-Object { $_.orderNo -notlike '*MF*' }) { throw '返回了不匹配的订单' }
}

# 4. 建单 -> 付款 -> 发货 -> 详情 -> 日志 -> by-no（管理员全流程）
# 注意：Check 的脚本块内赋值只对局部作用域生效，跨步骤共享必须用 $script: 前缀
$script:created = $null
Check '创建订单并锁定库存' {
  $products = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/products" -Headers (Headers $admin.accessToken)).data
  # 选取「在售且有库存」的 SKU（避免选到停用/无库存商品导致 409）
  $sku = $products | Where-Object { $_.status -eq 'ACTIVE' -and $_.availableQty -gt 0 } | Select-Object -First 1
  if (-not $sku) { throw '无在售且有库存的商品' }
  $body = @{
    customerName = '验证客户'; customerMobile = ('139' + (Get-Random -Minimum 10000000 -Maximum 99999999).ToString())
    items = @(@{ skuId = $sku.skuId; quantity = 1 })
  }
  $script:created = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers (Headers $admin.accessToken) -Body (JsonBody $body) -ContentType 'application/json').data
  if ($script:created.status -ne 'PENDING_PAYMENT') { throw '建单后状态应为待付款' }
}
Check '确认付款（扣减库存并记录 paid_at）' {
  $o = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders/$($script:created.id)/pay" -Method Post -Headers (Headers $admin.accessToken)).data
  if ($o.status -ne 'PENDING_SHIPMENT' -or $null -eq $o.paidAt) { throw '付款后应为待发货且 paid_at 非空' }
}
Check '仓库员发货' {
  $o = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders/$($script:created.id)/ship" -Method Post -Headers (Headers $warehouse.accessToken) -Body (JsonBody @{ remark = '脚本验证发货' }) -ContentType 'application/json').data
  if ($o.status -ne 'SHIPPED') { throw '发货后应为已发货' }
}
Check '订单详情含明细' {
  $d = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders/$($script:created.id)" -Headers (Headers $admin.accessToken)).data
  if ($d.items.Count -eq 0 -or $null -eq $d.order.customerName) { throw '明细或客户名缺失' }
}
Check '操作日志完整（创建/付款/发货）' {
  $logs = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders/$($script:created.id)/logs" -Headers (Headers $admin.accessToken)).data
  $actions = $logs | ForEach-Object { $_.action }
  if ($actions -notcontains 'CREATE' -or $actions -notcontains 'PAY' -or $actions -notcontains 'SHIP') { throw "日志缺失: $($actions -join ',')" }
}
Check '扫码/手输 by-no 查询' {
  $d = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders/by-no/$($script:created.orderNo)" -Headers (Headers $admin.accessToken)).data
  if ($d.order.orderNo -ne $script:created.orderNo) { throw '订单号不一致' }
}

# 5. 角色权限
Check '只读成员访问工作台（允许）' {
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/dashboard/summary" -Headers (Headers $viewer.accessToken) | Out-Null
}
Check '只读成员访问商品列表（应 403）' {
  try { Invoke-RestMethod -Uri "$BaseUrl/api/v1/products" -Headers (Headers $viewer.accessToken) | Out-Null; throw '未拦截' }
  catch { if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw "期望 403，实际 $($_.Exception.Response.StatusCode.value__)" } }
}
Check '仓库员建单（应 403）' {
  try {
    $body = @{ customerName = 'x'; customerMobile = '13900000000'; items = @(@{ skuId = 1; quantity = 1 }) } | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers (Headers $warehouse.accessToken) -Body $body -ContentType 'application/json' | Out-Null
    throw '未拦截'
  } catch { if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw "期望 403，实际 $($_.Exception.Response.StatusCode.value__)" } }
}

Write-Host ""
Write-Host "== 结果：PASS $pass / FAIL $fail ==" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }
