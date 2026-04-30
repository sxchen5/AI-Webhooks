<template>
  <div class="chat-page">
    <header class="chat-header">
      <el-button class="back-btn" text type="primary" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回配置
      </el-button>
      <div class="header-center">
        <div class="bot-avatar" aria-hidden="true">
          <el-icon :size="22"><ChatDotRound /></el-icon>
        </div>
        <div class="header-text">
          <h1 class="title">{{ project?.botName || 'AI 问答' }}</h1>
          <p class="subtitle">{{ project?.gitUrl || '' }}</p>
        </div>
      </div>
      <div class="header-spacer" />
    </header>

    <el-scrollbar ref="scrollbarRef" class="chat-scroll">
      <div class="messages">
        <div v-if="!messages.length" class="welcome">
          <p class="welcome-title">开始对话</p>
          <p class="welcome-desc">
            在仓库克隆目录中执行 Agent（stream-json），回复会随流式增量显示。
          </p>
        </div>
        <div
          v-for="(m, idx) in messages"
          :key="idx"
          class="msg-row"
          :class="m.role === 'user' ? 'msg-row--user' : 'msg-row--bot'"
        >
          <div v-if="m.role === 'user'" class="bubble bubble--user">
            <div class="bubble-plain">{{ m.content }}</div>
          </div>
          <div v-else class="bubble bubble--assistant">
            <MarkdownOutputPanel :text="m.content" :rows="8" hide-toolbar />
          </div>
        </div>
        <div v-if="replying" class="thinking-row">
          <div class="thinking-icon" aria-hidden="true">
            <el-icon :size="20"><Cpu /></el-icon>
          </div>
          <div class="typing-indicator">
            <span /><span /><span />
          </div>
        </div>
      </div>
    </el-scrollbar>

    <footer class="chat-footer">
      <div class="composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          :autosize="{ minRows: 2, maxRows: 5 }"
          placeholder="输入问题后发送…"
          :disabled="replying || !project"
          maxlength="8000"
          show-word-limit
          @keydown.enter.exact.prevent="onEnterSend"
        />
        <el-button
          type="primary"
          class="send-btn"
          :loading="replying"
          :disabled="!draft.trim() || !project"
          @click="send"
        >
          发送
        </el-button>
      </div>
      <p v-if="lastMeta && !lastMeta.success" class="meta-warn">
        Agent 退出码 {{ lastMeta.exitCode }}，请查看上方回复或错误提示。
      </p>
    </footer>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChatDotRound, Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getGitQaProject, streamGitQaChat } from '@/api/gitQaProject'
import MarkdownOutputPanel from '@/components/MarkdownOutputPanel.vue'

const route = useRoute()
const router = useRouter()
const project = ref(null)
const messages = ref([])
const draft = ref('')
const replying = ref(false)
const scrollbarRef = ref(null)
const lastMeta = ref(null)

let abortCtrl = null
let sseBuffer = ''

function goBack() {
  abortCtrl?.abort()
  router.push({ name: 'GitQaProjects' })
}

function scrollToBottom() {
  nextTick(() => {
    const wrap = scrollbarRef.value?.wrapRef
    if (wrap) {
      wrap.scrollTop = wrap.scrollHeight
    }
  })
}

function onEnterSend() {
  if (!draft.value.includes('\n')) {
    send()
  }
}

function parseSseBlock(block) {
  let event = 'message'
  const dataLines = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  const dataStr = dataLines.join('\n')
  return { event, dataStr }
}

async function consumeSseStream(reader, botMsg) {
  const decoder = new TextDecoder()
  sseBuffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    sseBuffer += decoder.decode(value, { stream: true })
    let idx
    while ((idx = sseBuffer.indexOf('\n\n')) >= 0) {
      const raw = sseBuffer.slice(0, idx)
      sseBuffer = sseBuffer.slice(idx + 2)
      const { event, dataStr } = parseSseBlock(raw)
      if (!dataStr) continue
      try {
        const obj = JSON.parse(dataStr)
        if (event === 'assistant' && obj.delta) {
          botMsg.content += obj.delta
          scrollToBottom()
        } else if (event === 'error') {
          const msg = obj.message || '执行出错'
          botMsg.content += (botMsg.content ? '\n\n' : '') + `【错误】${msg}`
          if (obj.rawTail) {
            botMsg.content += `\n\n【原始输出尾部】\n${obj.rawTail}`
          }
          ElMessage.error(msg)
        } else if (event === 'done') {
          lastMeta.value = {
            success: !!obj.success,
            exitCode: obj.exitCode ?? -1,
          }
          if (!obj.success) {
            ElMessage.warning('Agent 执行未成功（退出码非 0）')
          }
        }
      } catch {
        // 忽略无法解析的块
      }
    }
  }
}

async function send() {
  const q = draft.value.trim()
  if (!q || replying.value || !project.value) return
  draft.value = ''
  messages.value.push({ role: 'user', content: q })
  const botMsg = { role: 'assistant', content: '' }
  messages.value.push(botMsg)
  lastMeta.value = null
  replying.value = true
  scrollToBottom()
  abortCtrl = new AbortController()
  try {
    const res = await streamGitQaChat(project.value.id, q, abortCtrl.signal)
    const reader = res.body?.getReader()
    if (!reader) {
      throw new Error('浏览器不支持流式响应')
    }
    await consumeSseStream(reader, botMsg)
  } catch (e) {
    if (e?.name === 'AbortError') {
      botMsg.content += '\n\n【已取消】'
    } else {
      botMsg.content = `请求失败：${e?.message || String(e)}`
      ElMessage.error('执行失败')
    }
  } finally {
    replying.value = false
    abortCtrl = null
    scrollToBottom()
  }
}

onMounted(async () => {
  const id = route.params.id
  try {
    project.value = await getGitQaProject(id)
  } catch {
    ElMessage.error('加载配置失败')
    goBack()
  }
})

onBeforeUnmount(() => {
  abortCtrl?.abort()
})
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  max-height: calc(100vh - 88px);
  margin: -8px -8px 0;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}
.back-btn {
  flex-shrink: 0;
}
.header-center {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1;
}
.bot-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #409eff, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.header-text {
  min-width: 0;
}
.title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}
.subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: min(560px, 70vw);
}
.header-spacer {
  width: 72px;
  flex-shrink: 0;
}
.chat-scroll {
  flex: 1;
  min-height: 0;
  padding: 20px 16px;
  background: #fff;
}
:deep(.chat-scroll .el-scrollbar__wrap) {
  overflow-x: hidden;
}
.messages {
  max-width: 880px;
  margin: 0 auto;
  padding-bottom: 24px;
}
.welcome {
  text-align: center;
  padding: 32px 16px 8px;
  color: #606266;
}
.welcome-title {
  margin: 0 0 8px;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}
.welcome-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
.msg-row {
  display: flex;
  margin-bottom: 16px;
}
.msg-row--user {
  justify-content: flex-end;
}
.msg-row--bot {
  justify-content: flex-start;
}
.bubble {
  max-width: min(720px, 100%);
  border-radius: 12px;
  padding: 10px 14px;
  line-height: 1.55;
  font-size: 14px;
}
.bubble--user {
  background: #f5f5f5;
  color: #303133;
}
.bubble--assistant {
  background: transparent;
  border: none;
  padding: 4px 0;
  width: 100%;
  max-width: 100%;
}
.bubble-plain {
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble--assistant :deep(.md-output-panel) {
  margin: 0;
}
.bubble--assistant :deep(.md-preview) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  min-height: 0;
  max-height: none;
}
.thinking-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding-left: 2px;
}
.thinking-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f5f5;
  flex-shrink: 0;
}
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 0;
  background: transparent;
}
.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: bounce 1.2s infinite ease-in-out;
}
.typing-indicator span:nth-child(2) {
  animation-delay: 0.15s;
}
.typing-indicator span:nth-child(3) {
  animation-delay: 0.3s;
}
@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.65);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
.chat-footer {
  flex-shrink: 0;
  padding: 14px 18px 18px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}
.composer {
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
.send-btn {
  border-radius: 10px;
  padding: 12px 22px;
  font-weight: 600;
}
.meta-warn {
  max-width: 880px;
  margin: 10px auto 0;
  font-size: 12px;
  color: #e6a23c;
}
</style>
