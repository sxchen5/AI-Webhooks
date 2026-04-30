<template>
  <div class="chat-page" :class="prefs.theme === 'dark' ? 'chat-page--dark' : 'chat-page--light'">
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

    <el-scrollbar ref="scrollbarRef" class="chat-scroll" @scroll="onScrollWrap">
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
          @mouseenter="hoveredMsgIndex = idx"
          @mouseleave="hoveredMsgIndex = null"
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
          <div
            v-if="m.role === 'user'"
            class="msg-actions msg-actions--user"
            :class="{ 'msg-actions--visible': showUserToolbar(idx) }"
          >
            <div class="msg-actions-inner">
              <el-tooltip content="复制" placement="top">
                <el-button text class="icon-action icon-action--copy" @click="copyMessage(m.content)">
                  <el-icon :size="msgActionIconSize"><DocumentCopy /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
          <div
            v-else-if="!(replying && m.displayStream)"
            class="msg-actions msg-actions--bot assistant-toolbar"
            :class="{ 'msg-actions--visible': showAssistantToolbar(idx, m) }"
          >
            <div class="msg-actions-inner">
              <el-tooltip content="复制" placement="top">
                <el-button text class="icon-action icon-action--copy" @click="copyMessage(m.content)">
                  <el-icon :size="msgActionIconSize"><DocumentCopy /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="重新生成" placement="top">
                <el-button text class="icon-action" :disabled="replying" @click="onRegenerateAssistant(m)">
                  <el-icon :size="msgActionIconSize"><RefreshRight /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip :content="isSpeakingThis(m) ? '停止朗读' : '朗读'" placement="top">
                <el-button text class="icon-action" @click="onSpeak(m)">
                  <el-icon :size="msgActionIconSize">
                    <VideoPlay v-if="isSpeakingThis(m)" />
                    <Headset v-else />
                  </el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="点赞" placement="top">
                <el-button
                  text
                  class="icon-action"
                  :class="{ 'icon-action--feedback-on': m.feedback === 1 }"
                  :disabled="replying || m.id == null"
                  @click="onFeedback(m, true)"
                >
                  <el-icon :size="msgActionIconSize"><IconThumbUp /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="点踩" placement="top">
                <el-button
                  text
                  class="icon-action"
                  :class="{ 'icon-action--feedback-on': m.feedback === -1 }"
                  :disabled="replying || m.id == null"
                  @click="onFeedback(m, false)"
                >
                  <el-icon :size="msgActionIconSize"><IconThumbDown /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
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
      <div class="footer-stack">
        <transition name="fade-slide">
          <div v-show="!isAtBottom" class="scroll-to-bottom-wrap">
            <el-tooltip content="回到底部" placement="top">
              <el-button circle class="scroll-to-bottom-btn" @click="scrollToBottomImmediate">
                <el-icon :size="18"><ArrowDown /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </transition>
        <div class="composer">
        <div class="composer-input-wrap">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="2"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="输入问题后发送…"
            :disabled="replying || !project"
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
              <template #prefix>
                <span class="model-prefix">选择模型</span>
              </template>
              <el-option v-for="opt in modelOptions" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </div>
          <el-tooltip content="发送" placement="left">
            <el-button
              type="primary"
              circle
              class="send-btn-inner"
              :loading="replying"
              :disabled="!draft.trim() || !project"
              @click="send"
            >
              <el-icon v-if="!replying" :size="msgActionIconSize"><Right /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        </div>
      </div>
      <p v-if="lastMeta && !lastMeta.success" class="meta-warn">
        Agent 退出码 {{ lastMeta.exitCode }}，请查看上方回复或错误提示。
      </p>
    </footer>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  ArrowLeft,
  ChatDotRound,
  DocumentCopy,
  Headset,
  RefreshRight,
  Right,
  VideoPlay,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearAllGitQaChatMessages,
  fetchGitQaChatMessages,
  getGitQaProject,
  patchGitQaChatMessageFeedback,
  streamGitQaChat,
} from '@/api/gitQaProject'
import MarkdownOutputPanel from '@/components/MarkdownOutputPanel.vue'
import { usePreferencesStore } from '@/stores/preferences'

/** 线框大拇指朝上 */
const IconThumbUp = () =>
  h(
    'svg',
    {
      xmlns: 'http://www.w3.org/2000/svg',
      viewBox: '0 0 24 24',
      width: '1em',
      height: '1em',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '1.75',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
    },
    [
      h('path', { d: 'M7 11v8a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-8H7z' }),
      h('path', {
        d: 'M11 10.5V18a1 1 0 0 0 1 1h4.5a2 2 0 0 0 1.9-1.4l1.6-5.5a1.5 1.5 0 0 0-1.4-1.9H15l.4-2.2A2 2 0 0 0 13.5 5 1.5 1.5 0 0 0 12 6.5V10l-1 .5z',
      }),
    ],
  )

/** 线框大拇指朝下（与朝上对称） */
const IconThumbDown = () =>
  h(
    'svg',
    {
      xmlns: 'http://www.w3.org/2000/svg',
      viewBox: '0 0 24 24',
      width: '1em',
      height: '1em',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '1.75',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
    },
    [
      h('g', { transform: 'translate(12 12) scale(1 -1) translate(-12 -12)' }, [
        h('path', { d: 'M7 11v8a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-8H7z' }),
        h('path', {
          d: 'M11 10.5V18a1 1 0 0 0 1 1h4.5a2 2 0 0 0 1.9-1.4l1.6-5.5a1.5 1.5 0 0 0-1.4-1.9H15l.4-2.2A2 2 0 0 0 13.5 5 1.5 1.5 0 0 0 12 6.5V10l-1 .5z',
        }),
      ]),
    ],
  )

/** 消息下方工具栏与回到底部等操作图标统一尺寸 */
const msgActionIconSize = 15

const route = useRoute()
const router = useRouter()
const prefs = usePreferencesStore()
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
const hoveredMsgIndex = ref(null)
/** 距底部小于该像素视为在底部，避免浮点抖动 */
const scrollBottomTolerance = 48
const isAtBottom = ref(true)

/** 正在朗读的助手消息 clientKey（与气泡一一对应） */
const speakingClientKey = ref(null)

let abortCtrl = null
let sseBuffer = ''
let clientKeySeq = 0

const lastAssistantIndex = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'assistant') return i
  }
  return -1
})

const lastUserIndex = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') return i
  }
  return -1
})

function assistantToolbarEligible(m) {
  return !(replying.value && m.displayStream)
}

function showAssistantToolbar(idx, m) {
  if (!assistantToolbarEligible(m)) return false
  if (idx === lastAssistantIndex.value) return true
  return hoveredMsgIndex.value === idx
}

function showUserToolbar(idx) {
  if (idx === lastUserIndex.value) return true
  return hoveredMsgIndex.value === idx
}

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
      updateAtBottomFromWrap()
    }
  })
}

function updateAtBottomFromWrap() {
  const wrap = scrollbarRef.value?.wrapRef
  if (!wrap) {
    isAtBottom.value = true
    return
  }
  const { scrollTop, clientHeight, scrollHeight } = wrap
  isAtBottom.value = scrollTop + clientHeight >= scrollHeight - scrollBottomTolerance
}

function onScrollWrap() {
  updateAtBottomFromWrap()
}

function scrollToBottomImmediate() {
  const wrap = scrollbarRef.value?.wrapRef
  if (wrap) {
    wrap.scrollTop = wrap.scrollHeight
  }
  nextTick(() => updateAtBottomFromWrap())
}

watch(
  () => [messages.value.length, replying.value, historyLoading.value],
  () => {
    nextTick(() => updateAtBottomFromWrap())
  },
  { flush: 'post' },
)

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
  assistantMsg.feedback = null
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

function isSpeakingThis(m) {
  return m.role === 'assistant' && speakingClientKey.value != null && speakingClientKey.value === m.clientKey
}

function cancelSpeech() {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
  speakingClientKey.value = null
}

function onSpeak(m) {
  if (m.role !== 'assistant') return
  if (isSpeakingThis(m)) {
    cancelSpeech()
    return
  }
  const text = (m.content ?? '').trim()
  if (!text) {
    ElMessage.warning('没有可朗读的内容')
    return
  }
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    ElMessage.warning('当前浏览器不支持语音朗读')
    return
  }
  cancelSpeech()
  const key = m.clientKey
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'zh-CN'
  u.onend = () => {
    if (speakingClientKey.value === key) speakingClientKey.value = null
  }
  u.onerror = () => {
    if (speakingClientKey.value === key) speakingClientKey.value = null
  }
  speakingClientKey.value = key
  window.speechSynthesis.speak(u)
  ElMessage.success('开始朗读')
}

async function onFeedback(m, positive) {
  if (!project.value?.id || m.id == null || m.role !== 'assistant') return
  const nextVal = positive ? 1 : -1
  const body = m.feedback === nextVal ? null : nextVal
  try {
    await patchGitQaChatMessageFeedback(project.value.id, m.id, body)
    m.feedback = body
    ElMessage.success(body == null ? '已取消反馈' : body === 1 ? '已点赞' : '已点踩')
  } catch {
    /* axios 拦截器已提示错误 */
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
      feedback: row.role === 'ASSISTANT' ? row.feedback ?? null : null,
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
    feedback: null,
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
    nextTick(() => updateAtBottomFromWrap())
  } catch {
    ElMessage.error('加载配置失败')
    goBack()
  }
})

onBeforeUnmount(() => {
  abortCtrl?.abort()
  cancelSpeech()
})
</script>

<style scoped lang="scss">
.chat-page {
  --chat-surface: #ffffff;
  --chat-header-bg: #fafafa;
  --chat-header-border: #ebeef5;
  --chat-scroll-bg: #ffffff;
  --chat-title: #303133;
  --chat-subtitle: #909399;
  --chat-border: #e4e7ed;
  --chat-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
  --chat-user-bubble: #f5f5f5;
  --chat-body-text: #303133;
  --chat-muted: #606266;
  --chat-welcome-title: #303133;
  --chat-thinking-bg: #f5f5f5;
  --chat-dot: #c0c4cc;
  --chat-composer-border: #dcdfe6;
  --chat-composer-bg: #ffffff;
  --chat-model-text: #606266;
  --chat-model-hint: #909399;
  --chat-toolbar-icon: #606266;
  --chat-toolbar-copy: #7f7f7f;

  display: flex;
  flex-direction: column;
  height: calc(100vh - 96px);
  max-height: calc(100vh - 72px);
  margin: -8px -8px 0;
  border-radius: 20px;
  overflow: hidden;
  background: var(--chat-surface);
  border: 1px solid var(--chat-border);
  box-shadow: var(--chat-shadow);
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.chat-page--dark {
  --chat-surface: var(--el-bg-color, #141414);
  --chat-header-bg: var(--el-fill-color-darker, #1a1a1a);
  --chat-header-border: var(--el-border-color, #303030);
  --chat-scroll-bg: var(--el-bg-color, #141414);
  --chat-title: var(--el-text-color-primary, #e5e5e5);
  --chat-subtitle: var(--el-text-color-secondary, #a3a3a3);
  --chat-border: var(--el-border-color, #303030);
  --chat-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
  --chat-user-bubble: var(--el-fill-color-dark, #262626);
  --chat-body-text: var(--el-text-color-primary, #e5eaf3);
  --chat-muted: var(--el-text-color-secondary, #a3a3a3);
  --chat-welcome-title: var(--el-text-color-primary, #e5eaf3);
  --chat-thinking-bg: var(--el-fill-color-dark, #262626);
  --chat-dot: var(--el-text-color-placeholder, #6b7280);
  --chat-composer-border: var(--el-border-color, #404040);
  --chat-composer-bg: var(--el-fill-color-darker, #1a1a1a);
  --chat-model-text: var(--el-text-color-regular, #c0c4cc);
  --chat-model-hint: var(--el-text-color-secondary, #909399);
  --chat-toolbar-icon: var(--el-text-color-secondary, #a3a3a3);
  --chat-toolbar-copy: #9ca3af;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: var(--chat-header-bg);
  border-bottom: 1px solid var(--chat-header-border);
  flex-shrink: 0;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
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
  color: var(--chat-title);
}
.subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--chat-subtitle);
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
  background: var(--chat-scroll-bg);
  transition: background 0.2s ease;
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
  min-height: 36px;
  display: flex;
  align-items: center;
  margin-top: 2px;
  margin-bottom: 8px;
  padding: 0 2px;
}
.msg-actions-inner {
  display: flex;
  align-items: center;
  gap: 0;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
}
.msg-actions--visible .msg-actions-inner {
  opacity: 1;
  pointer-events: auto;
}
.msg-actions .icon-action {
  padding: 2px 4px;
  min-height: auto;
}
.icon-action--copy {
  color: var(--chat-toolbar-copy);
}
.icon-action--copy:hover {
  color: #409eff;
}
.assistant-toolbar .icon-action {
  color: var(--chat-toolbar-icon);
}
.assistant-toolbar .icon-action:hover {
  color: #409eff;
}
.assistant-toolbar .icon-action--feedback-on {
  color: #409eff;
}
.assistant-toolbar .icon-action--feedback-on:hover {
  color: #66b1ff;
}
.welcome {
  text-align: center;
  padding: 32px 16px 8px;
  color: var(--chat-muted);
}
.welcome-title {
  margin: 0 0 8px;
  font-size: 18px;
  color: var(--chat-welcome-title);
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
  background: var(--chat-user-bubble);
  color: var(--chat-body-text);
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
  color: var(--chat-body-text);
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
  background: var(--chat-thinking-bg);
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
  background: var(--chat-dot);
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
.footer-stack {
  position: relative;
  max-width: 880px;
  margin: 0 auto;
}
.scroll-to-bottom-wrap {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  z-index: 4;
}
.scroll-to-bottom-btn {
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.12);
}
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(6px);
}
.composer {
  max-width: none;
  margin: 0;
}
.composer-input-wrap {
  position: relative;
  border-radius: 16px;
  border: 1px solid var(--chat-composer-border);
  background: var(--chat-composer-bg);
  overflow: visible;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.composer-model-inner {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  pointer-events: auto;
}
.model-prefix {
  font-size: 12px;
  color: var(--chat-model-hint);
  white-space: nowrap;
  user-select: none;
  margin-right: 4px;
}
.model-select-inner {
  max-width: calc(100% - 140px);
}
.model-select-inner :deep(.el-select__prefix) {
  border: none;
  box-shadow: none;
}
.model-select-inner :deep(.el-select__wrapper) {
  min-height: 28px;
  padding: 2px 8px;
  border-radius: 8px;
  box-shadow: none !important;
  border: none !important;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: var(--chat-model-text);
}
.model-select-inner :deep(.el-select__wrapper.is-hovering),
.model-select-inner :deep(.el-select__wrapper.is-focused) {
  background: rgba(64, 158, 255, 0.06);
}
.composer-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  border-radius: 16px;
  resize: none;
  padding: 12px 52px 40px 12px;
  font-size: 14px;
  line-height: 1.55;
}
.composer-textarea :deep(.el-input__count) {
  display: none;
}
.send-btn-inner {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 3;
  width: 40px;
  height: 40px;
}
.meta-warn {
  max-width: 880px;
  margin: 10px auto 0;
  font-size: 12px;
  color: #e6a23c;
}
</style>

<style lang="scss">
.git-qa-model-popper.el-popper {
  border-radius: 10px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.12);
}
</style>
