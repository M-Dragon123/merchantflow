<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="not-found">
    <strong>404</strong>
    <p>页面不存在或已被移除</p>
    <el-button type="primary" size="large" @click="router.push('/')">返回工作台</el-button>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
}
.not-found strong {
  font-size: 4.5rem;
  color: #1e3a8a;
  line-height: 1;
}
.not-found p {
  margin: 0 0 14px;
  color: #64748b;
}
</style>
