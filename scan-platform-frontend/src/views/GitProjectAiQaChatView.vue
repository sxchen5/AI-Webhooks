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
          <p class="welcome-desc">将基于当前仓库克隆目录执行 Agent，回复以打字机效果展示。</p>
        </div>
        <div
          v-for="(m, idx) in messages"
          :key="idx"
          class="msg-row"
          :class="m.role === 'user' ? 'msg-row--user' : 'msg-row--bot'"
        >
          <div v-if="m.role === 'assistant'" class="bubble-avatar bot">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="bubble" :class="m.role === 'user' ? 'bubble--user' : 'bubble--bot'">
            <MarkdownOutputPanel v-if="m.role === 'assistant'" :text="m.content" :rows="8" compact dark />
            <div v-else class="bubble-plain">{{ m.content }}</div>
          </div>
          <div v-if="m.role === 'user'" class="bubble-avatar user">
            <el-icon><User /></el-icon>
          </div>
        </div>
        <div v-if="replying" class="msg-row msg-row--bot typing-row">
          <div class="bubble-avatar bot"><el-icon><Cpu /></el-icon></div>
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
        Agent 退出码 {{ lastMeta.exitCode }}，请查看回复中的错误信息。
      </p>
    </footer>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChatDotRound, Cpu, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getGitQaProject, postGitQaChat } from '@/api/gitQaProject'
import MarkdownOutputPanel from '@/components/MarkdownOutputPanel.vue'

const route = useRoute()
const router = useRouter()
const project = ref(null)
const messages = ref([])
const draft = ref('')
const replying = ref(false)
const scrollbarRef = ref(null)
const lastMeta = ref(null)

function goBack() {
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

async function typewriter(targetMsg, fullText) {
  const text = fullText || ''
  const len = text.length
  if (len === 0) {
    targetMsg.content = ''
    return
  }
  const chunk = Math.max(1, Math.ceil(len / 800))
  const delay = len > 12000 ? 0 : 12
  for (let i = 0; i < len; i += chunk) {
    targetMsg.content = text.slice(0, Math.min(len, i + chunk))
    scrollToBottom()
    if (delay > 0) {
      // eslint-disable-next-line no-await-in-loop
      await new Promise((r) => setTimeout(r, delay))
    }
  }
  targetMsg.content = text
}

function onEnterSend() {
  if (!draft.value.includes('\n')) {
    send()
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
  try {
    const res = await postGitQaChat(project.value.id, q)
    lastMeta.value = {
      success: res.success,
      exitCode: res.exitCode,
    }
    await typewriter(botMsg, res.output || '')
    if (!res.success) {
      ElMessage.warning('Agent 执行未成功（退出码非 0）')
    }
  } catch (e) {
    botMsg.content = `请求失败：${e?.message || String(e)}`
    ElMessage.error('执行失败')
  } finally {
    replying.value = false
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
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  max-height: calc(100vh - 88px);
  margin: -8px -8px 0;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(165deg, #0f172a 0%, #1e293b 45%, #0f172a 100%);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.35);
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: rgba(15, 23, 42, 0.85);
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
  flex-shrink: 0;
}
.back-btn {
  color: #94a3b8 !important;
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
  background: linear-gradient(135deg, #38bdf8, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 8px 20px rgba(56, 189, 248, 0.35);
}
.header-text {
  min-width: 0;
}
.title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #f1f5f9;
  letter-spacing: 0.02em;
}
.subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
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
  color: #94a3b8;
}
.welcome-title {
  margin: 0 0 8px;
  font-size: 18px;
  color: #e2e8f0;
  font-weight: 600;
}
.welcome-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 18px;
}
.msg-row--user {
  justify-content: flex-end;
}
.msg-row--bot {
  justify-content: flex-start;
}
.bubble-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.bubble-avatar.bot {
  background: rgba(99, 102, 241, 0.35);
  color: #c7d2fe;
}
.bubble-avatar.user {
  background: rgba(56, 189, 248, 0.25);
  color: #7dd3fc;
}
.bubble {
  max-width: min(720px, calc(100% - 80px));
  border-radius: 16px;
  padding: 12px 14px;
  line-height: 1.55;
  font-size: 14px;
}
.bubble--user {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #f8fafc;
  border-bottom-right-radius: 4px;
  box-shadow: 0 10px 28px rgba(14, 165, 233, 0.25);
}
.bubble--bot {
  background: rgba(30, 41, 59, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-bottom-left-radius: 4px;
  color: #e2e8f0;
}
.bubble-plain {
  white-space: pre-wrap;
  word-break: break-word;
}
:deep(.bubble--bot .markdown-output-panel) {
  color: #e2e8f0;
}
.typing-row {
  margin-bottom: 8px;
}
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 12px 18px;
  background: rgba(30, 41, 59, 0.85);
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.1);
}
.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #64748b;
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
  background: rgba(15, 23, 42, 0.92);
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}
.composer {
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
.composer :deep(.el-textarea__inner) {
  background: rgba(30, 41, 59, 0.9);
  border-color: rgba(148, 163, 184, 0.2);
  color: #f1f5f9;
  border-radius: 12px;
  resize: none;
}
.composer :deep(.el-input__count) {
  background: transparent;
  color: #64748b;
}
.send-btn {
  border-radius: 12px;
  padding: 12px 22px;
  font-weight: 600;
}
.meta-warn {
  max-width: 880px;
  margin: 10px auto 0;
  font-size: 12px;
  color: #fbbf24;
}
</style>
