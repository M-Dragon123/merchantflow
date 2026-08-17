<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart, Money, Van, WarningFilled, Refresh } from '@element-plus/icons-vue'
import { api } from '@/api/client'
import TrendChart from '@/components/TrendChart.vue'

interface Summary {
  todayOrders: number
  todaySales: number
  pendingShipment: number
  lowStock: number
}
interface TrendPoint {
  date: string
  amount: number
}
interface TopProduct {
  skuId: number
  skuCode: string
  quantity: number
  amount: number
}
interface OverdueOrder {
  id: number
  orderNo: string
  totalAmount: number
  createdAt: string
}
interface Anomalies {
  overduePaymentOrders: OverdueOrder[]
  refundingCount: number
}

const router = useRouter()
const loading = ref(true)
const error = ref('')
const summary = ref<Summary | null>(null)
const trend = ref<TrendPoint[]>([])
const top = ref<TopProduct[]>([])
const anomalies = ref<Anomalies | null>(null)
const trendDays = ref(14)

const cards = computed(() => [
  {
    label: '今日订单数',
    value: summary.value ? String(summary.value.todayOrders) : '—',
    hint: '今日 00:00 起创建',
    icon: ShoppingCart,
    tone: 'blue',
    to: '/orders',
  },
  {
    label: '今日销售额',
    value: summary.value
      ? `¥${Number(summary.value.todaySales).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
      : '—',
    hint: '已付款 · 不含退款',
    icon: Money,
    tone: 'green',
    to: '/orders',
  },
  {
    label: '待发货',
    value: summary.value ? String(summary.value.pendingShipment) : '—',
    hint: '等待仓库发货',
    icon: Van,
    tone: 'indigo',
    to: '/orders',
  },
  {
    label: '库存预警',
    value: summary.value ? String(summary.value.lowStock) : '—',
    hint: '可用 ≤ 安全库存',
    icon: WarningFilled,
    tone: 'orange',
    to: '/inventory',
  },
])

const topMax = computed(() => Math.max(...top.value.map((t) => t.quantity), 1))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [s, t, p, a] = await Promise.all([
      api<Summary>('/api/v1/dashboard/summary'),
      api<TrendPoint[]>(`/api/v1/dashboard/sales-trend?days=${trendDays.value}`),
      api<TopProduct[]>('/api/v1/dashboard/top-products?days=30'),
      api<Anomalies>('/api/v1/dashboard/anomalies'),
    ])
    summary.value = s
    trend.value = t
    top.value = p
    anomalies.value = a
  } catch (e) {
    error.value = e instanceof Error ? e.message : '工作台数据加载失败'
  } finally {
    loading.value = false
  }
}

function switchDays(days: number) {
  if (days === trendDays.value) return
  trendDays.value = days
  load()
}

function fmtMoney(value: number) {
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function fmtTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(load)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <el-alert v-if="error" type="error" :closable="false" show-icon class="error-banner">
      <template #title>
        <span>{{ error }}</span>
        <el-button size="small" text type="primary" :icon="Refresh" @click="load">重试</el-button>
      </template>
    </el-alert>

    <section class="stat-grid">
      <article
        v-for="card in cards"
        :key="card.label"
        class="stat-card"
        :class="`tone-${card.tone}`"
        @click="router.push(card.to)"
      >
        <div class="stat-icon"><el-icon><component :is="card.icon" /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">{{ card.label }}</span>
          <strong class="stat-value">{{ card.value }}</strong>
          <span class="stat-hint">{{ card.hint }}</span>
        </div>
      </article>
    </section>

    <section class="charts-row">
      <article class="panel trend-panel">
        <header class="panel-head">
          <div>
            <h2>销售趋势</h2>
            <p>按付款时间统计 · 排除已取消/已退款</p>
          </div>
          <el-radio-group size="small" :model-value="trendDays" @change="switchDays">
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="14">14天</el-radio-button>
            <el-radio-button :value="30">30天</el-radio-button>
          </el-radio-group>
        </header>
        <TrendChart v-if="trend.length" :points="trend" />
        <el-empty v-else description="所选区间暂无已支付订单" :image-size="72" />
      </article>

      <article class="panel top-panel">
        <header class="panel-head">
          <div>
            <h2>热销商品 TOP10</h2>
            <p>近 30 天已支付订单销量</p>
          </div>
        </header>
        <ul v-if="top.length" class="top-list">
          <li v-for="(item, index) in top" :key="item.skuId">
            <span class="rank" :class="{ hot: index < 3 }">{{ index + 1 }}</span>
            <span class="sku">{{ item.skuCode }}</span>
            <span class="bar-wrap"><i :style="{ width: `${(item.quantity / topMax) * 100}%` }" /></span>
            <span class="qty">{{ item.quantity }} 件</span>
            <span class="amt">{{ fmtMoney(item.amount) }}</span>
          </li>
        </ul>
        <el-empty v-else description="暂无销量数据" :image-size="72" />
      </article>
    </section>

    <section class="panel anomaly-panel">
      <header class="panel-head">
        <div>
          <h2>异常订单提醒</h2>
          <p>待付款超过 24 小时未支付 · 退款中订单</p>
        </div>
        <el-tag v-if="anomalies && anomalies.refundingCount" type="danger" effect="plain"
          >退款中 {{ anomalies.refundingCount }} 笔</el-tag
        >
      </header>
      <div v-if="anomalies && anomalies.overduePaymentOrders.length" class="anomaly-list">
        <el-table :data="anomalies.overduePaymentOrders" size="small">
          <el-table-column prop="orderNo" label="订单号" min-width="190" />
          <el-table-column label="金额" min-width="110">
            <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="160">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button
                text
                type="primary"
                @click="router.push({ path: '/orders', query: { keyword: row.orderNo } })"
                >查看</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else-if="anomalies" description="暂无超时未支付订单，一切正常" :image-size="64" />
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 22px;
}
.error-banner {
  margin-bottom: 2px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.stat-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}
.stat-card:hover {
  box-shadow: 0 10px 26px rgb(30 58 138 / 8%);
  transform: translateY(-2px);
}
.stat-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 12px;
  font-size: 1.35rem;
}
.tone-blue .stat-icon { color: #1d4ed8; background: #dbeafe; }
.tone-green .stat-icon { color: #15803d; background: #dcfce7; }
.tone-indigo .stat-icon { color: #4338ca; background: #e0e7ff; }
.tone-orange .stat-icon { color: #c2410c; background: #ffedd5; }
.stat-label,
.stat-hint {
  display: block;
  color: #64748b;
  font-size: 0.78rem;
}
.stat-value {
  display: block;
  margin: 3px 0 1px;
  font-size: 1.55rem;
  color: #172554;
}
.charts-row {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 14px;
  align-items: start;
}
.panel {
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}
.panel-head h2 {
  margin: 0;
  font-size: 1rem;
}
.panel-head p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 0.75rem;
}
.top-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 9px;
}
.top-list li {
  display: grid;
  grid-template-columns: 26px 1.1fr 1.6fr 62px 88px;
  gap: 10px;
  align-items: center;
  font-size: 0.82rem;
}
.rank {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
  font-weight: 600;
}
.rank.hot {
  background: #1e3a8a;
  color: #fff;
}
.sku {
  color: #334155;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar-wrap {
  height: 8px;
  background: #eef2f8;
  border-radius: 999px;
  overflow: hidden;
}
.bar-wrap i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #2563eb);
  border-radius: 999px;
}
.qty,
.amt {
  color: #64748b;
  text-align: right;
}
.anomaly-list {
  margin-top: 2px;
}
@media (max-width: 1080px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 560px) {
  .stat-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  .stat-card {
    padding: 12px;
    gap: 10px;
  }
  .stat-icon {
    width: 38px;
    height: 38px;
  }
  .stat-value {
    font-size: 1.2rem;
  }
  .top-list li {
    grid-template-columns: 22px 1fr 1.4fr 52px;
  }
  .top-list .amt {
    display: none;
  }
}
</style>
