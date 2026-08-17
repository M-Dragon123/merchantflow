<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

interface Product {
  skuId: number
  spuId: number
  skuCode: string
  salePrice: number
  costPrice: number
  status: string
  availableQty: number
  safetyStock: number
}
interface Category {
  id: number
  name: string
}

const auth = useAuthStore()
const canManage = () => auth.hasRole('ADMIN', 'OPERATOR')

const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const loading = ref(true)
const createOpen = ref(false)
const editOpen = ref(false)
const submitting = ref(false)
const createForm = reactive({
  name: '',
  categoryId: 0,
  skuCode: '',
  salePrice: 99,
  costPrice: 60,
  initialQty: 100,
  safetyStock: 20,
})
const editForm = reactive({
  skuId: 0,
  skuCode: '',
  name: '',
  salePrice: 99,
  costPrice: 60,
  status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    ;[products.value, categories.value] = await Promise.all([
      api<Product[]>('/api/v1/products'),
      api<Category[]>('/api/v1/categories'),
    ])
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '商品加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, {
    name: '',
    categoryId: categories.value[0]?.id || 0,
    skuCode: '',
    salePrice: 99,
    costPrice: 60,
    initialQty: 100,
    safetyStock: 20,
  })
  createOpen.value = true
}

async function createProduct() {
  if (!createForm.name || !createForm.categoryId || !createForm.skuCode) {
    ElMessage.warning('请填写商品名称、分类与 SKU 编码')
    return
  }
  submitting.value = true
  try {
    await api('/api/v1/products', {
      method: 'POST',
      body: JSON.stringify(createForm),
    })
    ElMessage.success('商品已创建，初始库存已入账')
    createOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}

function openEdit(row: Product) {
  Object.assign(editForm, {
    skuId: row.skuId,
    skuCode: row.skuCode,
    name: '',
    salePrice: row.salePrice,
    costPrice: row.costPrice,
    status: row.status,
  })
  editOpen.value = true
}

async function saveEdit() {
  if (editForm.salePrice < 0 || editForm.costPrice < 0) {
    ElMessage.warning('价格不能为负数')
    return
  }
  submitting.value = true
  try {
    await api(`/api/v1/products/${editForm.skuId}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: editForm.name || null,
        salePrice: editForm.salePrice,
        costPrice: editForm.costPrice,
        status: editForm.status,
      }),
    })
    ElMessage.success('商品已更新')
    editOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="module-page">
    <div class="toolbar">
      <div>
        <strong>商品档案</strong>
        <span> 共 {{ products.length }} 个 SKU</span>
      </div>
      <el-button v-if="canManage()" type="primary" @click="openCreate">新建商品</el-button>
    </div>

    <el-table :data="products" v-loading="loading" empty-text="暂无商品数据" class="data-table">
      <el-table-column prop="skuCode" label="SKU 编码" min-width="150" />
      <el-table-column label="售价" min-width="110">
        <template #default="{ row }">¥{{ Number(row.salePrice).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="成本" min-width="110">
        <template #default="{ row }">¥{{ Number(row.costPrice).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="availableQty" label="可用库存" min-width="110" />
      <el-table-column prop="safetyStock" label="安全库存" min-width="110" />
      <el-table-column label="状态" min-width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '在售' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canManage()" label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <p v-if="categories.length" class="category-line">
      可用分类：{{ categories.map((c) => c.name).join(' · ') }}
    </p>

    <el-dialog v-model="createOpen" title="新建商品" width="min(94vw, 480px)">
      <el-form label-position="top">
        <el-form-item label="商品名称（SPU）">
          <el-input v-model="createForm.name" placeholder="例如：夏季纯棉T恤" />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="createForm.categoryId" class="full">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU 编码">
          <el-input v-model="createForm.skuCode" placeholder="例如：MF-T001-WHITE-M" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="售价">
            <el-input-number v-model="createForm.salePrice" :min="0" :precision="2" class="full" />
          </el-form-item>
          <el-form-item label="成本价">
            <el-input-number v-model="createForm.costPrice" :min="0" :precision="2" class="full" />
          </el-form-item>
        </div>
        <div class="two-col">
          <el-form-item label="初始库存">
            <el-input-number v-model="createForm.initialQty" :min="0" class="full" />
          </el-form-item>
          <el-form-item label="安全库存">
            <el-input-number v-model="createForm.safetyStock" :min="0" class="full" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="createProduct">创建并入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editOpen" title="编辑商品" width="min(94vw, 460px)">
      <el-form label-position="top">
        <el-form-item label="SKU 编码">
          <el-input :model-value="editForm.skuCode" disabled />
        </el-form-item>
        <el-form-item label="商品名称（SPU，留空则不修改）">
          <el-input v-model="editForm.name" placeholder="输入新名称或留空" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="售价">
            <el-input-number v-model="editForm.salePrice" :min="0" :precision="2" class="full" />
          </el-form-item>
          <el-form-item label="成本价">
            <el-input-number v-model="editForm.costPrice" :min="0" :precision="2" class="full" />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio value="ACTIVE">在售</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.toolbar strong {
  font-size: 1.05rem;
}
.toolbar span,
.category-line {
  color: #64748b;
  font-size: 0.82rem;
}
.data-table {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
.category-line {
  margin-top: 14px;
}
.full {
  width: 100%;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
@media (max-width: 560px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
