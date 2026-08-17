<script setup lang="ts">
import { computed } from 'vue'
import {
  Box,
  DataAnalysis,
  DocumentChecked,
  Goods,
  List,
  Monitor,
  SwitchButton,
  Timer,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
function logout() {
  auth.logout()
  router.replace('/login')
}
const menus = [
  { to: '/', label: '工作台', icon: DataAnalysis },
  { to: '/products', label: '商品', icon: Goods, roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
  { to: '/inventory', label: '库存', icon: Box, roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
  { to: '/orders', label: '订单', icon: List },
  { to: '/stocktakes', label: '库存盘点', icon: DocumentChecked, roles: ['ADMIN', 'WAREHOUSE'] },
  { to: '/customers', label: '客户管理', icon: User, roles: ['ADMIN', 'OPERATOR'] },
  { to: '/users', label: '员工管理', icon: UserFilled, roles: ['ADMIN'] },
  { to: '/m', label: '仓库模式', icon: Monitor },
]
const visibleMenus = computed(() =>
  menus.filter((item) => !item.roles || auth.hasRole(...item.roles)),
)
const mobileMenus = [
  { to: '/m', label: '待处理', icon: Timer },
  { to: '/m/orders', label: '订单', icon: List },
  { to: '/m/inventory', label: '库存', icon: Box, roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
  { to: '/m/me', label: '我的', icon: User },
]
const visibleMobileMenus = computed(() =>
  mobileMenus.filter((item) => !item.roles || auth.hasRole(...item.roles)),
)
function isMobileActive(to: string) {
  if (to === '/m') return route.path === '/m' || route.path === '/m/'
  return route.path.startsWith(to)
}
</script>
<template>
  <div class="app-shell">
    <aside class="side-nav">
      <RouterLink class="logo" to="/"><b>MF</b><span>商家管家</span></RouterLink>
      <nav>
        <RouterLink v-for="item in visibleMenus" :key="item.to" :to="item.to"
          ><el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span></RouterLink
        >
      </nav>
      <div class="user-panel">
        <span>{{ auth.user?.name || '当前用户' }} · <em>{{ auth.roleLabel }}</em></span
        ><el-button text :icon="SwitchButton" @click="logout">退出</el-button>
      </div>
    </aside>
    <main class="main-panel">
      <header>
        <div>
          <p class="section-label">MERCHANTFLOW</p>
          <h1>{{ $route.meta.title || '商家管家' }}</h1>
        </div>
        <el-tag type="success" effect="plain">已连接</el-tag>
      </header>
      <section class="page-content"><RouterView /></section>
    </main>
    <nav class="mobile-nav">
      <button
        v-for="item in visibleMobileMenus"
        :key="item.to"
        type="button"
        class="m-item"
        :class="{ active: isMobileActive(item.to) }"
        @click="router.push(item.to)"
      >
        <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>
<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f5f7fb;
}
.side-nav {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 2;
  width: 236px;
  display: flex;
  flex-direction: column;
  padding: 24px 14px;
  color: #dbeafe;
  background: #172554;
}
.logo {
  display: flex;
  gap: 11px;
  align-items: center;
  padding: 0 10px 30px;
  color: inherit;
  text-decoration: none;
  font-weight: 700;
}
.logo b {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #172554;
  background: #93c5fd;
  border-radius: 8px;
}
.side-nav nav {
  display: grid;
  gap: 6px;
}
.side-nav nav a {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 11px 12px;
  border-radius: 8px;
  color: #bfdbfe;
  text-decoration: none;
}
.side-nav nav a.router-link-active {
  color: #fff;
  background: rgb(59 130 246 / 28%);
}
.user-panel {
  margin-top: auto;
  display: grid;
  gap: 4px;
  padding: 14px 10px;
  border-top: 1px solid #29498b;
  font-size: 0.8rem;
}
.user-panel em {
  color: #93c5fd;
  font-style: normal;
}
.user-panel :deep(.el-button) {
  justify-content: flex-start;
  color: #bfdbfe;
}
.main-panel {
  min-height: 100vh;
  margin-left: 236px;
}
.main-panel header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 40px 18px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}
.section-label {
  margin: 0;
  color: #64748b;
  letter-spacing: 0.13em;
  font-size: 0.66rem;
  font-weight: 700;
}
.main-panel h1 {
  margin: 4px 0 0;
  font-size: 1.45rem;
}
.page-content {
  padding: 28px 40px;
}
.mobile-nav {
  display: none;
}
@media (max-width: 760px) {
  .side-nav {
    display: none;
  }
  .main-panel {
    margin-left: 0;
    padding-bottom: 74px;
  }
  .main-panel header {
    padding: 18px 20px;
  }
  .page-content {
    padding: 18px 16px;
  }
  .mobile-nav {
    position: fixed;
    inset: auto 0 0;
    z-index: 5;
    display: flex;
    justify-content: space-around;
    padding: 8px 4px max(8px, env(safe-area-inset-bottom));
    background: #fff;
    border-top: 1px solid #e2e8f0;
  }
  .m-item {
    display: grid;
    gap: 3px;
    place-items: center;
    min-width: 64px;
    min-height: 52px;
    border: 0;
    background: transparent;
    color: #64748b;
    font-size: 0.68rem;
    cursor: pointer;
  }
  .m-item .el-icon {
    font-size: 1.3rem;
  }
  .m-item.active {
    color: #2563eb;
  }
}
</style>
