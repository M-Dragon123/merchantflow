<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps<{ placeholder?: string; loading?: boolean }>()
const emit = defineEmits<{ submit: [value: string] }>()
const value = ref('')

function onEnter() {
  const text = value.value.trim()
  if (!text) return
  emit('submit', text)
  value.value = ''
}
</script>

<template>
  <div class="scan-input">
    <el-icon class="scan-icon"><Search /></el-icon>
    <input
      v-model="value"
      type="text"
      :placeholder="props.placeholder || '输入订单号，回车查询（模拟扫码）'"
      :disabled="props.loading"
      enterkeyhint="search"
      @keyup.enter="onEnter"
    />
    <button v-if="props.loading" type="button" class="scan-btn" disabled>查询中…</button>
    <button v-else type="button" class="scan-btn" @click="onEnter">查询</button>
  </div>
</template>

<style scoped>
.scan-input {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 4px 14px;
  background: #fff;
  border: 1.5px solid #dbe4f0;
  border-radius: 12px;
  transition: border-color 0.15s ease;
}
.scan-input:focus-within {
  border-color: #2563eb;
}
.scan-icon {
  color: #64748b;
  font-size: 1.15rem;
  flex-shrink: 0;
}
.scan-input input {
  flex: 1;
  min-width: 0;
  height: 46px;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 1.02rem;
  color: #172554;
}
.scan-input input::placeholder {
  color: #94a3b8;
}
.scan-btn {
  flex-shrink: 0;
  height: 46px;
  padding: 0 22px;
  border: 0;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 600;
}
.scan-btn:disabled {
  background: #93b4f2;
}
</style>
