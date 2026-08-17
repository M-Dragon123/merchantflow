import { createRouter, createWebHistory } from 'vue-router'
import WelcomeView from '@/views/WelcomeView.vue'
import LoginView from '@/views/LoginView.vue'
import ForbiddenView from '@/views/ForbiddenView.vue'
import { useAuthStore } from '@/stores/auth'
import AppShell from '@/layouts/AppShell.vue'
import WarehouseShell from '@/layouts/WarehouseShell.vue'
import ProductView from '@/views/ProductView.vue'
import InventoryView from '@/views/InventoryView.vue'
import OrderView from '@/views/OrderView.vue'
import StocktakeView from '@/views/StocktakeView.vue'
import CustomersView from '@/views/CustomersView.vue'
import UsersView from '@/views/UsersView.vue'
import AssistantView from '@/views/AssistantView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import WarehouseTodoView from '@/views/warehouse/WarehouseTodoView.vue'
import WarehouseOrderView from '@/views/warehouse/WarehouseOrderView.vue'
import WarehouseInventoryView from '@/views/warehouse/WarehouseInventoryView.vue'
import WarehouseMeView from '@/views/warehouse/WarehouseMeView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    roles?: string[]
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppShell,
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'welcome', component: WelcomeView, meta: { title: '工作台' } },
        {
          path: 'products',
          name: 'products',
          component: ProductView,
          meta: { title: '商品与 SKU', roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
        },
        {
          path: 'inventory',
          name: 'inventory',
          component: InventoryView,
          meta: { title: '库存中心', roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
        },
        { path: 'orders', name: 'orders', component: OrderView, meta: { title: '订单管理' } },
        {
          path: 'stocktakes',
          name: 'stocktakes',
          component: StocktakeView,
          meta: { title: '库存盘点', roles: ['ADMIN', 'WAREHOUSE'] },
        },
        {
          path: 'customers',
          name: 'customers',
          component: CustomersView,
          meta: { title: '客户管理', roles: ['ADMIN', 'OPERATOR'] },
        },
        {
          path: 'users',
          name: 'users',
          component: UsersView,
          meta: { title: '员工管理', roles: ['ADMIN'] },
        },
        {
          path: 'assistant',
          name: 'assistant',
          component: AssistantView,
          meta: { title: 'AI 运营助手', roles: ['ADMIN', 'OPERATOR'] },
        },
      ],
    },
    {
      path: '/m',
      component: WarehouseShell,
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'm-todo', component: WarehouseTodoView, meta: { title: '待处理' } },
        { path: 'orders', name: 'm-orders', component: WarehouseOrderView, meta: { title: '订单' } },
        {
          path: 'inventory',
          name: 'm-inventory',
          component: WarehouseInventoryView,
          meta: { title: '库存', roles: ['ADMIN', 'OPERATOR', 'WAREHOUSE'] },
        },
        { path: 'me', name: 'm-me', component: WarehouseMeView, meta: { title: '我的' } },
      ],
    },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/forbidden', name: 'forbidden', component: ForbiddenView },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.accessToken)
    return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.requiresAuth) {
    await auth.ensureUser()
    // ensureUser 在 token 失效时会自动登出；此时必须回到登录页，否则页面会以未登录状态渲染导致接口 403
    if (!auth.accessToken) return { name: 'login', query: { redirect: to.fullPath } }
    const roles = to.meta.roles
    if (roles && roles.length && !auth.hasRole(...roles)) return { name: 'forbidden' }
  }
  if (to.name === 'login' && auth.accessToken) return { name: 'welcome' }
})

export default router
