# MerchantFlow M7 管理后台端到端验证脚本
# 前置：后端已启动（localhost:8080）。
# 用法：powershell -ExecutionPolicy Bypass -File .\verify-m7.ps1 [-BaseUrl http://localhost:8080]
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
  $resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json'
  return $resp.data
}
function Headers($token) { return @{ Authorization = "Bearer $token" } }
# PS 5.1 函数返回 byte[] 会被展开成 Object[]（请求体损坏导致 401），必须用一元逗号保持单对象返回
function JsonBody($obj) { return ,[System.Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 5)) }

Write-Host "== M7 验证（$BaseUrl）=="

$admin = Login 'admin'
$warehouse = Login 'warehouse'
$viewer = Login 'viewer'
$ha = Headers $admin.accessToken
$hw = Headers $warehouse.accessToken
$hv = Headers $viewer.accessToken
Check '登录（admin/warehouse/viewer）' { @($admin, $warehouse, $viewer) | ForEach-Object { if ($null -eq $_.accessToken) { throw 'token 为空' } } }

# 1. 员工管理
$stamp = Get-Date -Format 'HHmmss'
$newUser = "v7user$stamp"
Check '员工-新建（admin）' {
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/users" -Method Post -Headers $ha -Body (JsonBody @{ username = $newUser; password = 'V7Pass@123'; name = 'M7验证员'; roleCode = 'VIEWER' }) -ContentType 'application/json' | Out-Null
}
Check '员工-新建重名应 400' {
  try {
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/users" -Method Post -Headers $ha -Body (JsonBody @{ username = $newUser; password = 'V7Pass@123'; name = '重复'; roleCode = 'VIEWER' }) -ContentType 'application/json' | Out-Null
    throw '未拦截'
  } catch { if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw "期望400实际$($_.Exception.Response.StatusCode.value__)" } }
}
Check '员工-改角色（VIEWER→WAREHOUSE+VIEWER）' {
  $u = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/users" -Headers $ha).data | Where-Object { $_.username -eq $newUser }
  $r = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/users/$($u.id)/roles" -Method Put -Headers $ha -Body (JsonBody @{ roleCodes = @('WAREHOUSE', 'VIEWER') }) -ContentType 'application/json').data
  if ($r.roles -notcontains 'WAREHOUSE') { throw '角色未更新' }
}
Check '新员工按新角色可访问盘点列表' {
  $body = @{ username = $newUser; password = 'V7Pass@123' } | ConvertTo-Json
  $resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json'
  if ($resp.data.user.roles -notcontains 'WAREHOUSE') { throw '角色未生效' }
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/stocktakes" -Headers (Headers $resp.data.accessToken) | Out-Null
}
Check '员工-停用后无法登录' {
  $u = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/users" -Headers $ha).data | Where-Object { $_.username -eq $newUser }
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/users/$($u.id)/status" -Method Put -Headers $ha -Body (JsonBody @{ status = $false }) -ContentType 'application/json' | Out-Null
  $body = @{ username = $newUser; password = 'V7Pass@123' } | ConvertTo-Json
  try {
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json' | Out-Null
    throw '停用后仍可登录'
  } catch { if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw "期望401实际$($_.Exception.Response.StatusCode.value__)" } }
}
Check '仓库员访问员工列表应 403' {
  try { Invoke-RestMethod -Uri "$BaseUrl/api/v1/users" -Headers $hw | Out-Null; throw '未拦截' }
  catch { if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw "期望403实际$($_.Exception.Response.StatusCode.value__)" } }
}

# 2. 库存盘点全流程
$stId = $null
Check '盘点-创建（warehouse）' {
  $st = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/stocktakes" -Method Post -Headers $hw).data
  if ($st.status -ne 'DRAFT') { throw '应为草稿' }
  $script:stId = $st.id
}
Check '盘点-明细含全部 SKU' {
  $d = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/stocktakes/$($script:stId)" -Headers $hw).data
  if ($d.items.Count -lt 3) { throw "明细过少: $($d.items.Count)" }
  $script:first = $d.items[0]
}
Check '盘点-录入实盘数（造差异）' {
  $line = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/stocktakes/$($script:stId)/items/$($script:first.skuId)" -Method Put -Headers $hw -Body (JsonBody @{ skuId = $script:first.skuId; countedQty = ([int]$script:first.systemQty + 3) }) -ContentType 'application/json').data
  if ($line.diffQty -ne 3) { throw "差异应为3实际$($line.diffQty)" }
}
Check '盘点-完成并生成 ADJUSTMENT 流水' {
  $c = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/stocktakes/$($script:stId)/complete" -Method Post -Headers $hw).data
  if ($c.status -ne 'COMPLETED') { throw '状态未完成' }
  $tx = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/inventory/transactions" -Headers $hw).data
  if (-not ($tx | Where-Object { $_.type -eq 'ADJUSTMENT' -and $_.delta -eq 3 })) { throw '未找到 +3 盘点流水' }
}

# 3. 客户管理
Check '客户-列表分页' {
  $r = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers?page=1&size=20" -Headers $ha).data
  if ($null -eq $r.total -or $null -eq $r.items) { throw '分页结构缺失' }
}
Check '客户-关键字搜索（13800000001）' {
  $r = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers?keyword=13800000001&page=1&size=20" -Headers $ha).data
  if ($r.items.Count -ne 1 -or $r.items[0].mobile -ne '13800000001') { throw '搜索结果不符' }
}
Check '客户-详情含最近订单' {
  $r = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers?keyword=13800000001&page=1&size=5" -Headers $ha).data
  $d = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers/$($r.items[0].id)" -Headers $ha).data
  if ($d.recentOrders.Count -eq 0) { throw '无最近订单' }
}
Check '只读成员访问客户管理应 403' {
  try { Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers" -Headers $hv | Out-Null; throw '未拦截' }
  catch { if ($_.Exception.Response.StatusCode.value__ -ne 403) { throw "期望403实际$($_.Exception.Response.StatusCode.value__)" } }
}

# 4. 商品管理
$newSku = "MF-V7-$stamp"
Check '商品-新建' {
  Invoke-RestMethod -Uri "$BaseUrl/api/v1/products" -Method Post -Headers $ha -Body (JsonBody @{ name = 'M7验证商品'; categoryId = 1; skuCode = $newSku; salePrice = 66.6; costPrice = 30; initialQty = 20; safetyStock = 5 }) -ContentType 'application/json' | Out-Null
}
Check '商品-编辑价格与状态' {
  $p = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/products" -Headers $ha).data | Where-Object { $_.skuCode -eq $newSku }
  $u = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/products/$($p.skuId)" -Method Put -Headers $ha -Body (JsonBody @{ salePrice = 88.8; status = 'INACTIVE' }) -ContentType 'application/json').data
  if ([decimal]$u.salePrice -ne 88.8 -or $u.status -ne 'INACTIVE') { throw '更新结果不符' }
  $script:newSkuId = $u.skuId
}
Check '停用 SKU 不可下单（应 409）' {
  try {
    $body = @{ customerName = '验证'; customerMobile = '13700000099'; items = @(@{ skuId = $script:newSkuId; quantity = 1 }) }
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers $ha -Body (JsonBody $body) -ContentType 'application/json' | Out-Null
    throw '未拦截'
  } catch { if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw "期望409实际$($_.Exception.Response.StatusCode.value__)" } }
}

Write-Host ""
Write-Host "== 结果：PASS $pass / FAIL $fail ==" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }
