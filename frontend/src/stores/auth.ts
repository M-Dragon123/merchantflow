import { defineStore } from 'pinia'
import { api } from '@/api/client'
export interface CurrentUser {
  id: number
  username: string
  name: string
  roles: string[]
}
interface LoginResult {
  accessToken: string
  user: CurrentUser
}
const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  OPERATOR: '运营',
  WAREHOUSE: '仓库员',
  VIEWER: '只读成员',
}
export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('merchantflow_access_token') || '',
    user: null as CurrentUser | null,
  }),
  getters: {
    roleLabel: (state) => ROLE_LABELS[state.user?.roles[0] || ''] || '成员',
  },
  actions: {
    async login(username: string, password: string) {
      const result = await api<LoginResult>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      this.accessToken = result.accessToken
      this.user = result.user
      localStorage.setItem('merchantflow_access_token', result.accessToken)
    },
    /** 页面刷新后恢复用户信息；Token 失效则自动登出。 */
    async ensureUser() {
      if (!this.accessToken || this.user) return
      try {
        this.user = await api<CurrentUser>('/api/v1/auth/me')
      } catch {
        this.logout()
      }
    },
    hasRole(...roles: string[]) {
      return !!this.user && roles.some((role) => this.user!.roles.includes(role))
    },
    logout() {
      this.accessToken = ''
      this.user = null
      localStorage.removeItem('merchantflow_access_token')
    },
  },
})
