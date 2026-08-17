<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Van } from '@element-plus/icons-vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import ScanInput from '@/components/ScanInput.vue'

interface Order {
  id: number
  orderNo: string
  customerName: string
  status: string
  totalAmount: number
  createdAt: string
}
interface Summary {
  todayOrders: number
  todaySales: number
  pendingShipment: number
  lowStock: number
}

const auth = useAuthStore()
const canShip = () => auth.hasRole('ADMIN', 'WAREHOUSE')

const summary = ref<Summary | null>(null)
const pending = ref<Order[]>([])
const loading = ref(true)
const shippingId = ref(0)

const scanned = ref<Order | null>(null)
const scanLoading = ref(false)

const pendingCount = computed(() => summary.value?.pendingShipment ?? pending.value.length)

async function load() {
  loading.value = true
  try {
    const [s, list] = await Promise.all([
      api<Summary>('/api/v1/dashboard/summary'),
      api<{ items: Order[]; total: number }>('/api/v1/orders?status=PENDING_SHIPMENT&page=1&size=50'),
    ])
    summary.value = s
    pending.value = list.items
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '待处理数据加载失败')
  } finally {
    loading.value = false
  }
}

async function onScan(orderNo: string) {
  scanLoading.value = true
  scanned.value = null
  try {
    const detail = await api<{ order: Order; items: unknown[] }>(`/api/v1/orders/by-no/${orderNo}`)
    scanned.value = detail.order
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '未找到该订单')
  } finally {
    scanLoading.value = false
  }
}

async function ship(order: Order) {
  try {
    await ElMessageBox.confirm(`确认发货 ${order.orderNo}？`, '请确认操作', {
      type: 'warning',
      confirmButtonText: '确认发货',
      cancelButtonText: '返回',
    })
    shippingId.value = order.id
    await api(`/api/v1/orders/${order.id}/ship`, {
      method: 'POST',
      body: JSON.stringify({ remark: '仓库模式发货' }),
    })
    ElMessage.success('已发货')
    scanned.value = null
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '发货失败')
  } finally {
    shippingId.value = 0
  }
}

function fmtMoney(value: number) {
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}
function fmtTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(5, 16) : '—'
}

onMounted(load)
</script>

<template>
  <div class="todo-page" v-loading="loading">
    <section class="hero-card">
      <div class="hero-icon"><el-icon><Van /></el-icon></div>
      <div>
        <p class="hero-label">待发货订单</p>
        <strong class="hero-num">{{ pendingCount }}</strong>
        <p class="hero-hint">未发货订单合计 · 今日同步</p>
      </div>
    </section>

    <section class="scan-block">
      <h2>扫码 / 手动查单</h2>
      <ScanInput :loading="scanLoading" @submit="onScan" />
      <div v-if="scanned" class="scanned-card">
        <div class="scanned-line">
          <strong>{{ scanned.orderNo }}</strong>
          <el-tag type="primary" effect="plain">待发货</el-tag>
        </div>
        <p>{{ scanned.customerName }} · {{ fmtMoney(scanned.totalAmount) }}</p>
        <div class="action-row">
          <el-button size="large" @click="scanned = null">关闭</el-button>
          <el-button
            v-if="scanned.status === 'PENDING_SHIPMENT' && canShip()"
            size="large"
            type="primary"
            :loading="shippingId === scanned.id"
            class="big-btn"
            @click="ship(scanned)"
            >立即发货</el-button
          >
          <el-tag v-else-if="scanned.status !== 'PENDING_SHIPMENT'" type="info">{{ scanned.status }}</el-tag>
        </div>
      </div>
    </section>

    <section class="list-block">
      <header class="list-head">
        <h2>待发货列表</h2>
        <span>{{ pending.length }} 笔</span>
      </header>
      <ul v-if="pending.length" class="order-cards">
        <li v-for="order in pending" :key="order.id" class="order-card">
          <div class="card-main">
            <strong class="order-no">{{ order.orderNo }}</strong>
            <p class="order-meta">{{ order.customerName }} · {{ fmtTime(order.createdAt) }}</p>
            <span class="order-amt">{{ fmtMoney(order.totalAmount) }}</span>
          </div>
          <button
            v-if="canShip()"
            type="button"
            class="ship-btn"
            :disabled="shippingId === order.id"
            @click="ship(order)"
          >
            {{ shippingId === order.id ? '发货中…' : '发货' }}
          </button>
          <el-tag v-else type="info" effect="plain">只读</el-tag>
        </li>
      </ul>
      <el-empty v-else description="暂无待发货订单" :image-size="72" />
    </section>
  </div>
</template>

<style scoped>
.todo-page {
  display: grid;
  gap: 18px;
}
.hero-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #172554, #1e3a8a);
  border-radius: 14px;
  color: #fff;
}
.hero-icon {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border-radius: 14px;
  background: rgb(147 197 253 / 20%);
  color: #93c5fd;
  font-size: 1.7rem;
}
.hero-label,
.hero-hint {
  margin: 0;
  color: #cbd5e1;
  font-size: 0.8rem;
}
.hero-num {
  display: block;
  margin: 2px 0;
  font-size: 2.1rem;
  line-height: 1.1;
}
.scan-block h2,
.list-head h2 {
  margin: 0;
  font-size: 0.98rem;
}
.scan-block {
  display: grid;
  gap: 12px;
}
.scanned-card {
  padding: 14px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
}
.scanned-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.scanned-card p {
  margin: 6px 0 12px;
  color: #475569;
  font-size: 0.9rem;
}
.action-row {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.list-head span {
  color: #64748b;
  font-size: 0.8rem;
}
.order-cards {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.order-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
}
.card-main {
  flex: 1;
  min-width: 0;
}
.order-no {
  display: block;
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-meta {
  margin: 4px 0 2px;
  color: #64748b;
  font-size: 0.78rem;
}
.order-amt {
  color: #1d4ed8;
  font-weight: 700;
  font-size: 0.92rem;
}
.ship-btn {
  flex-shrink: 0;
  min-width: 92px;
  min-height: 48px;
  border: 0;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 700;
}
.ship-btn:disabled {
  background: #93b4f2;
}
.big-btn {
  min-width: 130px;
}
</style>
