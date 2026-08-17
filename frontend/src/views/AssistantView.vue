<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { api } from '@/api/client'

interface Suggestion {
  type: string
  skuId: number
  skuCode: string
  title: string
  description: string
  delta: number
}
interface ChatMessage {
  role: 'user' | 'assistant'
  text: string
  suggestions?: Suggestion[]
}

const QUICK_QUESTIONS = [
  '哪些商品建议补货？',
  '最近卖得最好的商品？',
  '今天销售额怎么样？',
  '有多少待发货订单？',
  '有什么异常订单？',
]

const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const listRef = ref<HTMLElement | null>(null)

function push(role: 'user' | 'assistant', text: string, suggestions?: Suggestion[]) {
  messages.value.push({ role, text, suggestions })
}

async function scrollBottom() {
  await nextTick()
  listRef.value?.scrollTo({ top: listRef.value.scrollHeight, behavior: 'smooth' })
}

async function send(text?: string) {
  const content = (text ?? input.value).trim()
  if (!content || loading.value) return
  push('user', content)
  input.value = ''
  loading.value = true
  try {
    const result = await api<{ reply: string; suggestions: Suggestion[] }>('/api/v1/assistant/chat', {
      method: 'POST',
      body: JSON.stringify({ message: content }),
    })
    push('assistant', result.reply, result.suggestions || [])
  } catch (e) {
    push('assistant', e instanceof Error ? e.message : '助手暂时不可用，请稍后再试')
  } finally {
    loading.value = false
    await scrollBottom()
  }
}

async function executeSuggestion(suggestion: Suggestion) {
  try {
    await ElMessageBox.confirm(
      `将执行：${suggestion.title}\n${suggestion.description}\n\n确认后按「入库 +${suggestion.delta}」写入库存流水，操作人记为当前账号。`,
      '二次确认：执行补货',
      { type: 'warning', confirmButtonText: '确认入库', cancelButtonText: '返回' },
    )
    await api('/api/v1/inventory/adjustments', {
      method: 'POST',
      body: JSON.stringify({
        skuId: suggestion.skuId,
        delta: suggestion.delta,
        type: 'INBOUND',
        reason: `AI 建议补货：${suggestion.skuCode}`,
      }),
    })
    ElMessage.success(`已入库 +${suggestion.delta}，流水已记录`)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e instanceof Error ? e.message : '执行失败')
  }
}

push(
  'assistant',
  '你好，我是商家管家 AI 运营助手。我可以帮你查询库存、销量与订单情况（只读查询，不会直接修改数据）。试试下面的问题，或直接输入你的问题：',
)
</script>

<template>
  <div class="assistant-page">
    <div ref="listRef" class="chat-list">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="msg-row"
        :class="msg.role === 'user' ? 'from-user' : 'from-ai'"
      >
        <div class="bubble">
          <p class="bubble-text">{{ msg.text }}</p>
          <div v-if="msg.suggestions && msg.suggestions.length" class="suggestions">
            <div v-for="s in msg.suggestions" :key="s.skuId" class="suggestion-card">
              <div class="suggestion-main">
                <strong>{{ s.title }}</strong>
                <p>{{ s.description }}</p>
              </div>
              <el-button type="primary" size="small" @click="executeSuggestion(s)">一键补货</el-button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="loading" class="msg-row from-ai">
        <div class="bubble bubble-typing"><span /><span /><span /></div>
      </div>
    </div>

    <div class="quick-row">
      <button
        v-for="q in QUICK_QUESTIONS"
        :key="q"
        type="button"
        class="quick-chip"
        :disabled="loading"
        @click="send(q)"
      >
        {{ q }}
      </button>
    </div>

    <div class="input-bar">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        placeholder="输入问题，例如：哪些商品建议补货？"
        resize="none"
        @keydown.enter.exact.prevent="send()"
      />
      <el-button
        type="primary"
        :loading="loading"
        :icon="Promotion"
        class="send-btn"
        @click="send()"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.assistant-page {
  display: grid;
  grid-template-rows: 1fr auto auto;
  gap: 12px;
  height: calc(100vh - 180px);
  min-height: 420px;
}
.chat-list {
  overflow-y: auto;
  padding: 6px 2px;
  display: grid;
  align-content: start;
  gap: 12px;
}
.msg-row {
  display: flex;
}
.from-user {
  justify-content: flex-end;
}
.from-ai {
  justify-content: flex-start;
}
.bubble {
  max-width: min(680px, 86%);
  padding: 12px 16px;
  border-radius: 14px;
  line-height: 1.65;
}
.from-user .bubble {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.from-ai .bubble {
  background: #fff;
  border: 1px solid #e8edf5;
  border-bottom-left-radius: 4px;
}
.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  font-size: 0.92rem;
}
.suggestions {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}
.suggestion-card {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}
.suggestion-main strong {
  font-size: 0.86rem;
  color: #1e3a8a;
}
.suggestion-main p {
  margin: 3px 0 0;
  font-size: 0.78rem;
  color: #475569;
}
.bubble-typing {
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 14px 18px;
}
.bubble-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: blink 1.2s infinite;
}
.bubble-typing span:nth-child(2) {
  animation-delay: 0.2s;
}
.bubble-typing span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%, 80%, 100% { opacity: 0.25; }
  40% { opacity: 1; }
}
.quick-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.quick-chip {
  padding: 7px 14px;
  border: 1px solid #dbe4f0;
  border-radius: 999px;
  background: #fff;
  color: #1d4ed8;
  font-size: 0.82rem;
  cursor: pointer;
}
.quick-chip:disabled {
  opacity: 0.5;
}
.input-bar {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.send-btn {
  height: 48px;
  min-width: 92px;
}
@media (max-width: 560px) {
  .assistant-page {
    height: calc(100vh - 210px);
  }
  .quick-row {
    display: none;
  }
}
</style>
