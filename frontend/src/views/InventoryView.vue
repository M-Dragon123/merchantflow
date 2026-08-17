<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
interface Stock {
  skuId: number
  skuCode: string
  availableQty: number
  lockedQty: number
  safetyStock: number
  version: number
}
interface Tx {
  id: number
  skuId: number
  type: string
  delta: number
  beforeQty: number
  afterQty: number
  reason: string
  operator: string
  createdAt: string
}
const stocks = ref<Stock[]>([])
const transactions = ref<Tx[]>([])
const loading = ref(true)
const dialog = ref(false)
const submitting = ref(false)
const form = reactive({ skuId: 0, delta: 1, type: 'INBOUND', reason: '' })
const alerts = computed(() => stocks.value.filter((s) => s.availableQty <= s.safetyStock))
async function load() {
  loading.value = true
  try {
    ;[stocks.value, transactions.value] = await Promise.all([
      api<Stock[]>('/api/v1/inventory'),
      api<Tx[]>('/api/v1/inventory/transactions'),
    ])
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '库存加载失败')
  } finally {
    loading.value = false
  }
}
function openAdjust(row: Stock) {
  form.skuId = row.skuId
  form.delta = 1
  form.type = 'INBOUND'
  form.reason = ''
  dialog.value = true
}
async function submit() {
  submitting.value = true
  try {
    await api('/api/v1/inventory/adjustments', {
      method: 'POST',
      body: JSON.stringify({ ...form, delta: Number(form.delta) }),
    })
    ElMessage.success('库存已更新并写入流水')
    dialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '库存调整失败')
  } finally {
    submitting.value = false
  }
}
onMounted(load)
</script>
<template>
  <div class="module-page">
    <section class="stock-summary">
      <article>
        <span>可用 SKU</span><strong>{{ stocks.length }}</strong>
      </article>
      <article class="warning">
        <span>低库存预警</span><strong>{{ alerts.length }}</strong>
      </article>
      <article>
        <span>最近流水</span><strong>{{ transactions.length }}</strong>
      </article>
    </section>
    <div class="toolbar">
      <div><strong>库存快照</strong><span> 低于或等于安全库存将标记预警</span></div>
    </div>
    <el-table :data="stocks" v-loading="loading" empty-text="暂无库存数据" class="data-table"
      ><el-table-column prop="skuCode" label="SKU 编码" min-width="150" /><el-table-column
        prop="availableQty"
        label="可用"
        min-width="90"
      /><el-table-column prop="lockedQty" label="锁定" min-width="90" /><el-table-column
        prop="safetyStock"
        label="安全库存"
        min-width="110"
      /><el-table-column label="库存状态" min-width="110"
        ><template #default="{ row }"
          ><el-tag :type="row.availableQty <= row.safetyStock ? 'danger' : 'success'">{{
            row.availableQty <= row.safetyStock ? '需关注' : '正常'
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" width="110"
        ><template #default="{ row }"
          ><el-button type="primary" text @click="openAdjust(row)">调整</el-button></template
        ></el-table-column
      ></el-table
    >
    <section class="recent">
      <h2>最近库存流水</h2>
      <el-table :data="transactions.slice(0, 5)" size="small" empty-text="暂无流水"
        ><el-table-column prop="skuId" label="SKU" /><el-table-column
          prop="type"
          label="类型" /><el-table-column label="变动"
          ><template #default="{ row }"
            ><span :class="row.delta >= 0 ? 'positive' : 'negative'"
              >{{ row.delta >= 0 ? '+' : '' }}{{ row.delta }}</span
            ></template
          ></el-table-column
        ><el-table-column prop="afterQty" label="调整后" /><el-table-column
          prop="operator"
          label="操作人" /><el-table-column prop="reason" label="说明"
      /></el-table>
    </section>
    <el-dialog v-model="dialog" title="库存调整" width="min(92vw, 440px)"
      ><el-form label-position="top"
        ><el-form-item label="库存类型"
          ><el-select v-model="form.type"
            ><el-option label="入库" value="INBOUND" /><el-option
              label="出库"
              value="OUTBOUND" /><el-option
              label="调整"
              value="ADJUSTMENT" /></el-select></el-form-item
        ><el-form-item label="调整数量（出库请填负数）"
          ><el-input-number v-model="form.delta" :min="-999999" :max="999999" /></el-form-item
        ><el-form-item label="说明"
          ><el-input
            v-model="form.reason"
            maxlength="100"
            show-word-limit /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="submitting" @click="submit"
          >确认调整</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<style scoped>
.stock-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 25px;
}
.stock-summary article {
  padding: 17px 20px;
  border-left: 4px solid #2563eb;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 18px rgb(30 58 138 / 5%);
}
.stock-summary article.warning {
  border-left-color: #f97316;
}
.stock-summary span,
.toolbar span {
  color: #64748b;
  font-size: 0.82rem;
}
.stock-summary strong {
  display: block;
  margin-top: 7px;
  font-size: 1.75rem;
}
.toolbar {
  margin-bottom: 16px;
}
.data-table {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
.recent {
  margin-top: 30px;
}
.recent h2 {
  margin: 0 0 12px;
  font-size: 1rem;
}
.positive {
  color: #15803d;
  font-weight: 600;
}
.negative {
  color: #dc2626;
  font-weight: 600;
}
@media (max-width: 560px) {
  .stock-summary {
    gap: 8px;
  }
  .stock-summary article {
    padding: 12px;
  }
  .stock-summary strong {
    font-size: 1.3rem;
  }
}
</style>
