<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import ScanInput from '@/components/ScanInput.vue'

type Status =
  | 'PENDING_PAYMENT'
  | 'PENDING_SHIPMENT'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDING'
  | 'REFUNDED'
interface Order {
  id: number
  orderNo: string
  customerName: string
  status: Status
  totalAmount: number
  createdAt: string
  paidAt: string | null
}
interface OrderDetail {
  order: Order
  items: { skuId: number; skuCode: string; quantity: number; unitPrice: number; subtotalAmount: number }[]
}
interface LogItem {
  action: string
  fromStatus: string
  toStatus: string
  remark: string
  operatorName: string
  createdAt: string
}

const labels: Record<Status, string> = {
  PENDING_PAYMENT: '待付款',
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}
const chips: { value: Status | ''; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'PENDING_SHIPMENT', label: '待发货' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REFUNDING', label: '退款中' },
]
const actionNames: Record<string, string> = {
  CREATE: '创建订单',
  PAY: '确认付款',
  SHIP: '发货',
  COMPLETE: '完成订单',
  CANCEL: '取消订单',
  REFUND_REQUEST: '发起退款',
  REFUND_COMPLETE: '退款完成',
}

const auth = useAuthStore()
const canManageOrder = () => auth.hasRole('ADMIN', 'OPERATOR')
const canShip = () => auth.hasRole('ADMIN', 'WAREHOUSE')
const canRefundComplete = () => auth.hasRole('ADMIN')

const orders = ref<Order[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const total = ref(0)
const page = ref(1)
const size = 20
const status = ref<Status | ''>('')
const keyword = ref('')

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<OrderDetail | null>(null)
const logs = ref<LogItem[]>([])
const busyId = ref(0)

async function load(reset = true) {
  if (reset) {
    page.value = 1
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(size) })
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    if (status.value) params.set('status', status.value)
    const result = await api<{ items: Order[]; total: number }>(`/api/v1/orders?${params.toString()}`)
    orders.value = reset ? result.items : [...orders.value, ...result.items]
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '订单加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (orders.value.length >= total.value) return
  page.value += 1
  await load(false)
}

function pickStatus(value: Status | '') {
  status.value = value
  load()
}

function onKeyword(value: string) {
  keyword.value = value
  load()
}

async function openDetail(order: Order) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  logs.value = []
  try {
    const [d, l] = await Promise.all([
      api<OrderDetail>(`/api/v1/orders/${order.id}`),
      api<LogItem[]>(`/api/v1/orders/${order.id}/logs`),
    ])
    detail.value = d
    logs.value = l
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '订单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function action(order: Order, path: string, text: string) {
  try {
    await ElMessageBox.confirm(`确认${text} ${order.orderNo}？`, '请确认操作', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '返回',
    })
    busyId.value = order.id
    await api(`/api/v1/orders/${order.id}/${path}`, {
      method: 'POST',
      body: JSON.stringify({ remark: `移动端${text}` }),
    })
    ElMessage.success(`已${text}`)
    detailOpen.value = false
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    busyId.value = 0
  }
}

function statusType(value: Status) {
  return value === 'PENDING_PAYMENT'
    ? 'warning'
    : value === 'PENDING_SHIPMENT'
      ? 'primary'
      : value === 'SHIPPED' || value === 'COMPLETED'
        ? 'success'
        : value === 'CANCELLED' || value === 'REFUNDED'
          ? 'info'
          : 'danger'
}

function fmtMoney(value: number) {
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}
function fmtTime(iso: string | null) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(() => load())
</script>

<template>
  <div class="order-page">
    <section class="toolbar">
      <ScanInput placeholder="订单号 / 客户，回车查询" @submit="onKeyword" />
      <div class="chips">
        <button
          v-for="chip in chips"
          :key="chip.value"
          type="button"
          class="chip"
          :class="{ active: status === chip.value }"
          @click="pickStatus(chip.value)"
        >
          {{ chip.label }}
        </button>
      </div>
    </section>

    <section v-loading="loading">
      <ul v-if="orders.length" class="order-cards">
        <li v-for="order in orders" :key="order.id" class="order-card" @click="openDetail(order)">
          <div class="card-top">
            <strong class="order-no">{{ order.orderNo }}</strong>
            <el-tag :type="statusType(order.status)" effect="plain">{{ labels[order.status] }}</el-tag>
          </div>
          <p class="order-meta">
            {{ order.customerName || '—' }} · {{ fmtTime(order.createdAt) }}<span v-if="order.paidAt"> · 已付款</span>
          </p>
          <div class="card-bottom">
            <span class="order-amt">{{ fmtMoney(order.totalAmount) }}</span>
            <span class="tap-hint">点按查看详情 ›</span>
          </div>
        </li>
      </ul>
      <el-empty v-else description="暂无订单" :image-size="72" />

      <button
        v-if="orders.length && orders.length < total"
        type="button"
        class="load-more"
        :disabled="loadingMore"
        @click="loadMore"
      >
        {{ loadingMore ? '加载中…' : `加载更多（${orders.length}/${total}）` }}
      </button>
    </section>

    <el-dialog v-model="detailOpen" title="订单详情" width="min(94vw, 480px)" class="detail-dialog">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <strong>{{ detail.order.orderNo }}</strong>
            <el-tag :type="statusType(detail.order.status)">{{ labels[detail.order.status] }}</el-tag>
          </div>
          <p class="detail-meta">
            {{ detail.order.customerName || '—' }} · 创建 {{ fmtTime(detail.order.createdAt) }}<br />
            付款 {{ fmtTime(detail.order.paidAt) }} · 金额 <b>{{ fmtMoney(detail.order.totalAmount) }}</b>
          </p>

          <h3 class="section-title">商品明细</h3>
          <ul class="item-list">
            <li v-for="item in detail.items" :key="item.skuId">
              <span>{{ item.skuCode }}</span><span>×{{ item.quantity }}</span
              ><b>{{ fmtMoney(item.subtotalAmount) }}</b>
            </li>
          </ul>

          <h3 class="section-title">操作记录</h3>
          <ul v-if="logs.length" class="log-list">
            <li v-for="log in logs" :key="log.createdAt + log.action">
              <b>{{ actionNames[log.action] || log.action }}</b>
              <span class="log-status"
                >{{ labels[log.fromStatus as Status] || log.fromStatus || '—' }} → {{ labels[log.toStatus as Status] || log.toStatus }}</span
              >
              <p>{{ log.remark }}</p>
              <small>{{ fmtTime(log.createdAt) }} · {{ log.operatorName }}</small>
            </li>
          </ul>
          <p v-else class="no-log">暂无操作记录</p>

          <div class="action-row">
            <el-button v-if="detail.order.status === 'PENDING_PAYMENT' && canManageOrder()" size="large" type="primary" @click="action(detail.order, 'pay', '确认付款')">确认付款</el-button>
            <el-button v-if="detail.order.status === 'PENDING_PAYMENT' && canManageOrder()" size="large" type="danger" plain @click="action(detail.order, 'cancel', '取消订单')">取消订单</el-button>
            <el-button v-if="detail.order.status === 'PENDING_SHIPMENT' && canShip()" size="large" type="primary" :loading="busyId === detail.order.id" @click="action(detail.order, 'ship', '发货')">发货</el-button>
            <el-button v-if="detail.order.status === 'SHIPPED' && canManageOrder()" size="large" type="success" plain @click="action(detail.order, 'complete', '完成')">完成订单</el-button>
            <el-button v-if="detail.order.status === 'SHIPPED' && canManageOrder()" size="large" type="warning" plain @click="action(detail.order, 'refund', '发起退款')">发起退款</el-button>
            <el-button v-if="detail.order.status === 'REFUNDING' && canRefundComplete()" size="large" type="warning" @click="action(detail.order, 'refund/complete', '退款完成')">退款完成</el-button>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-page {
  display: grid;
  gap: 14px;
}
.toolbar {
  display: grid;
  gap: 10px;
}
.chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.chip {
  flex-shrink: 0;
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 0.86rem;
}
.chip.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}
.order-cards {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.order-card {
  padding: 14px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  cursor: pointer;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.order-no {
  font-size: 0.93rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-meta {
  margin: 6px 0 10px;
  color: #64748b;
  font-size: 0.8rem;
}
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-amt {
  color: #1d4ed8;
  font-weight: 700;
  font-size: 1.05rem;
}
.tap-hint {
  color: #94a3b8;
  font-size: 0.76rem;
}
.load-more {
  display: block;
  width: 100%;
  min-height: 46px;
  margin-top: 12px;
  border: 1px solid #dbe4f0;
  border-radius: 10px;
  background: #fff;
  color: #2563eb;
  font-size: 0.9rem;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.detail-meta {
  margin: 10px 0 0;
  color: #475569;
  font-size: 0.86rem;
  line-height: 1.7;
}
.section-title {
  margin: 18px 0 8px;
  font-size: 0.92rem;
}
.item-list,
.log-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}
.item-list li {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #334155;
}
.log-list li {
  padding: 10px 12px;
  border-left: 3px solid #bfdbfe;
  background: #f8fafc;
  border-radius: 0 8px 8px 0;
  font-size: 0.82rem;
}
.log-status {
  display: block;
  margin: 3px 0;
  color: #64748b;
}
.log-list p {
  margin: 3px 0;
  color: #475569;
}
.log-list small {
  color: #94a3b8;
}
.no-log {
  color: #94a3b8;
  font-size: 0.84rem;
}
.action-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
  justify-content: flex-end;
}
</style>
