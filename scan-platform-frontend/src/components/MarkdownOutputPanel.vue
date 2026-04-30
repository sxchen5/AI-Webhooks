<template>
  <div class="md-output-panel">
    <div v-if="!hideToolbar && !compact" class="toolbar">
      <el-radio-group v-model="mode" size="small">
        <el-radio-button label="preview">预览</el-radio-button>
        <el-radio-button label="raw">原文</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="copyText">复制原文</el-button>
    </div>
    <div v-else-if="!hideToolbar && compact" class="toolbar toolbar--compact">
      <el-button size="small" text type="primary" @click="copyText">复制</el-button>
    </div>
    <MarkdownPreview v-if="mode === 'preview'" class="preview-box" :class="{ 'preview-box--dark': dark }" :source="text" />
    <el-input v-else type="textarea" :rows="rows" readonly class="raw-box" :model-value="text" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownPreview from '@/components/MarkdownPreview.vue'

const props = defineProps({
  /** 原始 Markdown / 纯文本 */
  text: { type: String, default: '' },
  rows: { type: Number, default: 14 },
  /** 完全隐藏工具栏（聊天气泡内纯内容） */
  hideToolbar: { type: Boolean, default: false },
  /** 深色预览背景（聊天气泡内） */
  dark: { type: Boolean, default: false },
})

const mode = ref('preview')

async function copyText() {
  const t = props.text ?? ''
  try {
    await navigator.clipboard.writeText(t)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选中文本复制')
  }
}
</script>

<style scoped lang="scss">
.md-output-panel {
  width: 100%;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.toolbar--compact {
  justify-content: flex-end;
  margin-bottom: 4px;
}
.preview-box {
  width: 100%;
}
.preview-box--dark :deep(.md-preview) {
  background: rgba(15, 23, 42, 0.55);
  border-color: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
}
.preview-box--dark :deep(.md-preview a) {
  color: #7dd3fc;
}
.raw-box :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
</style>
