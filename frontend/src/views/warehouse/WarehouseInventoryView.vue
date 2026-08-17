<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

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
  afterQty: number
  operator: string
  reason: string
  createdAt: string
}

const auth = useAuthStore()
const canAdjust = () => auth.hasRole('ADMIN', 'WAREHOUSE')

const alerts = ref<Stock[]>([])
const allStocks = ref<Stock[]>([])
const transactions = ref<Tx[]>([])
const loading = ref(true)
const showAll = ref(false)

const adjustOpen = ref(false)
const submitting = ref(false)
const form = ref({ skuId: 0, skuCode: '', type: 'INBOUND', delta: 1, reason: '' })

async function load() {
  loading.value = true
  try {
    const [a, s, t] = await Promise.all([
      api<Stock[]>('/api/v1/inventory/alerts'),
      api<Stock[]>('/api/v1/inventory'),
      api<Tx[]>('/api/v1/inventory/transactions'),
    ])
    alerts.value = a
    allStocks.value = s
    transactions.value = t
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '库存加载失败')
  } finally {
    loading.value = false
  }
}

function openAdjust(stock: Stock) {
  form.value = { skuId: stock.skuId, skuCode: stock.skuCode, type: 'INBOUND', delta: 1, reason: '' }
  adjustOpen.value = true
}

async function submitAdjust() {
  if (!form.value.reason.trim()) {
    ElMessage.warning('请填写调整说明')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认对 ${form.value.skuCode} 执行${form.value.type === 'INBOUND' ? '入库' : form.value.type === 'OUTBOUND' ? '出库' : '调整'} ${form.value.delta >= 0 ? '+' : ''}${form.value.delta} 件？`,
      '请确认操作',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await api('/api/v1/inventory/adjustments', {
      method: 'POST',
      body: JSON.stringify({ ...form.value, delta: Number(form.value.delta), reason: form.value.reason }),
    })
    ElMessage.success('库存已调整并写入流水')
    adjustOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '调整失败')
  } finally {
    submitting.value = false
  }
}

const typeLabel = (t: string) =>
  ({ INBOUND: '入库', OUTBOUND: '出库', ADJUSTMENT: '调整' } as Record<string, string>)[t] || t

function fmtTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(5, 16) : '—'
}

onMounted(load)
</script>

<template>
  <div class="inv-page" v-loading="loading">
    <section class="alert-summary">
      <div class="alert-icon"><el-icon><WarningFilled /></el-icon></div>
      <div>
        <p class="alert-label">低库存预警</p>
        <strong class="alert-num">{{ alerts.length }}</strong>
        <p class="alert-hint">可用库存 ≤ 安全库存</p>
      </div>
      <el-switch
        v-model="showAll"
        inline-prompt
        active-text="全部"
        inactive-text="预警"
        @change="() => load()"
      />
    </section>

    <section>
      <ul v-if="(showAll ? allStocks : alerts).length" class="stock-cards">
        <li
          v-for="stock in (showAll ? allStocks : alerts)"
          :key="stock.skuId"
          class="stock-card"
          :class="{ warn: stock.availableQty <= stock.safetyStock }"
        >
          <div class="stock-main">
            <strong>{{ stock.skuCode }}</strong>
            <p class="stock-meta">可用 {{ stock.availableQty }} · 锁定 {{ stock.lockedQty }} · 安全 {{ stock.safetyStock }}</p>
          </div>
          <div class="stock-right">
            <span class="stock-tag" :class="stock.availableQty <= stock.safetyStock ? 'warn' : 'ok'">
              {{ stock.availableQty <= stock.safetyStock ? '需补货' : '正常' }}
            </span>
            <button v-if="canAdjust()" type="button" class="adjust-btn" @click="openAdjust(stock)">调整</button>
          </div>
        </li>
      </ul>
      <el-empty v-else description="暂无预警库存" :image-size="72" />
    </section>

    <section class="tx-block">
      <h2>最近库存流水</h2>
      <ul v-if="transactions.length" class="tx-list">
        <li v-for="tx in transactions.slice(0, 6)" :key="tx.id">
          <span class="tx-type" :class="tx.delta >= 0 ? 'in' : 'out'">{{ typeLabel(tx.type) }}</span>
          <span class="tx-body">
            <b>{{ tx.delta >= 0 ? '+' : '' }}{{ tx.delta }}</b> → {{ tx.afterQty }} ·
            {{ tx.operator }} · {{ fmtTime(tx.createdAt) }}
            <p>{{ tx.reason }}</p>
          </span>
        </li>
      </ul>
      <p v-else class="no-tx">暂无流水</p>
    </section>

    <el-dialog v-model="adjustOpen" title="库存调整" width="min(94vw, 420px)">
      <div class="adjust-form">
        <label>SKU：{{ form.skuCode }}</label>
        <div class="type-row">
          <button
            v-for="t in ['INBOUND', 'OUTBOUND', 'ADJUSTMENT']"
            :key="t"
            type="button"
            class="type-btn"
            :class="{ active: form.type === t }"
            @click="form.type = t"
          >
            {{ typeLabel(t) }}
          </button>
        </div>
        <label>数量（出库/调整填负数）</label>
        <input
          v-model.number="form.delta"
          type="number"
          class="big-input"
          :min="-999999"
          :max="999999"
        />
        <label>说明（必填）</label>
        <input v-model="form.reason" type="text" class="big-input" maxlength="100" placeholder="例如：到货入库" />
      </div>
      <template #footer>
        <el-button size="large" @click="adjustOpen = false">取消</el-button>
        <el-button size="large" type="primary" :loading="submitting" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.inv-page {
  display: grid;
  gap: 16px;
}
.alert-summary {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 14px;
}
.alert-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 12px;
  background: #ffedd5;
  color: #c2410c;
  font-size: 1.4rem;
}
.alert-label,
.alert-hint {
  margin: 0;
  color: #9a3412;
  font-size: 0.78rem;
}
.alert-num {
  display: block;
  font-size: 1.8rem;
  line-height: 1.1;
  color: #7c2d12;
}
.alert-summary .el-switch {
  margin-left: auto;
}
.stock-cards {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.stock-card {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
}
.stock-card.warn {
  border-color: #fed7aa;
  background: #fffbf5;
}
.stock-main {
  flex: 1;
  min-width: 0;
}
.stock-main strong {
  font-size: 0.95rem;
}
.stock-meta {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 0.78rem;
}
.stock-right {
  display: grid;
  gap: 8px;
  place-items: end;
}
.stock-tag {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
}
.stock-tag.warn {
  background: #ffedd5;
  color: #c2410c;
}
.stock-tag.ok {
  background: #dcfce7;
  color: #15803d;
}
.adjust-btn {
  min-width: 84px;
  min-height: 42px;
  border: 0;
  border-radius: 9px;
  background: #2563eb;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
}
.tx-block h2 {
  margin: 0 0 10px;
  font-size: 0.98rem;
}
.tx-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}
.tx-list li {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 10px;
  font-size: 0.82rem;
}
.tx-type {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 700;
}
.tx-type.in {
  background: #dcfce7;
  color: #15803d;
}
.tx-type.out {
  background: #fee2e2;
  color: #dc2626;
}
.tx-body {
  color: #475569;
}
.tx-body p {
  margin: 2px 0 0;
  color: #94a3b8;
}
.no-tx {
  color: #94a3b8;
  font-size: 0.84rem;
}
.adjust-form {
  display: grid;
  gap: 10px;
}
.adjust-form label {
  color: #475569;
  font-size: 0.86rem;
}
.type-row {
  display: flex;
  gap: 8px;
}
.type-btn {
  flex: 1;
  min-height: 44px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: #fff;
  color: #475569;
  font-size: 0.9rem;
}
.type-btn.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}
.big-input {
  height: 46px;
  padding: 0 12px;
  border: 1.5px solid #dbe4f0;
  border-radius: 10px;
  font-size: 1rem;
  outline: 0;
}
.big-input:focus {
  border-color: #2563eb;
}
</style>
