# MerchantFlow 阶段 2（AI 运营助手）端到端验证脚本
# 前置：MySQL 已启动且已建库、后端已启动（localhost:8080）。
# 用法：powershell -ExecutionPolicy Bypass -File .\verify-assistant.ps1 [-BaseUrl http://localhost:8080]
#
# 要点：
# - 响应统一经 Invoke-Api 用 UTF-8 字节流解码（PS 5.1 的 Invoke-RestMethod 会把无 charset 的
#   JSON 按 ISO-8859-1 解码成乱码，中文断言会失败）。
# - 补货建议依赖"存在低库存商品"，脚本先用库存调整接口把某个 SKU 压到安全库存以下，
#   保证建议卡片确定出现，再验证"建议 -> 二次确认 -> 标准入库接口 -> 流水"整条链路。
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

# 请求体：hashtable -> JSON 字符串
function To-Json($obj) { return $obj | ConvertTo-Json -Depth 5 }
# 统一请求入口：成功与失败都返回 @{ Status = int; Body = 解析后的 JSON 对象（UTF-8）或 $null }
function Invoke-Api([string]$Method, [string]$Path, [string]$Token, $JsonBody) {
  $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true }
  if ($Token) { $params.Headers = @{ Authorization = "Bearer $Token" } }
  if ($null -ne $JsonBody) { $params.Body = [System.Text.Encoding]::UTF8.GetBytes($JsonBody); $params.ContentType = 'application/json' }
  try {
    $resp = Invoke-WebRequest @params
    $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    $parsed = $null
    try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null }
    return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $parsed }
  } catch {
    $err = $_.Exception
    if ($err.Response) {
      $status = [int]$err.Response.StatusCode
      $reader = New-Object System.IO.StreamReader($err.Response.GetResponseStream(), [System.Text.Encoding]::UTF8)
      $text = $reader.ReadToEnd()
      $parsed = $null
      try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null }
      return [pscustomobject]@{ Status = $status; Body = $parsed }
    }
    throw $err
  }
}
function Login([string]$username) {
  $r = Invoke-Api 'POST' '/api/v1/auth/login' $null (To-Json @{ username = $username; password = 'MerchantFlow@2026' })
  if ($r.Status -ne 200) { throw "登录失败 $username：HTTP $($r.Status)" }
  return $r.Body.data
}

Write-Host "== 阶段 2 AI 助手验证（$BaseUrl）=="

# 1. 登录与权限
$admin = Login 'admin'
$operator = Login 'operator'
$viewer = Login 'viewer'
Check '管理员/运营/只读三账号登录' { @($admin, $operator, $viewer) | ForEach-Object { if ($null -eq $_.accessToken) { throw 'token 为空' } } }

# 2. 制造确定性的低库存：把某 SKU 调整到 available = safety - 1（alerts 条件 available <= safety）
$script:targetSku = $null
Check '制造低库存条件（压到安全库存以下）' {
  $r = Invoke-Api 'GET' '/api/v1/inventory' $admin.accessToken $null
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $inv = $r.Body.data
  $sku = $inv | Where-Object { $_.availableQty -gt $_.safetyStock -and $_.safetyStock -ge 2 } | Select-Object -First 1
  if ($null -eq $sku) { throw '没有可操作的 SKU（需要 available > safety >= 2）' }
  $delta = -($sku.availableQty - $sku.safetyStock + 1)
  $adj = Invoke-Api 'POST' '/api/v1/inventory/adjustments' $admin.accessToken (To-Json @{ skuId = $sku.skuId; delta = $delta; type = 'OUTBOUND'; reason = '验证脚本制造低库存' })
  if ($adj.Status -ne 200 -or -not $adj.Body.success) { throw "调整失败：HTTP $($adj.Status)" }
  $script:targetSku = $sku
}

# 3. 补货意图：只读查询 + 建议卡片（必须命中目标 SKU 且入库量 > 0）
$script:suggestion = $null
Check '补货问答返回建议卡片并命中目标 SKU' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $admin.accessToken (To-Json @{ message = '哪些商品建议补货？' })
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $data = $r.Body.data
  if ($data.reply -notmatch '补货') { throw "reply 未命中补货意图: $($data.reply)" }
  $s = $data.suggestions | Where-Object { $_.skuId -eq $script:targetSku.skuId } | Select-Object -First 1
  if ($null -eq $s) { throw "建议卡片未包含目标 SKU $($script:targetSku.skuId)" }
  if ($s.delta -le 0) { throw "建议入库量应 > 0，实际 $($s.delta)" }
  $script:suggestion = $s
}

# 4. 其余意图路由
Check '热销问答' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $operator.accessToken (To-Json @{ message = '最近卖得最好的商品？' })
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $reply = $r.Body.data.reply
  if ($reply -notmatch '热销') { throw "reply 未命中热销意图: $reply" }
}
Check '经营概况问答' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $operator.accessToken (To-Json @{ message = '今天销售额怎么样？' })
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $reply = $r.Body.data.reply
  if ($reply -notmatch '今日订单|销售额') { throw "reply 未命中经营意图: $reply" }
}
Check '待发货问答' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $operator.accessToken (To-Json @{ message = '有多少待发货订单？' })
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $reply = $r.Body.data.reply
  if ($reply -notmatch '待发货') { throw "reply 未命中待发货意图: $reply" }
}
Check '异常订单问答' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $admin.accessToken (To-Json @{ message = '有什么异常订单要处理？' })
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $reply = $r.Body.data.reply
  if ([string]::IsNullOrWhiteSpace($reply)) { throw 'reply 为空' }
}
Check '帮助意图兜底' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $admin.accessToken (To-Json @{ message = '你能做什么？' })
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $reply = $r.Body.data.reply
  if ($reply -notmatch '我可以帮你') { throw '未命中帮助意图' }
}

# 5. 建议二次确认后走标准入库接口（记录操作人），助手本身不写库
Check '一键补货：按建议卡片执行入库' {
  $s = $script:suggestion
  $r = Invoke-Api 'POST' '/api/v1/inventory/adjustments' $admin.accessToken (To-Json @{ skuId = $s.skuId; delta = $s.delta; type = 'INBOUND'; reason = "AI 建议补货：$($s.skuCode)（脚本验证）" })
  if ($r.Status -ne 200 -or -not $r.Body.success) { throw "入库失败：HTTP $($r.Status)" }
}
Check '入库流水已记录（含操作人 admin）' {
  $r = Invoke-Api 'GET' '/api/v1/inventory/transactions' $admin.accessToken $null
  if ($r.Status -ne 200) { throw "HTTP $($r.Status)" }
  $txs = $r.Body.data
  $hit = $txs | Where-Object { $_.type -eq 'INBOUND' -and $_.operator -eq 'admin' -and $_.reason -match 'AI 建议补货' } | Select-Object -First 1
  if ($null -eq $hit) { throw '未找到 AI 补货产生的 INBOUND 流水' }
}

# 6. 权限：只读角色不可用助手
Check '只读成员调用助手（应 403）' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $viewer.accessToken (To-Json @{ message = '哪些商品建议补货？' })
  if ($r.Status -ne 403) { throw "期望 403，实际 $($r.Status)" }
}

# 7. API 健壮性回归：畸形请求体 / 未知路径必须返回 400/404 JSON，而不是裸 401
Check '畸形请求体返回 400 JSON（而非裸 401）' {
  $r = Invoke-Api 'POST' '/api/v1/assistant/chat' $admin.accessToken 'not-json{{{'
  if ($r.Status -ne 400) { throw "期望 400，实际 $($r.Status)" }
  if ($null -eq $r.Body -or $null -eq $r.Body.data.message) { throw '缺少统一错误 JSON' }
}
Check '未知接口返回 404 JSON（而非裸 401）' {
  $r = Invoke-Api 'GET' '/api/v1/no-such-path' $admin.accessToken $null
  if ($r.Status -ne 404) { throw "期望 404，实际 $($r.Status)" }
  if ($null -eq $r.Body -or $null -eq $r.Body.data.message) { throw '缺少统一错误 JSON' }
}

Write-Host ""
Write-Host "== 结果：PASS $pass / FAIL $fail ==" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }
