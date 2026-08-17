<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/client'

interface UserItem {
  id: number
  username: string
  name: string
  status: boolean
  roles: string[]
  createdAt: string
}

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  OPERATOR: '运营',
  WAREHOUSE: '仓库员',
  VIEWER: '只读成员',
}
const ROLE_CODES = Object.keys(ROLE_LABELS)

const users = ref<UserItem[]>([])
const loading = ref(true)
const createOpen = ref(false)
const submitting = ref(false)
const form = reactive({ username: '', password: '', name: '', roleCode: 'OPERATOR' })
const roleOpen = ref(false)
const roleTarget = ref<UserItem | null>(null)
const roleCodes = ref<string[]>([])
const roleSubmitting = ref(false)

async function load() {
  loading.value = true
  try {
    users.value = await api<UserItem[]>('/api/v1/users')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '员工列表加载失败')
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!form.username || !form.password || !form.name || !form.roleCode) {
    ElMessage.warning('请完整填写员工信息')
    return
  }
  submitting.value = true
  try {
    await api('/api/v1/users', {
      method: 'POST',
      body: JSON.stringify(form),
    })
    ElMessage.success('员工已创建')
    createOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: UserItem) {
  const next = !row.status
  const actionText = next ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}账号 ${row.username}？`, '请确认操作', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '返回',
    })
    await api(`/api/v1/users/${row.id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: next }),
    })
    ElMessage.success(`已${actionText}`)
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

function openRoles(row: UserItem) {
  roleTarget.value = row
  roleCodes.value = [...row.roles]
  roleOpen.value = true
}

async function saveRoles() {
  if (!roleTarget.value || !roleCodes.value.length) {
    ElMessage.warning('至少选择一个角色')
    return
  }
  roleSubmitting.value = true
  try {
    await api(`/api/v1/users/${roleTarget.value.id}/roles`, {
      method: 'PUT',
      body: JSON.stringify({ roleCodes: roleCodes.value }),
    })
    ElMessage.success('角色已更新')
    roleOpen.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败')
  } finally {
    roleSubmitting.value = false
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
        <strong>员工管理</strong>
        <span> 仅管理员可维护员工账号与角色；停用后该账号无法登录</span>
      </div>
      <el-button type="primary" @click="createOpen = true">新建员工</el-button>
    </div>

    <el-table :data="users" v-loading="loading" empty-text="暂无员工" class="data-table">
      <el-table-column prop="username" label="账号" min-width="130" />
      <el-table-column prop="name" label="姓名" min-width="110" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role" size="small" effect="plain" class="role-tag">
            {{ ROLE_LABELS[role] || role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '正常' : '已停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="165">
        <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="170" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openRoles(row)">修改角色</el-button>
          <el-button text :type="row.status ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createOpen" title="新建员工" width="min(94vw, 440px)">
      <el-form label-position="top">
        <el-form-item label="登录账号">
          <el-input v-model="form.username" placeholder="例如：xiaoli" autocomplete="off" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="例如：李小明" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 8 位" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode" class="full">
            <el-option v-for="code in ROLE_CODES" :key="code" :label="ROLE_LABELS[code]" :value="code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleOpen" title="修改角色" width="min(94vw, 400px)">
      <p v-if="roleTarget" class="role-hint">{{ roleTarget.name }}（{{ roleTarget.username }}）</p>
      <el-checkbox-group v-model="roleCodes" class="role-group">
        <el-checkbox v-for="code in ROLE_CODES" :key="code" :value="code" :label="ROLE_LABELS[code]" />
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleOpen = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="saveRoles">保存</el-button>
      </template>
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
.role-tag {
  margin-right: 6px;
}
.full {
  width: 100%;
}
.role-hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 0.86rem;
}
.role-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
