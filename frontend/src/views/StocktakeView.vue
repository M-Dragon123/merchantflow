<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/client'

interface StocktakeItemView {
  id: number
  stocktakeNo: string
  status: string
  operatorName: string
  createdAt: string
}
interface Line {
  skuId: number
  skuCode: string
  systemQty: number
  countedQty: number
  diffQty: number
}
interface StocktakeDetail {
  stocktake: StocktakeItemView
  items: Line[]
}

const statusLabel: Record<string, string> = { DRAFT: '盘点中', COMPLETED: '已完成', CANCELLED: '已取消' }

const list = ref<StocktakeItemView[]>([])
const loading = ref(true)
const creating = ref(false)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<StocktakeDetail | null>(null)

async function load() {
  loading.value = true
  try {
    list.value = await api<StocktakeItemView[]>('/api/v1/stocktakes')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '盘点单加载失败')
  } finally {
    loading.value = false
  }
}

async function createStocktake() {
  creating.value = true
  try {
    await ElMessageBox.confirm('将按当前库存为全部商品创建盘点草稿，继续？', '发起盘点', {
      type: 'info',
      confirmButtonText: '创建',
      cancelButtonText: '返回',
    })
    const created = await api<StocktakeItemView>('/api/v1/stocktakes', { method: 'POST' })
    ElMessage.success(`盘点单 ${created.stocktakeNo} 已创建`)
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    creating.value = false
  }
}

async function openDetail(row: StocktakeItemView) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await api<StocktakeDetail>(`/api/v1/stocktakes/${row.id}`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '盘点单加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function updateCounted(line: Line) {
  if (!detail.value || detail.value.stocktake.status !== 'DRAFT') return
  try {
    await api(`/api/v1/stocktakes/${detail.value.stocktake.id}/items/${line.skuId}`, {
      method: 'PUT',
      body: JSON.stringify({ skuId: line.skuId, countedQty: line.countedQty }),
    })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '录入失败')
    await openDetail(detail.value.stocktake)
  }
}

async function completeStocktake() {
  if (!detail.value) return
  const totalDiff = detail.value.items.reduce((sum, item) => sum + item.diffQty, 0)
  const sign = totalDiff > 0 ? '盘盈' : totalDiff < 0 ? '盘亏' : '无差异'
  try {
    await ElMessageBox.confirm(
      `完成盘点后，差异将写入库存流水（合计 ${totalDiff > 0 ? '+' : ''}${totalDiff}，${sign}），且不可撤销。确认完成？`,
      '完成盘点',
      { type: 'warning', confirmButtonText: '确认完成', cancelButtonText: '返回' },
    )
    await api(`/api/v1/stocktakes/${detail.value.stocktake.id}/complete`, { method: 'POST' })
    ElMessage.success('盘点完成，差异已写入库存流水')
    detailOpen.value = false
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function cancelStocktake() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('取消后该盘点单作废，确认取消？', '取消盘点', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
    })
    await api(`/api/v1/stocktakes/${detail.value.stocktake.id}/cancel`, { method: 'POST' })
    ElMessage.success('盘点单已取消')
    detailOpen.value = false
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

function fmtTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 19) : '—'
}

onMounted(load)
</script>

<template>
  <div class="module-page">
    <div class="toolbar">
      <div>
        <strong>库存盘点</strong>
        <span> 盘点差异在完成时统一写入库存流水，全程可追溯</span>
      </div>
      <el-button type="primary" :loading="creating" @click="createStocktake">发起盘点</el-button>
    </div>

    <el-table :data="list" v-loading="loading" empty-text="暂无盘点单" class="data-table">
      <el-table-column prop="stocktakeNo" label="盘点单号" min-width="170" />
      <el-table-column label="状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'CANCELLED' ? 'info' : 'primary'">
            {{ statusLabel[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="发起人" min-width="110" />
      <el-table-column label="发起时间" min-width="170">
        <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button text type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailOpen" title="盘点明细" width="min(94vw, 760px)">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="3" border size="small" class="detail-desc">
            <el-descriptions-item label="盘点单号">{{ detail.stocktake.stocktakeNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="detail.stocktake.status === 'COMPLETED' ? 'success' : detail.stocktake.status === 'CANCELLED' ? 'info' : 'primary'">
                {{ statusLabel[detail.stocktake.status] }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="发起人">{{ detail.stocktake.operatorName }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.items" size="small" empty-text="无盘点明细">
            <el-table-column prop="skuCode" label="SKU 编码" min-width="140" />
            <el-table-column prop="systemQty" label="账面库存" width="100" />
            <el-table-column label="实盘数量" width="150">
              <template #default="{ row }">
                <el-input-number
                  v-if="detail.stocktake.status === 'DRAFT'"
                  v-model="row.countedQty"
                  :min="0"
                  size="small"
                  controls-position="right"
                  @change="updateCounted(row)"
                />
                <span v-else>{{ row.countedQty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="差异" width="100">
              <template #default="{ row }">
                <span :class="row.diffQty > 0 ? 'positive' : row.diffQty < 0 ? 'negative' : 'zero'">
                  {{ row.diffQty > 0 ? '+' : '' }}{{ row.diffQty }}
                </span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="detail.stocktake.status === 'DRAFT'" class="action-row">
            <el-button @click="cancelStocktake">取消盘点</el-button>
            <el-button type="primary" @click="completeStocktake">完成盘点</el-button>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar strong {
  font-size: 1.05rem;
}
.toolbar span {
  color: #64748b;
  font-size: 0.82rem;
}
.data-table {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
.detail-body {
  min-height: 160px;
}
.detail-desc {
  margin-bottom: 14px;
}
.positive {
  color: #15803d;
  font-weight: 700;
}
.negative {
  color: #dc2626;
  font-weight: 700;
}
.zero {
  color: #94a3b8;
}
.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>
