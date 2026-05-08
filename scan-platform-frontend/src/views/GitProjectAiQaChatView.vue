<template>
  <div class="chat-page" :class="prefs.theme === 'dark' ? 'chat-page--dark' : 'chat-page--light'">
    <header class="chat-header">
      <el-button class="back-btn" text type="primary" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回配置
      </el-button>
      <div class="header-center">
        <div class="bot-avatar" aria-hidden="true">
          <el-icon :size="20"><ChatDotRound /></el-icon>
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

    <div class="chat-body-row">
      <el-scrollbar
        ref="scrollbarRef"
        class="chat-scroll chat-scroll--grow"
        :class="{ 'chat-scroll--toc-pad': tocColumnVisible && tocOpen, 'chat-scroll--toc-pad-collapsed': tocColumnVisible && !tocOpen }"
        wrap-class="git-qa-scroll-wrap"
        @scroll="onScrollWrap"
      >
      <div v-if="!messages.length && !historyLoading" class="welcome-full">
        <p class="welcome-title">{{ t('gitQaChat.welcomeEmpty') }}</p>
      </div>
      <div v-else class="messages">
        <div
          v-for="(m, idx) in messages"
          :key="m.clientKey || m.id || idx"
          class="msg-block"
          :ref="(el) => setMsgBlockRef(messageAnchorKey(m), el)"
          :class="m.role === 'user' ? 'msg-block--user' : 'msg-block--bot'"
          @mouseenter="hoveredMsgIndex = idx"
          @mouseleave="hoveredMsgIndex = null"
        >
          <div class="msg-row" :class="m.role === 'user' ? 'msg-row--user' : 'msg-row--bot'">
            <div v-if="m.role === 'user'" class="bubble bubble--user">
              <div v-if="m.attachments?.length" class="user-attach-row">
                <el-tag v-for="a in m.attachments" :key="a.name" size="small" type="info" effect="plain" class="user-attach-tag">
                  {{ a.name }}
                </el-tag>
              </div>
              <MarkdownOutputPanel :text="userBubbleBody(m)" :rows="6" hide-toolbar />
            </div>
            <div v-else class="bubble bubble--assistant">
              <div v-if="assistantThinkingPreview(m)" class="assistant-thinking-fold">
                <button
                  type="button"
                  class="thinking-fold-toggle"
                  @click="m.thinkingOpen = !m.thinkingOpen"
                >
                  <el-icon class="thinking-fold-caret" :class="{ 'thinking-fold-caret--open': m.thinkingOpen }">
                    <ArrowDown />
                  </el-icon>
                  <span>{{ m.thinkingOpen ? t('gitQaChat.thinkingCollapse') : t('gitQaChat.thinkingExpand') }}</span>
                </button>
                <div v-show="m.thinkingOpen" class="thinking-fold-body">
                  <pre>{{ assistantThinkingPreview(m) }}</pre>
                </div>
              </div>
              <div v-if="m.displayStream" class="stream-plain">{{ m.streamPlain != null ? m.streamPlain : m.content }}</div>
              <div
                v-else
                class="bubble-md"
                :class="{ 'bubble-md--dark': prefs.theme === 'dark' }"
                v-html="assistantRendered[messageAnchorKey(m)]?.html || ''"
              />
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
                <el-button text class="icon-action icon-action--copy" @click="copyMessage(assistantCopyBody(m))">
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

      <div v-show="tocColumnVisible" class="toc-float" :class="{ 'toc-float--collapsed': !tocOpen }">
        <div v-if="tocOpen" class="toc-panel">
          <div class="toc-panel-header">
            <span class="toc-title">{{ t('gitQaChat.outline') }}</span>
            <el-button text type="primary" size="small" @click="tocOpen = false">{{ t('gitQaChat.outlineClose') }}</el-button>
          </div>
          <div v-if="!tocItems.length" class="toc-empty">{{ t('gitQaChat.noOutline') }}</div>
          <nav v-else class="toc-nav" :aria-label="t('gitQaChat.outline')">
            <button
              v-for="h in tocItems"
              :key="h.id"
              type="button"
              class="toc-item"
              :class="{ 'toc-item--active': h.id === activeTocId }"
              :style="{ paddingLeft: `${8 + (h.level - 1) * 12}px` }"
              @click="onTocNavigate(h.id)"
            >
              {{ h.text }}
            </button>
          </nav>
        </div>
        <button v-else type="button" class="toc-reopen" @click="tocOpen = true" :title="t('gitQaChat.outlineOpen')">
          {{ t('gitQaChat.outline') }}
        </button>
      </div>
    </div>

    <footer class="chat-footer">
      <div class="footer-stack">
        <transition name="fade-slide">
          <div v-show="!isAtBottom" class="scroll-to-bottom-wrap">
            <el-tooltip :content="t('gitQaChat.scrollBottom')" placement="top">
              <el-button circle class="scroll-to-bottom-btn" @click="scrollToBottomImmediate">
                <el-icon :size="18"><ArrowDown /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </transition>
        <div class="composer">
        <div class="composer-input-wrap">
          <input
            ref="fileInputRef"
            type="file"
            class="composer-file-input"
            multiple
            accept=".txt,.md,.markdown,.json,.csv,.log,.xml,.yaml,.yml,.properties,.env,.java,.ts,.tsx,.js,.mjs,.jsx,.vue,.css,.scss,.html,.sql,.py,.go,.rs,.c,.h,.cpp,.hpp,.cs,.kt,.swift,.rb,.php,.sh,.bat,.ps1,.gradle,.kts,.toml,.ini"
            @change="onFilesSelected"
          />
          <div v-if="pendingFiles.length" class="pending-files">
            <el-tag
              v-for="(f, fi) in pendingFiles"
              :key="f.id"
              closable
              type="info"
              effect="plain"
              class="pending-file-tag"
              @close="removePendingFile(fi)"
            >
              {{ f.name }}
            </el-tag>
          </div>
          <div class="composer-textarea-stack">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 6 }"
              :placeholder="t('gitQaChat.inputPlaceholder')"
              :disabled="replying || !project"
              class="composer-textarea"
              @keydown.enter.exact.prevent="onEnterSend"
            />
            <div class="composer-inner-bar">
              <div class="composer-model-inner">
                <span class="model-hint-text">{{
                  selectedModel ? t('gitQaChat.modelPrefix', { name: selectedModel }) : t('gitQaChat.selectModel')
                }}</span>
                <el-select
                  v-model="selectedModel"
                  :placeholder="t('gitQaChat.modelDefault')"
                  clearable
                  size="small"
                  class="model-select-inner"
                  popper-class="git-qa-model-popper"
                  :disabled="replying || !project"
                >
                  <el-option v-for="opt in modelOptions" :key="opt" :label="opt" :value="opt" />
                </el-select>
              </div>
              <div class="composer-actions-right">
                <el-tooltip :content="t('gitQaChat.uploadFile')" placement="top">
                  <el-button text class="composer-icon-btn" :disabled="replying || !project" @click="triggerFilePick">
                    <el-icon :size="20"><Upload /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip :content="isRecording ? t('gitQaChat.stopVoice') : t('gitQaChat.startVoice')" placement="top">
                  <el-button text class="composer-icon-btn" :disabled="replying || !project" @click="toggleVoiceInput">
                    <el-icon v-if="!isRecording" :size="20"><Microphone /></el-icon>
                    <el-icon v-else :size="20"><VideoPause /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="showSendFab" :content="replying ? t('gitQaChat.stopGenerate') : t('gitQaChat.send')" placement="top">
                  <el-button circle class="composer-send-fab" :disabled="!project" @click="replying ? stopReply() : send()">
                    <el-icon v-if="!replying" :size="16"><Top /></el-icon>
                    <el-icon v-else :size="16"><IconStopGenerate /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
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
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, shallowReactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowDown,
  ArrowLeft,
  ChatDotRound,
  DocumentCopy,
  Headset,
  Microphone,
  RefreshRight,
  Top,
  Upload,
  VideoPause,
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
import { AGENT_MODEL_OPTIONS } from '@/constants/agentModels'
import { extractMarkdownHeadings, renderMarkdownWithAnchors } from '@/utils/markdownAnchors'
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

/** 停止生成：圆形按钮上的白圆角方块（示意黑底圆 + 白停止块） */
const IconStopGenerate = () =>
  h(
    'svg',
    {
      xmlns: 'http://www.w3.org/2000/svg',
      viewBox: '0 0 16 16',
      width: '1em',
      height: '1em',
      display: 'block',
    },
    [h('rect', { x: 3.5, y: 3.5, width: 9, height: 9, rx: 1.5, fill: 'currentColor' })],
  )

/** 消息下方工具栏与回到底部等操作图标统一尺寸 */
const msgActionIconSize = 15

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const prefs = usePreferencesStore()
const project = ref(null)
const messages = ref([])
const draft = ref('')
const fileInputRef = ref(null)
const pendingFiles = ref([])
let pendingFileSeq = 0
const MAX_FILE_BYTES = 400 * 1024
const isRecording = ref(false)
let speechRec = null

const GITQA_ATTACH = /^<!--gitqa-attach:(\[.*?\])-->\s*\n?/

const GITQA_THINK_BEGIN = '<!--GITQA_THINKING_BEGIN-->'
const GITQA_THINK_END = '<!--GITQA_THINKING_END-->'

function splitAssistantStored(raw) {
  const s = raw || ''
  const i = s.indexOf(GITQA_THINK_BEGIN)
  const j = s.indexOf(GITQA_THINK_END)
  if (i >= 0 && j > i) {
    const thinking = s.slice(i + GITQA_THINK_BEGIN.length, j).replace(/^\n+|\n+$/g, '').trim()
    const body = s.slice(j + GITQA_THINK_END.length).replace(/^\n+/, '')
    return { thinking, body }
  }
  return { thinking: '', body: s }
}

function assistantThinkingPreview(m) {
  if (m?.role !== 'assistant') return ''
  return String(m.thinking || '').trim()
}

function assistantCopyBody(m) {
  return (m?.content ?? '').trim()
}

const showSendFab = computed(
  () => messages.value.length > 0 || draft.value.trim().length > 0 || pendingFiles.value.length > 0,
)

function parseUserAttachmentsRow(raw) {
  const s = raw || ''
  const m = s.match(GITQA_ATTACH)
  if (!m) return { attachments: [], body: s }
  try {
    const arr = JSON.parse(m[1])
    const attachments = Array.isArray(arr) ? arr.filter((x) => x && x.name) : []
    return { attachments, body: s.slice(m[0].length) }
  } catch {
    return { attachments: [], body: s }
  }
}

function userBubbleBody(m) {
  if (m?.role !== 'user') return m?.content || ''
  return parseUserAttachmentsRow(m.content).body
}

function buildQuestionForApi(text, files) {
  const tx = (text || '').trim()
  const parts = []
  if (tx) parts.push(tx)
  const attLabel = t('gitQaChat.attachmentSection')
  for (const f of files) {
    parts.push(`### ${attLabel}: ${f.name}\n\n\`\`\`\n${f.text}\n\`\`\``)
  }
  return parts.join('\n\n')
}

function triggerFilePick() {
  fileInputRef.value?.click()
}

function removePendingFile(idx) {
  pendingFiles.value.splice(idx, 1)
}

function readFileAsUtf8(file) {
  return new Promise((resolve, reject) => {
    const fr = new FileReader()
    fr.onload = () => resolve(String(fr.result || ''))
    fr.onerror = () => reject(fr.error)
    fr.readAsText(file, 'UTF-8')
  })
}

async function onFilesSelected(ev) {
  const input = ev.target
  const list = input?.files ? Array.from(input.files) : []
  if (input) input.value = ''
  for (const file of list) {
    if (file.size > MAX_FILE_BYTES) {
      ElMessage.warning(t('gitQaChat.fileTooLarge', { name: file.name }))
      continue
    }
    try {
      const text = await readFileAsUtf8(file)
      pendingFileSeq += 1
      pendingFiles.value.push({ id: `pf-${pendingFileSeq}`, name: file.name, text })
    } catch {
      ElMessage.error(t('gitQaChat.fileReadFail', { name: file.name }))
    }
  }
}

function stopReply() {
  abortCtrl?.abort()
}

function toggleVoiceInput() {
  const SR = typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition)
  if (!SR) {
    ElMessage.warning(t('gitQaChat.voiceUnsupported'))
    return
  }
  if (isRecording.value) {
    try {
      speechRec?.stop()
    } catch {
      /* */
    }
    isRecording.value = false
    return
  }
  speechRec = new SR()
  speechRec.lang = prefs.locale === 'en' ? 'en-US' : 'zh-CN'
  speechRec.continuous = true
  speechRec.interimResults = true
  speechRec.onresult = (event) => {
    let chunk = ''
    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      chunk += event.results[i][0].transcript
    }
    if (chunk) draft.value += chunk
  }
  speechRec.onerror = () => {
    isRecording.value = false
  }
  speechRec.onend = () => {
    isRecording.value = false
  }
  try {
    speechRec.start()
    isRecording.value = true
  } catch {
    ElMessage.error(t('gitQaChat.voiceStartFail'))
    isRecording.value = false
  }
}

const selectedModel = ref(undefined)
const modelOptions = AGENT_MODEL_OPTIONS
const replying = ref(false)
const historyLoading = ref(false)
const scrollbarRef = ref(null)
const msgBlockRefs = new Map()
const assistantRendered = shallowReactive({})
const activeAssistantKey = ref('')
const activeTocId = ref('')
const tocOpen = ref(true)

function messageAnchorKey(m) {
  if (m?.clientKey) return String(m.clientKey)
  if (m?.id != null) return `id-${m.id}`
  return ''
}

function setMsgBlockRef(key, el) {
  const k = key ? String(key) : ''
  if (!k) return
  if (el) msgBlockRefs.set(k, el)
  else msgBlockRefs.delete(k)
}

function pickDefaultAssistantTocKey() {
  for (const m of messages.value) {
    if (m.role !== 'assistant' || m.displayStream) continue
    if (extractMarkdownHeadings(m.content || '').length) return messageAnchorKey(m)
  }
  return ''
}

function rebuildAssistantRendered() {
  for (const k of Object.keys(assistantRendered)) {
    delete assistantRendered[k]
  }
  for (const m of messages.value) {
    if (m.role !== 'assistant' || m.displayStream) continue
    const k = messageAnchorKey(m)
    if (!k || !(m.content && String(m.content).trim())) continue
    const prefix = `cq-${k}`
    const { html, headings } = renderMarkdownWithAnchors(m.content || '', prefix)
    assistantRendered[k] = { html, headings }
  }
  const cur = activeAssistantKey.value
  const curOk =
    cur &&
    messages.value.some((m) => m.role === 'assistant' && !m.displayStream && messageAnchorKey(m) === cur)
  if (!curOk) {
    activeAssistantKey.value = pickDefaultAssistantTocKey()
  }
}

function updateActiveAssistantFromScroll() {
  const wrap = scrollbarRef.value?.wrapRef
  if (!wrap) return
  const rootRect = wrap.getBoundingClientRect()
  const focusY = rootRect.top + rootRect.height * 0.34
  let bestKey = ''
  let bestScore = -1
  for (const m of messages.value) {
    if (m.role !== 'assistant' || m.displayStream) continue
    if (!extractMarkdownHeadings(m.content || '').length) continue
    const k = messageAnchorKey(m)
    const el = msgBlockRefs.get(k)
    if (!el) continue
    const r = el.getBoundingClientRect()
    const visibleTop = Math.max(r.top, rootRect.top)
    const visibleBottom = Math.min(r.bottom, rootRect.bottom)
    const visibleH = Math.max(0, visibleBottom - visibleTop)
    if (visibleH < 20) continue
    const mid = (r.top + r.bottom) / 2
    const dist = Math.abs(mid - focusY)
    const score = visibleH * 1.2 - dist * 0.12
    if (score > bestScore) {
      bestScore = score
      bestKey = k
    }
  }
  if (bestKey) {
    activeAssistantKey.value = bestKey
  }
}

function onTocNavigate(headingId) {
  activeTocId.value = headingId
  const wrap = scrollbarRef.value?.wrapRef
  if (!wrap) return
  const esc =
    typeof CSS !== 'undefined' && CSS.escape ? CSS.escape(headingId) : String(headingId).replace(/[^a-zA-Z0-9_-]/g, '')
  const el = wrap.querySelector(`#${esc}`)
  if (!el || !(el instanceof HTMLElement)) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  el.classList.remove('md-heading--toc-flash')
  void el.offsetWidth
  el.classList.add('md-heading--toc-flash')
  window.setTimeout(() => el.classList.remove('md-heading--toc-flash'), 2200)
}

const tocItems = computed(() => {
  const k = activeAssistantKey.value
  if (!k) return []
  return assistantRendered[k]?.headings ?? []
})

const tocColumnVisible = computed(() =>
  messages.value.some((m) => {
    if (m.role !== 'assistant' || m.displayStream) return false
    return extractMarkdownHeadings(m.content || '').length > 0
  }),
)

watch(activeAssistantKey, () => {
  activeTocId.value = ''
})

watch(
  () =>
    messages.value
      .map((m) => `${messageAnchorKey(m)}|${m.role}|${!!m.displayStream}|${m.content ?? ''}`)
      .join('\n'),
  () => {
    rebuildAssistantRendered()
    nextTick(() => updateActiveAssistantFromScroll())
  },
  { flush: 'post' },
)

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
/** 流式展示打字机：content 为完整文本，streamPlain 为当前显示前缀 */
const streamAnimTimers = new Map()

function stopStreamAnim(botMsg) {
  const h = streamAnimTimers.get(botMsg)
  if (h != null) {
    cancelAnimationFrame(h)
    streamAnimTimers.delete(botMsg)
  }
  if (botMsg && botMsg.displayStream === false && botMsg.content != null) {
    botMsg.streamPlain = botMsg.content
  }
}

function scheduleStreamAnim(botMsg) {
  if (!botMsg?.displayStream) return
  if (streamAnimTimers.has(botMsg)) return
  const step = () => {
    const full = botMsg.content || ''
    let shown = botMsg.streamPlain ?? ''
    if (shown.length >= full.length) {
      botMsg.streamPlain = full
      streamAnimTimers.delete(botMsg)
      return
    }
    const jump = Math.max(1, Math.ceil((full.length - shown.length) / 40))
    botMsg.streamPlain = full.slice(0, Math.min(full.length, shown.length + jump))
    streamAnimTimers.set(botMsg, requestAnimationFrame(step))
  }
  streamAnimTimers.set(botMsg, requestAnimationFrame(step))
}

function appendThinkingDelta(botMsg, delta) {
  const d = delta ?? ''
  if (!d) return
  botMsg.thinking = (botMsg.thinking || '') + d
}

function appendStreamDelta(botMsg, delta) {
  const d = delta ?? ''
  if (!d) return
  botMsg.content = (botMsg.content || '') + d
  if (botMsg.displayStream) {
    scheduleStreamAnim(botMsg)
  }
}

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

function scrollToBottomAnimated() {
  const wrap = scrollbarRef.value?.wrapRef
  if (!wrap) return
  const target = Math.max(0, wrap.scrollHeight - wrap.clientHeight)
  const start = wrap.scrollTop
  const dist = target - start
  if (dist <= 2) {
    wrap.scrollTop = target
    updateAtBottomFromWrap()
    updateActiveAssistantFromScroll()
    return
  }
  const duration = 400
  const t0 = performance.now()
  const ease = (x) => 1 - (1 - x) ** 3
  function step(now) {
    const p = Math.min(1, (now - t0) / duration)
    wrap.scrollTop = start + dist * ease(p)
    if (p < 1) {
      requestAnimationFrame(step)
    } else {
      updateAtBottomFromWrap()
      updateActiveAssistantFromScroll()
    }
  }
  requestAnimationFrame(step)
}

function scrollToBottom() {
  nextTick(() => scrollToBottomAnimated())
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
  updateActiveAssistantFromScroll()
}

function scrollToBottomImmediate() {
  scrollToBottomAnimated()
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
    if (!replying.value && showSendFab.value) send()
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
  const idx = messages.value.indexOf(assistantMsg)
  if (idx < 0) return
  const q = (userRow.content ?? '').trim()
  if (!q) {
    ElMessage.warning('原问题为空，无法重新生成')
    return
  }
  const newUserMsg = {
    id: null,
    role: 'user',
    content: userRow.content,
    attachments: parseUserAttachmentsRow(userRow.content ?? '').attachments,
    displayStream: false,
    streamPlain: null,
    clientKey: nextClientKey(),
  }
  const newBotMsg = {
    id: null,
    role: 'assistant',
    content: '',
    thinking: '',
    thinkingOpen: false,
    displayStream: true,
    streamPlain: null,
    feedback: null,
    clientKey: nextClientKey(),
  }
  messages.value.splice(idx + 1, 0, newUserMsg, newBotMsg)
  lastMeta.value = null
  replying.value = true
  scrollToBottom()
  abortCtrl = new AbortController()
  const body = {
    question: q,
    regenerate: true,
    userMessageId: userRow.id,
    ...(selectedModel.value ? { model: selectedModel.value } : {}),
  }
  try {
    const res = await streamGitQaChat(project.value.id, body, abortCtrl.signal)
    const reader = res.body?.getReader()
    if (!reader) throw new Error('浏览器不支持流式响应')
    await consumeSseStream(reader, newUserMsg, newBotMsg)
  } catch (e) {
    if (e?.name !== 'AbortError') {
      newBotMsg.content = `请求失败：${e?.message || String(e)}`
      ElMessage.error('执行失败')
    }
    newBotMsg.displayStream = false
    stopStreamAnim(newBotMsg)
    newBotMsg.streamPlain = null
    await loadHistory()
  } finally {
    replying.value = false
    abortCtrl = null
    stopStreamAnim(newBotMsg)
    newBotMsg.displayStream = false
    newBotMsg.streamPlain = null
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
        } else if (event === 'thinking' && obj.delta) {
          appendThinkingDelta(botMsg, obj.delta)
          scrollToBottom()
        } else if (event === 'assistant' && obj.delta) {
          appendStreamDelta(botMsg, obj.delta)
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
          stopStreamAnim(botMsg)
          botMsg.displayStream = false
          botMsg.streamPlain = null
          await loadHistory()
        } else if (event === 'done') {
          lastMeta.value = {
            success: !!obj.success,
            exitCode: obj.exitCode ?? -1,
          }
          stopStreamAnim(botMsg)
          botMsg.displayStream = false
          botMsg.streamPlain = null
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
  streamAnimTimers.forEach((h) => cancelAnimationFrame(h))
  streamAnimTimers.clear()
  try {
    const page = await fetchGitQaChatMessages(id, 0, 500)
    const list = page.content || []
    messages.value = list.map((row) => {
      const role = row.role === 'USER' ? 'user' : 'assistant'
      const parsed = role === 'user' ? parseUserAttachmentsRow(row.content || '') : { attachments: [] }
      const assistantSplit = role === 'assistant' ? splitAssistantStored(row.content || '') : { thinking: '', body: '' }
      return {
        id: row.id,
        role,
        content: role === 'assistant' ? assistantSplit.body : row.content || '',
        thinking: role === 'assistant' ? assistantSplit.thinking : undefined,
        thinkingOpen: false,
        attachments: role === 'user' ? parsed.attachments : undefined,
        streamPlain: null,
        displayStream: false,
        clientKey: nextClientKey(),
        feedback: row.role === 'ASSISTANT' ? row.feedback ?? null : null,
      }
    })
  } catch {
    ElMessage.warning('加载历史记录失败')
  } finally {
    historyLoading.value = false
    scrollToBottom()
  }
}

async function send() {
  const text = draft.value.trim()
  if (replying.value || !project.value) return
  if (!text && !pendingFiles.value.length) return
  const q = buildQuestionForApi(text, pendingFiles.value)
  const attachMeta = pendingFiles.value.map((f) => ({ name: f.name }))
  const persisted = attachMeta.length ? `<!--gitqa-attach:${JSON.stringify(attachMeta)}-->\n${q}` : q
  draft.value = ''
  pendingFiles.value = []
  const userMsg = {
    id: null,
    role: 'user',
    content: persisted,
    attachments: attachMeta,
    displayStream: false,
    streamPlain: null,
    clientKey: nextClientKey(),
  }
  const botMsg = {
    id: null,
    role: 'assistant',
    content: '',
    thinking: '',
    thinkingOpen: false,
    displayStream: true,
    streamPlain: null,
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
    stopStreamAnim(botMsg)
    botMsg.streamPlain = null
  } finally {
    replying.value = false
    abortCtrl = null
    stopStreamAnim(botMsg)
    botMsg.displayStream = false
    botMsg.streamPlain = null
    scrollToBottom()
  }
}

onMounted(async () => {
  const id = route.params.id
  try {
    project.value = await getGitQaProject(id)
    await loadHistory()
    nextTick(() => {
      updateAtBottomFromWrap()
      updateActiveAssistantFromScroll()
    })
  } catch {
    ElMessage.error('加载配置失败')
    goBack()
  }
})

onBeforeUnmount(() => {
  abortCtrl?.abort()
  cancelSpeech()
  try {
    speechRec?.stop()
  } catch {
    /* */
  }
  speechRec = null
  streamAnimTimers.forEach((h) => cancelAnimationFrame(h))
  streamAnimTimers.clear()
})
</script>

<style scoped lang="scss">
.chat-page {
  --chat-surface: #f7f7f7;
  --chat-header-bg: #f7f7f7;
  --chat-header-border: #ebeef5;
  --chat-scroll-bg: #f7f7f7;
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
  --chat-composer-bg: #f7f7f7;
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
  width: 36px;
  height: 36px;
  border-radius: 10px;
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
.chat-body-row {
  flex: 1;
  min-height: 0;
  position: relative;
}
.chat-scroll--grow {
  width: 100%;
  min-height: 0;
}
.chat-scroll--toc-pad :deep(.git-qa-scroll-wrap) {
  padding-right: 25px;
  box-sizing: border-box;
}
.chat-scroll--toc-pad-collapsed :deep(.git-qa-scroll-wrap) {
  padding-right: 25px;
  box-sizing: border-box;
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
:deep(.git-qa-scroll-wrap) {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}
.messages {
  max-width: 880px;
  margin: 0 auto;
  padding-top: 16px;
  padding-bottom: 24px;
  box-sizing: border-box;
}
.welcome-full {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 100%;
  box-sizing: border-box;
  padding: 24px 16px;
}
.welcome-title {
  margin: 0;
  max-width: 880px;
  font-size: 25px;
  color: var(--chat-welcome-title);
  font-weight: 600;
  line-height: 1.45;
  text-align: center;
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
.user-attach-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.user-attach-tag {
  max-width: 100%;
}
.bubble--assistant {
  background: transparent;
  border: none;
  padding: 4px 0;
  width: 100%;
  max-width: 100%;
}
.assistant-thinking-fold {
  margin-bottom: 8px;
}
.thinking-fold-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: var(--el-color-primary, #409eff);
}
.thinking-fold-toggle:hover {
  text-decoration: underline;
}
.thinking-fold-caret {
  transition: transform 0.2s ease;
  font-size: 12px;
}
.thinking-fold-caret--open {
  transform: rotate(180deg);
}
.thinking-fold-body {
  margin-top: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--chat-thinking-bg);
  border: 1px solid var(--chat-border);
  max-height: 40vh;
  overflow-y: auto;
}
.thinking-fold-body pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.45;
  color: var(--chat-muted);
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
.bubble-md {
  width: 100%;
  font-size: 14px;
  line-height: 1.6;
  color: var(--chat-body-text);
}
.bubble-md :deep(h1),
.bubble-md :deep(h2),
.bubble-md :deep(h3),
.bubble-md :deep(h4),
.bubble-md :deep(h5),
.bubble-md :deep(h6) {
  scroll-margin-top: 8px;
  margin: 0.75em 0 0.35em;
  font-weight: 600;
}
.bubble-md :deep(h1) {
  font-size: 1.35em;
}
.bubble-md :deep(h2) {
  font-size: 1.2em;
}
.bubble-md :deep(p) {
  margin: 0.45em 0;
}
.bubble-md :deep(ul),
.bubble-md :deep(ol) {
  margin: 0.45em 0;
  padding-left: 1.3em;
}
.bubble-md :deep(pre) {
  margin: 0.5em 0;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  overflow-x: auto;
}
.bubble-md--dark {
  color: var(--el-text-color-primary, #e5eaf3);
}
.bubble-md--dark :deep(pre) {
  background: rgba(15, 23, 42, 0.45);
}
.bubble-md--dark :deep(a) {
  color: #7dd3fc;
}

.toc-float {
  position: absolute;
  right: 10px;
  top: 6px;
  bottom: 6px;
  z-index: 40;
  width: 200px;
  pointer-events: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.toc-float > * {
  pointer-events: auto;
}
.toc-float--collapsed {
  width: auto;
}
.toc-panel {
  max-height: min(70vh, calc(100vh - 200px));
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  padding: 0 8px 8px;
  border: 1px solid var(--chat-border);
  background: transparent;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
.chat-page--dark .toc-panel {
  border-color: var(--el-border-color, #303030);
}
.toc-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 8px 2px 6px;
  border-bottom: 1px solid var(--chat-header-border);
}
.toc-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--chat-title);
}
.toc-empty {
  padding: 10px 4px;
  font-size: 12px;
  color: var(--chat-muted);
}
.toc-nav {
  max-height: 55vh;
  overflow-y: auto;
  padding: 6px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.toc-item {
  display: block;
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  line-height: 1.35;
  color: var(--chat-muted);
  padding: 4px 6px;
  border-radius: 6px;
  transition: background 0.15s ease, color 0.15s ease;
}
.toc-item:hover {
  background: rgba(64, 158, 255, 0.08);
  color: var(--chat-body-text);
}
.toc-item--active {
  color: var(--el-color-primary, #409eff);
  font-weight: 600;
  background: rgba(64, 158, 255, 0.1);
}
.toc-reopen {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  padding: 12px 6px;
  border-radius: 10px;
  border: 1px solid var(--chat-border);
  background: transparent;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--el-color-primary, #409eff);
}
.chat-page--dark .toc-reopen {
  border-color: var(--el-border-color, #303030);
}
.thinking-row {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 12px;
}
.thinking-bubble {
  display: inline-flex;
  align-items: center;
  padding: 0;
  border-radius: 0;
  background: transparent;
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
  padding: 0 20px 12px 20px;
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
.composer-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
.pending-files {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px 0;
}
.pending-file-tag {
  max-width: 100%;
}
.composer-textarea-stack {
  position: relative;
}
.composer-inner-bar {
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 2px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 4px;
  pointer-events: none;
}
.composer-inner-bar > * {
  pointer-events: auto;
}
.composer-model-inner {
  position: static;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  pointer-events: auto;
}
.composer-actions-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-right: 5px;
  margin-bottom: 5px;
}
.composer-actions-right :deep(.el-button) {
  margin-left: 0 !important;
}
.composer-icon-btn {
  padding: 6px;
  color: var(--chat-toolbar-icon);
}
.composer-icon-btn:hover {
  color: #409eff;
}
.composer-send-fab {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
  margin-left: 2px;
  flex-shrink: 0;
  background: #111 !important;
  border: none !important;
  color: #fff !important;
}
.composer-send-fab:hover {
  background: #333 !important;
}
.composer-send-fab :deep(.el-icon) {
  color: #fff;
}
.model-hint-text {
  font-size: 12px;
  color: var(--chat-model-hint);
  white-space: nowrap;
  user-select: none;
  max-width: min(200px, 38vw);
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}
.model-select-inner {
  flex: 1;
  min-width: 0;
  max-width: calc(100% - 120px);
}
.model-select-inner :deep(.el-select__wrapper) {
  min-height: 28px;
  padding: 2px 8px;
  border-radius: 8px;
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  font-size: 12px;
  font-weight: 500;
  color: var(--chat-model-text);
}
.model-select-inner :deep(.el-select__wrapper.is-hovering),
.model-select-inner :deep(.el-select__wrapper.is-focused),
.model-select-inner :deep(.el-select__wrapper:hover) {
  background: transparent !important;
  box-shadow: none !important;
}
.model-select-inner :deep(.el-select__wrapper .el-select__selected-item) {
  color: var(--chat-model-text);
}
.model-select-inner :deep(.el-select__caret) {
  color: var(--chat-model-hint);
}
.composer-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  border-radius: 16px;
  resize: none;
  padding: 12px 12px 42px 12px;
  font-size: 14px;
  line-height: 1.55;
}
.composer-textarea :deep(.el-input__count) {
  display: none;
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

.chat-page .bubble-md :deep(h1.md-heading--toc-flash),
.chat-page .bubble-md :deep(h2.md-heading--toc-flash),
.chat-page .bubble-md :deep(h3.md-heading--toc-flash),
.chat-page .bubble-md :deep(h4.md-heading--toc-flash),
.chat-page .bubble-md :deep(h5.md-heading--toc-flash),
.chat-page .bubble-md :deep(h6.md-heading--toc-flash) {
  animation: gitQaTocHeadingFlash 1.1s ease-in-out 2;
}

@keyframes gitQaTocHeadingFlash {
  0% {
    background-color: rgba(64, 158, 255, 0.45);
    box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.25);
  }
  100% {
    background-color: transparent;
    box-shadow: none;
  }
}
</style>
