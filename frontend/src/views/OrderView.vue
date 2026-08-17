<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

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
  customerId: number
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
interface Product {
  skuId: number
  skuCode: string
  salePrice: number
  availableQty: number
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
const statusOptions = (Object.keys(labels) as Status[]).map((value) => ({ value, label: labels[value] }))
const actionNames: Record<string, string> = {
  CREATE: '创建订单',
  PAY: '确认付款',
  SHIP: '发货',
  COMPLETE: '完成订单',
  CANCEL: '取消订单',
  REFUND_REQUEST: '发起退款',
  REFUND_COMPLETE: '退款完成',
}

const route = useRoute()
const auth = useAuthStore()
const canManageOrder = () => auth.hasRole('ADMIN', 'OPERATOR')
const canShip = () => auth.hasRole('ADMIN', 'WAREHOUSE')
const canRefundComplete = () => auth.hasRole('ADMIN')

const orders = ref<Order[]>([])
const products = ref<Product[]>([])
const loading = ref(true)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filters = reactive({
  keyword: '',
  status: '' as Status | '',
  dateRange: [] as string[],
})
const createOpen = ref(false)
const submitting = ref(false)
const form = reactive({ customerName: '', customerMobile: '', skuId: 0, quantity: 1 })
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<OrderDetail | null>(null)
const logs = ref<LogItem[]>([])

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(size.value) })
    if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim())
    if (filters.status) params.set('status', filters.status)
    if (filters.dateRange.length === 2) {
      params.set('dateFrom', filters.dateRange[0])
      params.set('dateTo', filters.dateRange[1])
    }
    const result = await api<{ items: Order[]; total: number; page: number; size: number }>(
      `/api/v1/orders?${params.toString()}`,
    )
    orders.value = result.items
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function ensureProducts() {
  try {
    products.value = await api<Product[]>('/api/v1/products')
    if (!form.skuId && products.value.length) form.skuId = products.value[0].skuId
  } catch {
    /* 仅用于创建订单，失败不阻塞列表 */
  }
}

function search() {
  page.value = 1
  load()
}

// 关键字模糊查询：输入停顿 400ms 后自动搜索（回车亦可立即触发）
let keywordTimer: ReturnType<typeof setTimeout> | undefined
watch(
  () => filters.keyword,
  () => {
    if (keywordTimer) clearTimeout(keywordTimer)
    keywordTimer = setTimeout(() => search(), 400)
  },
)

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.dateRange = []
  page.value = 1
  load()
}

async function create() {
  if (!form.customerName || !form.customerMobile || !form.skuId) {
    ElMessage.warning('请填写客户与商品信息')
    return
  }
  submitting.value = true
  try {
    await api('/api/v1/orders', {
      method: 'POST',
      body: JSON.stringify({
        customerName: form.customerName,
        customerMobile: form.customerMobile,
        items: [{ skuId: form.skuId, quantity: form.quantity }],
      }),
    })
    ElMessage.success('订单已创建，库存已锁定')
    createOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '建单失败')
  } finally {
    submitting.value = false
  }
}

async function action(order: Order, path: string, text: string) {
  try {
    await ElMessageBox.confirm(`确认要${text}订单 ${order.orderNo} 吗？`, '请确认操作', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '返回',
    })
    await api(`/api/v1/orders/${order.id}/${path}`, {
      method: 'POST',
      body: JSON.stringify({ remark: `人工${text}` }),
    })
    ElMessage.success(`订单已${text}`)
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
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

function resetForm() {
  form.customerName = ''
  form.customerMobile = ''
  form.quantity = 1
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
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function fmtTime(iso: string | null) {
  return iso ? iso.replace('T', ' ').slice(0, 19) : '—'
}

onMounted(async () => {
  const preset = route.query.keyword
  if (typeof preset === 'string' && preset) filters.keyword = preset
  ensureProducts()
  load()
})
</script>

<template>
  <div class="module-page">
    <section class="filter-bar">
      <el-input
        v-model="filters.keyword"
        class="keyword"
        placeholder="订单号 / 客户姓名 / 手机号"
        clearable
        @keyup.enter="search"
      >
        <template #prefix><span class="search-prefix">搜索</span></template>
      </el-input>
      <el-select
        v-model="filters.status"
        placeholder="全部状态"
        clearable
        class="status-select"
        @change="search"
      >
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        range-separator="至"
        class="date-range"
        @change="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <div class="spacer" />
      <el-button v-if="canManageOrder()" type="primary" plain @click="createOpen = true">创建订单</el-button>
    </section>

    <el-table :data="orders" v-loading="loading" empty-text="暂无符合条件的订单" class="data-table">
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column prop="customerName" label="客户" min-width="110" show-overflow-tooltip />
      <el-table-column label="订单金额" min-width="110">
        <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" min-width="105">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ labels[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="165">
        <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="付款时间" min-width="165">
        <template #default="{ row }">{{ fmtTime(row.paidAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="300" fixed="right">
        <template #default="{ row }">
          <el-button text type="info" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 'PENDING_PAYMENT' && canManageOrder()"
            type="primary"
            text
            @click="action(row, 'pay', '确认付款')"
            >付款</el-button
          >
          <el-button
            v-if="row.status === 'PENDING_PAYMENT' && canManageOrder()"
            type="danger"
            text
            @click="action(row, 'cancel', '取消')"
            >取消</el-button
          >
          <el-button
            v-if="row.status === 'PENDING_SHIPMENT' && canShip()"
            type="primary"
            text
            @click="action(row, 'ship', '发货')"
            >发货</el-button
          >
          <el-button
            v-if="row.status === 'SHIPPED' && canManageOrder()"
            type="success"
            text
            @click="action(row, 'complete', '完成')"
            >完成</el-button
          >
          <el-button
            v-if="row.status === 'SHIPPED' && canManageOrder()"
            type="warning"
            text
            @click="action(row, 'refund', '发起退款')"
            >退款</el-button
          >
          <el-button
            v-if="row.status === 'REFUNDING' && canRefundComplete()"
            type="warning"
            text
            @click="action(row, 'refund/complete', '退款完成')"
            >退款完成</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="() => { page = 1; load() }"
      />
    </div>

    <el-dialog v-model="createOpen" title="创建订单" width="min(94vw, 480px)" @closed="resetForm">
      <el-form label-position="top">
        <el-form-item label="客户姓名">
          <el-input v-model="form.customerName" placeholder="例如：李明" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.customerMobile" placeholder="例如：13800138000" />
        </el-form-item>
        <el-form-item label="商品 SKU">
          <el-select v-model="form.skuId" class="full">
            <el-option
              v-for="sku in products"
              :key="sku.skuId"
              :label="`${sku.skuCode} · 可用 ${sku.availableQty}`"
              :value="sku.skuId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.quantity" :min="1" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="create">创建并锁定库存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="订单详情" size="min(94vw, 640px)">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(detail.order.status)">{{ labels[detail.order.status] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="客户">{{ detail.order.customerName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">{{ fmtMoney(detail.order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ fmtTime(detail.order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="付款时间">{{ fmtTime(detail.order.paidAt) }}</el-descriptions-item>
          </el-descriptions>

          <h3 class="section-title">商品明细</h3>
          <el-table :data="detail.items" size="small" empty-text="无商品明细">
            <el-table-column prop="skuCode" label="SKU" min-width="140" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column label="单价" width="110">
              <template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="120">
              <template #default="{ row }">{{ fmtMoney(row.subtotalAmount) }}</template>
            </el-table-column>
          </el-table>

          <h3 class="section-title">操作记录</h3>
          <el-timeline v-if="logs.length">
            <el-timeline-item
              v-for="log in logs"
              :key="log.createdAt + log.action"
              :timestamp="`${fmtTime(log.createdAt)} · ${log.operatorName}`"
              placement="top"
            >
              <div class="log-line">
                <strong>{{ actionNames[log.action] || log.action }}</strong>
                <span v-if="log.fromStatus" class="log-status"
                  >{{ labels[log.fromStatus as Status] || log.fromStatus }} → {{ labels[log.toStatus as Status] || log.toStatus }}</span
                >
              </div>
              <p class="log-remark">{{ log.remark }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作记录" :image-size="56" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.keyword {
  width: 250px;
}
.search-prefix {
  color: #94a3b8;
  font-size: 0.78rem;
}
.status-select {
  width: 140px;
}
.date-range {
  width: 280px;
}
.spacer {
  flex: 1;
}
.data-table {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.full {
  width: 100%;
}
.detail-body {
  min-height: 200px;
}
.detail-desc {
  margin-bottom: 6px;
}
.section-title {
  margin: 22px 0 10px;
  font-size: 0.95rem;
}
.log-line {
  display: flex;
  gap: 10px;
  align-items: baseline;
}
.log-status {
  color: #64748b;
  font-size: 0.78rem;
}
.log-remark {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 0.78rem;
}
@media (max-width: 760px) {
  .keyword,
  .date-range {
    width: 100%;
  }
  .filter-bar {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .keyword,
  .date-range {
    grid-column: 1 / -1;
  }
}
</style>
