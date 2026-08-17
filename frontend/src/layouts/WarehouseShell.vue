<script setup lang="ts">
import { computed } from 'vue'
import { Box, List, Monitor, Timer, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tabs = [
  { to: '/m', label: '待处理', icon: Timer },
  { to: '/m/orders', label: '订单', icon: List },
  { to: '/m/inventory', label: '库存', icon: Box, roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
  { to: '/m/me', label: '我的', icon: User },
]
const visibleTabs = computed(() => tabs.filter((t) => !t.roles || auth.hasRole(...t.roles)))

function isActive(to: string) {
  if (to === '/m') return route.path === '/m' || route.path === '/m/'
  return route.path.startsWith(to)
}

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="warehouse-shell">
    <header class="shell-head">
      <button type="button" class="back-link" @click="goHome">
        <el-icon><Monitor /></el-icon><span>管理后台</span>
      </button>
      <div class="head-right">
        <span class="head-role">{{ auth.roleLabel }}</span>
        <span class="head-name">{{ auth.user?.name || '' }}</span>
      </div>
    </header>

    <main class="shell-main">
      <RouterView />
    </main>

    <nav class="bottom-nav">
      <RouterLink
        v-for="tab in visibleTabs"
        :key="tab.to"
        :to="tab.to"
        class="nav-item"
        :class="{ active: isActive(tab.to) }"
      >
        <el-icon class="nav-icon"><component :is="tab.icon" /></el-icon>
        <span class="nav-label">{{ tab.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.warehouse-shell {
  min-height: 100vh;
  background: #f5f7fb;
}
.shell-head {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: #172554;
  color: #dbeafe;
}
.back-link {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 8px 12px;
  border: 0;
  border-radius: 8px;
  background: rgb(59 130 246 / 20%);
  color: #dbeafe;
  font-size: 0.86rem;
  cursor: pointer;
}
.head-right {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 0.82rem;
}
.head-role {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgb(147 197 253 / 18%);
  color: #93c5fd;
}
.head-name {
  color: #bfdbfe;
}
.shell-main {
  max-width: 860px;
  margin: 0 auto;
  padding: 16px 16px calc(88px + env(safe-area-inset-bottom));
}
.bottom-nav {
  position: fixed;
  inset: auto 0 0;
  z-index: 20;
  display: flex;
  justify-content: space-around;
  padding: 8px 6px max(8px, env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #e2e8f0;
  box-shadow: 0 -4px 18px rgb(30 58 138 / 6%);
}
.nav-item {
  display: grid;
  gap: 3px;
  place-items: center;
  min-width: 72px;
  min-height: 56px;
  border-radius: 10px;
  color: #64748b;
  text-decoration: none;
}
.nav-item.active {
  color: #2563eb;
  background: #eff6ff;
}
.nav-icon {
  font-size: 1.45rem;
}
.nav-label {
  font-size: 0.74rem;
  font-weight: 600;
}
@media (min-width: 760px) {
  .shell-head {
    justify-content: flex-end;
  }
  .shell-main {
    padding: 28px 24px calc(100px + env(safe-area-inset-bottom));
  }
  .nav-item {
    min-height: 62px;
    min-width: 96px;
  }
  .nav-icon {
    font-size: 1.6rem;
  }
}
</style>
