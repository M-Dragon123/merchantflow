<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'

interface CustomerItem {
  id: number
  name: string
  mobile: string
  orderCount: number
}
interface OrderLine {
  id: number
  orderNo: string
  status: string
  totalAmount: number
  createdAt: string
}
interface CustomerDetail {
  id: number
  name: string
  mobile: string
  recentOrders: OrderLine[]
}

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: '待付款',
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}

const customers = ref<CustomerItem[]>([])
const loading = ref(true)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<CustomerDetail | null>(null)

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(size.value) })
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const result = await api<{ items: CustomerItem[]; total: number }>(`/api/v1/customers?${params.toString()}`)
    customers.value = result.items
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '客户列表加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

// 关键字模糊查询：输入停顿 400ms 后自动搜索（回车亦可立即触发）
let keywordTimer: ReturnType<typeof setTimeout> | undefined
watch(keyword, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => search(), 400)
})

async function openDetail(row: CustomerItem) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api<CustomerDetail>(`/api/v1/customers/${row.id}`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '客户详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function fmtMoney(value: number) {
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}
function fmtTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}
function statusType(value: string) {
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

onMounted(load)
</script>

<template>
  <div class="module-page">
    <div class="toolbar">
      <div>
        <strong>客户管理</strong>
        <span> 客户来自下单时自动建档，可查看客户最近订单</span>
      </div>
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="客户姓名 / 手机号"
          clearable
          class="keyword"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <el-table :data="customers" v-loading="loading" empty-text="暂无客户" class="data-table">
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column prop="mobile" label="手机号" min-width="140" />
      <el-table-column prop="orderCount" label="订单数" min-width="90" />
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button text type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="() => { page = 1; load() }"
      />
    </div>

    <el-drawer v-model="detailOpen" title="客户详情" size="min(94vw, 560px)">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.mobile }}</el-descriptions-item>
          </el-descriptions>

          <h3 class="section-title">最近订单（最多 20 笔）</h3>
          <el-table :data="detail.recentOrders" size="small" empty-text="暂无订单">
            <el-table-column prop="orderNo" label="订单号" min-width="180" />
            <el-table-column label="金额" min-width="100">
              <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.status)">{{ STATUS_LABELS[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="150">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
.toolbar strong {
  font-size: 1.05rem;
}
.toolbar span {
  color: #64748b;
  font-size: 0.82rem;
}
.search-box {
  display: flex;
  gap: 10px;
}
.keyword {
  width: 220px;
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
.detail-body {
  min-height: 160px;
}
.section-title {
  margin: 20px 0 10px;
  font-size: 0.95rem;
}
</style>
