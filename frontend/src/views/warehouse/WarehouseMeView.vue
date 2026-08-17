<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.replace('/login')
}

function backToDesktop() {
  router.push('/')
}
</script>

<template>
  <div class="me-page">
    <section class="user-card">
      <div class="avatar">{{ auth.user?.name?.slice(0, 1) || '客' }}</div>
      <div class="user-info">
        <strong>{{ auth.user?.name || '—' }}</strong>
        <p>{{ auth.user?.username || '' }}</p>
        <span class="role-badge">{{ auth.roleLabel }}</span>
      </div>
    </section>

    <section class="panel">
      <h2>使用说明</h2>
      <ul class="tips">
        <li>
          <b>扫码/手输查单</b>：在「待处理」或「订单」页的输入框中输入订单号后按回车，与扫码枪行为一致；未来可接入摄像头扫码。
        </li>
        <li>
          <b>发货</b>：「待处理」页可直接对待发货订单一键发货；需要管理员或仓库员角色。
        </li>
        <li>
          <b>库存调整</b>：在「库存」页对预警商品入库/出库/调整，所有变动都会写入库存流水并可追溯。
        </li>
        <li>
          <b>权限</b>：只读成员仅能查看订单与工作台；按钮会按角色自动显示或隐藏。
        </li>
      </ul>
    </section>

    <section class="panel">
      <h2>其他入口</h2>
      <button type="button" class="plain-btn" @click="backToDesktop">返回 PC 管理后台</button>
    </section>

    <button type="button" class="logout-btn" @click="logout">退出登录</button>
  </div>
</template>

<style scoped>
.me-page {
  display: grid;
  gap: 16px;
}
.user-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 20px;
  background: #172554;
  border-radius: 14px;
  color: #fff;
}
.avatar {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  font-size: 1.5rem;
  font-weight: 700;
}
.user-info strong {
  font-size: 1.15rem;
}
.user-info p {
  margin: 4px 0 6px;
  color: #cbd5e1;
  font-size: 0.82rem;
}
.role-badge {
  padding: 3px 12px;
  border-radius: 999px;
  background: rgb(147 197 253 / 20%);
  color: #93c5fd;
  font-size: 0.78rem;
  font-weight: 700;
}
.panel {
  padding: 16px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
}
.panel h2 {
  margin: 0 0 10px;
  font-size: 0.98rem;
}
.tips {
  margin: 0;
  padding: 0 0 0 18px;
  display: grid;
  gap: 10px;
}
.tips li {
  color: #475569;
  font-size: 0.86rem;
  line-height: 1.6;
}
.plain-btn {
  width: 100%;
  min-height: 46px;
  border: 1px solid #dbe4f0;
  border-radius: 10px;
  background: #fff;
  color: #2563eb;
  font-size: 0.92rem;
  font-weight: 600;
}
.logout-btn {
  width: 100%;
  min-height: 50px;
  border: 0;
  border-radius: 12px;
  background: #dc2626;
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
}
</style>
