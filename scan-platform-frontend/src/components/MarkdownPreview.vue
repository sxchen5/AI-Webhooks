<template>
  <div class="md-preview" v-html="html" />
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  source: { type: String, default: '' },
})

const html = computed(() => renderMarkdown(props.source || ''))
</script>

<style scoped lang="scss">
.md-preview {
  padding: 12px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-blank);
  font-size: 14px;
  line-height: 1.6;
  min-height: 120px;
  max-height: 60vh;
  overflow: auto;
}
.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3) {
  margin: 0.75em 0 0.4em;
  font-weight: 600;
}
.md-preview :deep(h1) {
  font-size: 1.35em;
}
.md-preview :deep(h2) {
  font-size: 1.2em;
}
.md-preview :deep(p) {
  margin: 0.5em 0;
}
.md-preview :deep(ul),
.md-preview :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.4em;
}
.md-preview :deep(pre) {
  margin: 0.6em 0;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  overflow-x: auto;
}
.md-preview :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
}
.md-preview :deep(p code),
.md-preview :deep(li code) {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
}
.md-preview :deep(pre code) {
  padding: 0;
  background: transparent;
}
.md-preview :deep(blockquote) {
  margin: 0.5em 0;
  padding: 4px 12px;
  border-left: 4px solid var(--el-color-primary-light-5);
  color: var(--el-text-color-secondary);
}
.md-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.6em 0;
}
.md-preview :deep(th),
.md-preview :deep(td) {
  border: 1px solid var(--el-border-color);
  padding: 6px 10px;
}
.md-preview :deep(a) {
  color: var(--el-color-primary);
}
</style>
