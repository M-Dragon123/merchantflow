<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'MerchantFlow@2026' })
onMounted(() => {
  // 防御：若浏览器残留失效 token（守卫已保证能进登录页的 token 必然不可用），直接清理，避免旧状态干扰
  if (auth.accessToken && !auth.user) auth.logout()
})
async function submit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    await router.replace(String(router.currentRoute.value.query.redirect || '/'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main class="login-shell">
    <section class="login-brand">
      <p>MERCHANTFLOW</p>
      <h1>把订单<br />握在手中。</h1>
      <small>商家管家 · 运营与仓配协同</small>
    </section>
    <section class="login-card">
      <div>
        <p class="eyebrow">安全登录</p>
        <h2>欢迎回来</h2>
        <p class="hint">使用已分配的工作账号进入系统。</p>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="submit"
        ><el-form-item label="账号"
          ><el-input v-model="form.username" size="large" autocomplete="username" /></el-form-item
        ><el-form-item label="密码"
          ><el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            autocomplete="current-password" /></el-form-item
        ><el-button
          native-type="submit"
          type="primary"
          size="large"
          :loading="loading"
          class="login-button"
          >进入工作台</el-button
        ></el-form
      >
      <p class="demo-tip">演示账号：admin / MerchantFlow@2026</p>
    </section>
  </main>
</template>
<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr minmax(380px, 46%);
  background: #f8fafc;
}
.login-brand {
  padding: clamp(3rem, 9vw, 9rem);
  color: #e0e7ff;
  background: linear-gradient(145deg, #172554, #1e3a8a);
}
.login-brand p {
  letter-spacing: 0.18em;
  font-size: 0.75rem;
}
.login-brand h1 {
  margin: 20vh 0 1rem;
  font-size: clamp(3rem, 6vw, 5.5rem);
  letter-spacing: 0.02em;
}
.login-brand small {
  color: #bfdbfe;
}
.login-card {
  align-self: center;
  padding: clamp(2rem, 8vw, 6rem);
  max-width: 620px;
}
.login-card h2 {
  margin: 0.5rem 0;
  font-size: 2.5rem;
}
.hint,
.demo-tip {
  color: #64748b;
}
.login-card form {
  margin-top: 2.5rem;
}
.login-button {
  width: 100%;
  margin-top: 1rem;
}
.demo-tip {
  font-size: 0.78rem;
  text-align: center;
  margin-top: 1.5rem;
}
@media (max-width: 760px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
  .login-brand {
    padding: 2.5rem;
  }
  .login-brand h1 {
    margin: 3rem 0 0.5rem;
  }
  .login-card {
    padding: 2.5rem 1.5rem;
  }
}
</style>
