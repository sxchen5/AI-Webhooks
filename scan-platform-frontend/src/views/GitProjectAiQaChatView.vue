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
      <el-button
        v-if="project"
        text
        type="danger"
        :disabled="replying || !messages.length"
        @click="onClearAllChats"
      >
        清空记录
      </el-button>
    </header>

    <el-scrollbar ref="scrollbarRef" class="chat-scroll">
      <div class="messages">
        <div v-if="!messages.length && !historyLoading" class="welcome">
          <p class="welcome-title">开始对话</p>
          <p class="welcome-desc">
            Agent 使用 stream-json 时回复会随流式增量显示；完成后切换为 Markdown 渲染。
          </p>
        </div>
        <div
          v-for="(m, idx) in messages"
          :key="m.clientKey || m.id || idx"
          class="msg-block"
          :class="m.role === 'user' ? 'msg-block--user' : 'msg-block--bot'"
        >
          <div class="msg-row" :class="m.role === 'user' ? 'msg-row--user' : 'msg-row--bot'">
            <div v-if="m.role === 'user'" class="bubble bubble--user">
              <MarkdownOutputPanel :text="m.content" :rows="6" hide-toolbar />
            </div>
            <div v-else class="bubble bubble--assistant">
              <div v-if="m.displayStream" class="stream-plain">{{ m.content }}</div>
              <MarkdownOutputPanel v-else :text="m.content" :rows="8" hide-toolbar />
            </div>
          </div>
          <div v-if="m.role === 'user'" class="msg-actions msg-actions--user">
            <el-button
              v-if="m.id != null"
              text
              type="danger"
              class="icon-action"
              aria-label="删除用户消息"
              @click="onDeleteMessage(m)"
            >
              <el-icon :size="18"><Delete /></el-icon>
            </el-button>
          </div>
          <div
            v-else-if="!(replying && m.displayStream)"
            class="msg-actions msg-actions--bot assistant-toolbar"
          >
            <el-button text class="icon-action" aria-label="复制" @click="copyMessage(m.content)">
              <el-icon :size="18"><DocumentCopy /></el-icon>
            </el-button>
            <el-button
              text
              class="icon-action"
              aria-label="重新生成"
              :disabled="replying"
              @click="onRegenerateAssistant(m)"
            >
              <el-icon :size="18"><RefreshRight /></el-icon>
            </el-button>
            <el-button text class="icon-action" aria-label="朗读" @click="onSpeak(m.content)">
              <el-icon :size="18"><Headset /></el-icon>
            </el-button>
            <el-button text class="icon-action" aria-label="点赞" @click="onFeedback(m, true)">
              <el-icon :size="18"><Top /></el-icon>
            </el-button>
            <el-button text class="icon-action" aria-label="点踩" @click="onFeedback(m, false)">
              <el-icon :size="18"><Bottom /></el-icon>
            </el-button>
          </div>
        </div>
        <div v-if="replying" class="thinking-row">
          <div class="thinking-bubble" aria-hidden="true">
            <div class="typing-indicator">
              <span /><span /><span />
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>

    <footer class="chat-footer">
      <div class="composer">
        <div class="composer-input-wrap">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="4"
            :autosize="{ minRows: 4, maxRows: 10 }"
            placeholder="输入问题后发送…"
            :disabled="replying || !project"
            maxlength="8000"
            show-word-limit
            class="composer-textarea"
            @keydown.enter.exact.prevent="onEnterSend"
          />
          <div class="composer-model-inner">
            <el-select
              v-model="selectedModel"
              placeholder="默认"
              clearable
              size="small"
              fit-input-width
              class="model-select-inner"
              popper-class="git-qa-model-popper"
              :disabled="replying || !project"
            >
              <el-option
                v-for="opt in modelOptions"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
          </div>
        </div>
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
import { ArrowLeft, Bottom, ChatDotRound, Delete, DocumentCopy, Headset, RefreshRight, Top } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearAllGitQaChatMessages,
  deleteGitQaChatMessage,
  fetchGitQaChatMessages,
  getGitQaProject,
  streamGitQaChat,
} from '@/api/gitQaProject'
import MarkdownOutputPanel from '@/components/MarkdownOutputPanel.vue'

const route = useRoute()
const router = useRouter()
const project = ref(null)
const messages = ref([])
const draft = ref('')
const selectedModel = ref('')
const modelOptions = [
  'auto',
  'composer-2-fast',
  'composer-2',
  'composer-1.5',
  'grok-4-20',
  'grok-4-20-thinking',
  'kimi-k2.5',
]
const replying = ref(false)
const historyLoading = ref(false)
const scrollbarRef = ref(null)
const lastMeta = ref(null)

let abortCtrl = null
let sseBuffer = ''
let clientKeySeq = 0

function nextClientKey() {
  clientKeySeq += 1
  return `c-${clientKeySeq}`
}

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

async function copyMessage(text) {
  const t = text ?? ''
  try {
    await navigator.clipboard.writeText(t)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择文本')
  }
}

async function onClearAllChats() {
  if (!project.value?.id) return
  try {
    await ElMessageBox.confirm('确定清空该机器人的全部聊天记录？此操作不可恢复。', '清空记录', {
      type: 'warning',
    })
    await clearAllGitQaChatMessages(project.value.id)
    messages.value = []
    ElMessage.success('已清空')
  } catch {
    /* cancel */
  }
}

function findUserMessageBeforeAssistant(assistantMsg) {
  const i = messages.value.indexOf(assistantMsg)
  if (i <= 0) return null
  const prev = messages.value[i - 1]
  if (prev.role !== 'user' || prev.id == null) return null
  return prev
}

async function onRegenerateAssistant(assistantMsg) {
  if (replying.value) return
  const userRow = findUserMessageBeforeAssistant(assistantMsg)
  if (!userRow) {
    ElMessage.warning('找不到对应的用户问题，无法重新生成')
    return
  }
  assistantMsg.content = ''
  assistantMsg.displayStream = true
  assistantMsg.id = null
  lastMeta.value = null
  replying.value = true
  scrollToBottom()
  abortCtrl = new AbortController()
  const body = {
    question: userRow.content,
    regenerate: true,
    userMessageId: userRow.id,
    ...(selectedModel.value ? { model: selectedModel.value } : {}),
  }
  try {
    const res = await streamGitQaChat(project.value.id, body, abortCtrl.signal)
    const reader = res.body?.getReader()
    if (!reader) throw new Error('浏览器不支持流式响应')
    await consumeSseStream(reader, userRow, assistantMsg)
  } catch (e) {
    if (e?.name !== 'AbortError') {
      assistantMsg.content = `请求失败：${e?.message || String(e)}`
      ElMessage.error('执行失败')
    }
    assistantMsg.displayStream = false
    await loadHistory()
  } finally {
    replying.value = false
    abortCtrl = null
    assistantMsg.displayStream = false
    scrollToBottom()
  }
}

function onSpeak(text) {
  const t = (text ?? '').trim()
  if (!t) {
    ElMessage.warning('没有可朗读的内容')
    return
  }
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel()
    const u = new SpeechSynthesisUtterance(t)
    u.lang = 'zh-CN'
    window.speechSynthesis.speak(u)
    ElMessage.success('开始朗读')
  } else {
    ElMessage.warning('当前浏览器不支持语音朗读')
  }
}

function onFeedback(_m, positive) {
  ElMessage.success(positive ? '已记录点赞' : '已记录点踩')
}

async function onDeleteMessage(m) {
  if (m.id == null) return
  try {
    await ElMessageBox.confirm('确定删除这条消息？', '删除', { type: 'warning' })
    await deleteGitQaChatMessage(project.value.id, m.id)
    const i = messages.value.findIndex((x) => x === m)
    if (i >= 0) messages.value.splice(i, 1)
    ElMessage.success('已删除')
  } catch {
    /* cancel */
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

async function consumeSseStream(reader, userMsg, botMsg) {
  const decoder = new TextDecoder()
  sseBuffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    sseBuffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    let idx
    while ((idx = sseBuffer.indexOf('\n\n')) >= 0) {
      const raw = sseBuffer.slice(0, idx)
      sseBuffer = sseBuffer.slice(idx + 2)
      const { event, dataStr } = parseSseBlock(raw)
      if (!dataStr) continue
      try {
        const obj = JSON.parse(dataStr)
        if (event === 'meta' && obj.userMessageId != null) {
          userMsg.id = obj.userMessageId
        } else if (event === 'assistant' && obj.delta) {
          botMsg.content += obj.delta
          scrollToBottom()
        } else if (event === 'saved' && obj.assistantMessageId != null) {
          botMsg.id = obj.assistantMessageId
        } else if (event === 'error') {
          const msg = obj.message || '执行出错'
          botMsg.content += (botMsg.content ? '\n\n' : '') + `【错误】${msg}`
          if (obj.rawTail) {
            botMsg.content += `\n\n【原始输出尾部】\n${obj.rawTail}`
          }
          ElMessage.error(msg)
          await loadHistory()
        } else if (event === 'done') {
          lastMeta.value = {
            success: !!obj.success,
            exitCode: obj.exitCode ?? -1,
          }
          botMsg.displayStream = false
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

async function loadHistory() {
  const id = route.params.id
  historyLoading.value = true
  try {
    const page = await fetchGitQaChatMessages(id, 0, 500)
    const list = page.content || []
    messages.value = list.map((row) => ({
      id: row.id,
      role: row.role === 'USER' ? 'user' : 'assistant',
      content: row.content || '',
      displayStream: false,
      clientKey: nextClientKey(),
    }))
  } catch {
    ElMessage.warning('加载历史记录失败')
  } finally {
    historyLoading.value = false
    scrollToBottom()
  }
}

async function send() {
  const q = draft.value.trim()
  if (!q || replying.value || !project.value) return
  draft.value = ''
  const userMsg = { id: null, role: 'user', content: q, displayStream: false, clientKey: nextClientKey() }
  const botMsg = {
    id: null,
    role: 'assistant',
    content: '',
    displayStream: true,
    clientKey: nextClientKey(),
  }
  messages.value.push(userMsg, botMsg)
  lastMeta.value = null
  replying.value = true
  scrollToBottom()
  abortCtrl = new AbortController()
  try {
    const res = await streamGitQaChat(
      project.value.id,
      {
        question: q,
        ...(selectedModel.value ? { model: selectedModel.value } : {}),
      },
      abortCtrl.signal,
    )
    const reader = res.body?.getReader()
    if (!reader) {
      throw new Error('浏览器不支持流式响应')
    }
    await consumeSseStream(reader, userMsg, botMsg)
  } catch (e) {
    if (e?.name === 'AbortError') {
      botMsg.content += '\n\n【已取消】'
    } else {
      botMsg.content = `请求失败：${e?.message || String(e)}`
      ElMessage.error('执行失败')
    }
    botMsg.displayStream = false
  } finally {
    replying.value = false
    abortCtrl = null
    botMsg.displayStream = false
    scrollToBottom()
  }
}

onMounted(async () => {
  const id = route.params.id
  try {
    project.value = await getGitQaProject(id)
    await loadHistory()
  } catch {
    ElMessage.error('加载配置失败')
    goBack()
  }
})

onBeforeUnmount(() => {
  abortCtrl?.abort()
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel()
  }
})
</script>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 96px);
  max-height: calc(100vh - 72px);
  margin: -8px -8px 0;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
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
  flex: 1;
  min-width: 8px;
}
.chat-scroll {
  flex: 1;
  min-height: 0;
  padding: 0px 16px;
  background: #fff;
}
:deep(.chat-scroll .el-scrollbar__wrap) {
  overflow-x: hidden;
}
.messages {
  max-width: 880px;
  margin: 0 auto;
  padding-top: 16px;
  padding-bottom: 24px;
}
.msg-block {
  margin-bottom: 6px;
}
.msg-block--user .msg-actions {
  justify-content: flex-end;
}
.msg-block--bot .msg-actions {
  justify-content: flex-start;
}
.msg-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  margin-bottom: 10px;
  padding: 0 2px;
}
.assistant-toolbar .icon-action {
  padding: 6px 8px;
  min-height: auto;
  color: #606266;
}
.assistant-toolbar .icon-action:hover {
  color: #409eff;
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
  margin-bottom: 0;
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
  padding: 5px 14px;
}
.bubble--assistant {
  background: transparent;
  border: none;
  padding: 4px 0;
  width: 100%;
  max-width: 100%;
}
.stream-plain {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.55;
  color: #303133;
}
.bubble--user :deep(.md-output-panel) {
  margin: 0;
}
.bubble--user :deep(.md-preview) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  min-height: 0;
  max-height: none;
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
  justify-content: flex-start;
  margin-bottom: 12px;
}
.thinking-bubble {
  display: inline-flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 12px;
  background: #f5f5f5;
}
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
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
  padding: 12px 20px 20px;
}
.composer {
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  gap: 14px;
  align-items: center;
}
.composer-input-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
  border-radius: 16px;
  border: 1px solid #dcdfe6;
  background: #fff;
  overflow: visible;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.composer-model-inner {
  position: absolute;
  left: 10px;
  bottom: 30px;
  z-index: 2;
  pointer-events: auto;
}
.model-select-inner {
  max-width: calc(100% - 100px);
}
.model-select-inner :deep(.el-select__wrapper) {
  min-height: 28px;
  padding: 2px 10px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  background: rgba(255, 255, 255, 0.95);
  font-size: 12px;
  font-weight: 500;
  color: #606266;
}
.model-select-inner :deep(.el-select__wrapper.is-hovering),
.model-select-inner :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #c0c4cc inset, 0 2px 8px rgba(64, 158, 255, 0.12);
}
.composer-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  border-radius: 16px;
  resize: none;
  padding: 12px 14px 44px 14px;
  font-size: 14px;
  line-height: 1.55;
}
.composer-textarea :deep(.el-input__count) {
  background: transparent;
  bottom: 8px;
  right: 12px;
}
.send-btn {
  border-radius: 14px;
  padding: 14px 24px;
  font-weight: 600;
  flex-shrink: 0;
  align-self: center;
}
.meta-warn {
  max-width: 880px;
  margin: 10px auto 0;
  font-size: 12px;
  color: #e6a23c;
}
</style>

<style lang="scss">
/* 下拉挂载在 body，需非 scoped */
.git-qa-model-popper.el-popper {
  border-radius: 10px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.12);
}
</style>
