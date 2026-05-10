<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>平台技能</span>
        <div class="header-actions">
          <el-input
            v-model="keywordDraft"
            clearable
            placeholder="技能名或说明关键词"
            style="width: 260px"
            @keyup.enter="runSearch"
          />
          <el-button type="primary" plain @click="runSearch">查询</el-button>
          <el-button type="primary" @click="openCreate">新建技能</el-button>
        </div>
      </div>
    </template>
    <p class="tip">
      与仓库根目录下技能包同名时，<strong>每次扫描前</strong>由平台<strong>整目录覆盖写入</strong>（含
      <code>SKILL.md</code> 及子目录下附加文件），优先级高于仓库内已有同名技能目录。写入路径随<strong>Git 项目配置 / 下发任务</strong>中的
      <strong>Agent CLI</strong>：<strong>Cursor</strong> 为 <code>.cursor/skills/&lt;技能名&gt;/</code>，<strong>Claude Code</strong> 为
      <code>.claude/skills/&lt;技能名&gt;/</code>。须包含根路径 <code>SKILL.md</code>。可从本机<strong>选择技能文件夹一键导入</strong>（推荐），或上传多个文件 /
      导入 JSON。进入页面后请点击<strong>查询</strong>加载列表。
    </p>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="skillName" label="技能名" width="180" />
      <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        class-name="col-actions"
        header-cell-class-name="col-actions"
        align="left"
        header-align="left"
      >
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="info" link @click="openCopy(row)">复制</el-button>
          <el-button type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="page"
        :page-size="size"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <!-- 置于主弹窗外：避免 destroy-on-close 卸载后 ref 丢失；且需非零尺寸才能稳定触发系统文件选择框 -->
  <input
    ref="dirImportRef"
    type="file"
    class="file-input-native"
    webkitdirectory
    directory
    multiple
    @change="onDirectoryPicked"
  />
  <input ref="fileImportRef" type="file" class="file-input-native" multiple @change="onMultiFilesPicked" />
  <input ref="jsonImportRef" type="file" class="file-input-native" accept="application/json,.json" @change="onJsonPicked" />

  <el-drawer v-model="detailVisible" title="平台技能详情" size="62%">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small" class="detail-desc">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="技能名">{{ detail.skillName }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ detail.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="文件数">{{ detail.files?.length ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ detail.status === 1 ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatBackendDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatBackendDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <h4>SKILL.md 预览</h4>
      <MarkdownPreview :source="primarySkillMd(detail.files)" />
      <template v-if="(detail.files || []).length > 1">
        <h4>其它文件</h4>
        <el-table :data="nonPrimaryFiles(detail.files)" size="small" border max-height="280">
          <el-table-column prop="path" label="路径" min-width="200" />
          <el-table-column label="大小" width="100">
            <template #default="{ row }">{{ (row.content || '').length }} 字</template>
          </el-table-column>
        </el-table>
      </template>
    </template>
  </el-drawer>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="680px"
    align-center
    class="skill-form-dialog"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="技能名" prop="skillName">
        <el-input
          v-model="form.skillName"
          maxlength="128"
          placeholder="仅字母数字及 ._-；与仓库内 .cursor/skills/ 或 .claude/skills/ 下目录名一致（由 Agent CLI 决定写入路径）"
          :disabled="isEdit"
        />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model="form.description" maxlength="500" placeholder="列表展示用" />
      </el-form-item>
      <el-form-item label="技能文件" prop="files">
        <div class="files-toolbar">
          <el-button type="primary" size="small" @click="addEmptyFile">添加文件</el-button>
          <el-button type="success" size="small" @click="triggerDirImport">从本地目录一键导入</el-button>
          <el-button size="small" @click="triggerFileImport">上传文件</el-button>
          <el-button size="small" @click="triggerJsonImport">导入 JSON</el-button>
          <el-button size="small" @click="exportJsonBundle">导出 JSON</el-button>
        </div>
        <p class="field-hint">
          路径为相对技能根目录的正斜杠形式（如 <code>SKILL.md</code>、<code>refs/rule.md</code>）。目录导入会合并同名路径；须包含
          <code>SKILL.md</code> 且内容非空。
        </p>
        <el-table :data="form.files" border size="small" class="files-table" max-height="360">
          <el-table-column label="路径" min-width="220">
            <template #default="{ row, $index }">
              <el-input v-model="row.path" size="small" placeholder="相对路径" @change="syncPrimaryCase($index)" />
            </template>
          </el-table-column>
          <el-table-column label="字数" width="90">
            <template #default="{ row }">{{ (row.content || '').length }}</template>
          </el-table-column>
          <el-table-column
            label="操作"
            class-name="col-actions"
            header-cell-class-name="col-actions"
            align="left"
            header-align="left"
          >
            <template #default="{ $index }">
              <el-button type="primary" link size="small" @click="openFileEditor($index)">编辑内容</el-button>
              <el-button
                type="danger"
                link
                size="small"
                :disabled="isOnlySkillMd(form.files, $index)"
                @click="removeFileRow($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveDialog">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="fileEditorVisible"
    :title="`编辑：${editingPath}`"
    width="800px"
    align-center
    class="skill-file-editor-dialog"
    destroy-on-close
    @closed="onFileEditorClosed"
  >
    <div class="file-editor-toolbar">
      <el-radio-group v-model="fileEditorViewMode" size="small">
        <el-radio-button label="edit">编辑</el-radio-button>
        <el-radio-button label="split">编辑+预览</el-radio-button>
        <el-radio-button label="preview">Markdown 预览</el-radio-button>
      </el-radio-group>
    </div>
    <div v-if="fileEditorViewMode === 'edit'" class="file-editor-body">
      <el-input
        v-model="editingContent"
        type="textarea"
        :rows="18"
        class="file-editor-ta"
        placeholder="文件 UTF-8 文本内容"
      />
    </div>
    <div v-else-if="fileEditorViewMode === 'split'" class="file-editor-body file-editor-split">
      <el-input v-model="editingContent" type="textarea" :rows="16" class="file-editor-ta split-editor" />
      <MarkdownPreview class="split-preview" :source="editingContent" />
    </div>
    <div v-else class="file-editor-body">
      <MarkdownPreview :source="editingContent" />
    </div>
    <template #footer>
      <el-button @click="fileEditorVisible = false">取消</el-button>
      <el-button type="primary" @click="applyFileEditor">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import {
  createPlatformSkill,
  deletePlatformSkill,
  fetchPlatformSkills,
  getPlatformSkill,
  updatePlatformSkill,
} from '@/api/platformSkill'
import { formatBackendDateTime } from '@/utils/formatTime'

const DEFAULT_SKILL_MD = `---
name: my-scan-skill
description: 示例：按需修改 name 与正文
---

# 扫描说明

在此编写技能正文…
`

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keywordDraft = ref('')
const keywordFilter = ref('')
const hasSearched = ref(false)

const detailVisible = ref(false)
const detail = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const isCopy = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  skillName: '',
  description: '',
  files: [],
  status: 1,
})

const dirImportRef = ref(null)
const fileImportRef = ref(null)
const jsonImportRef = ref(null)

const fileEditorVisible = ref(false)
const fileEditorViewMode = ref('edit')
const editingIndex = ref(-1)
const editingPath = ref('')
const editingContent = ref('')

const rules = {
  skillName: [{ required: true, message: '必填', trigger: 'blur' }],
  files: [
    {
      validator: (_rule, _val, callback) => {
        const list = normalizeFilesForPayload(form.files)
        if (!list.length) {
          callback(new Error('至少保留一个文件'))
          return
        }
        const primary = list.find((f) => f.path === 'SKILL.md')
        if (!primary) {
          callback(new Error('必须包含 SKILL.md'))
          return
        }
        if (!String(primary.content || '').trim()) {
          callback(new Error('SKILL.md 内容不能为空'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑平台技能'
  if (isCopy.value) return '复制平台技能'
  return '新建平台技能'
})

function primarySkillMd(files) {
  const p = (files || []).find((f) => (f.path || '').toLowerCase() === 'skill.md')
  return p?.content || ''
}

function nonPrimaryFiles(files) {
  return (files || []).filter((f) => (f.path || '').toLowerCase() !== 'skill.md')
}

function runSearch() {
  keywordFilter.value = keywordDraft.value?.trim() || ''
  page.value = 1
  hasSearched.value = true
  load()
}

function onPageChange() {
  if (!hasSearched.value) return
  load()
}

async function load() {
  if (!hasSearched.value) return
  loading.value = true
  try {
    const res = await fetchPlatformSkills(page.value - 1, size.value, keywordFilter.value || undefined)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function normalizeFilesForPayload(files) {
  const map = new Map()
  for (const f of files || []) {
    let p = (f.path || '').trim().replace(/\\/g, '/').replace(/^\/+/, '')
    if (!p) continue
    if (p.toLowerCase() === 'skill.md') p = 'SKILL.md'
    map.set(p, f.content ?? '')
  }
  return Array.from(map.entries()).map(([path, content]) => ({ path, content }))
}

function upsertFile(path, content) {
  const p = path.trim().replace(/\\/g, '/').replace(/^\/+/, '')
  if (!p) return
  const norm = p.toLowerCase() === 'skill.md' ? 'SKILL.md' : p
  const idx = form.files.findIndex((x) => (x.path || '').replace(/\\/g, '/').replace(/^\/+/, '') === norm)
  if (idx >= 0) {
    form.files[idx] = { path: norm, content: content ?? '' }
  } else {
    form.files.push({ path: norm, content: content ?? '' })
  }
}

function commonDirPrefix(paths) {
  if (!paths.length) return ''
  const parts = paths.map((p) => p.split('/').filter(Boolean))
  const minLen = Math.min(...parts.map((a) => a.length))
  const prefix = []
  for (let i = 0; i < minLen; i++) {
    const seg = parts[0][i]
    if (parts.every((p) => p[i] === seg)) prefix.push(seg)
    else break
  }
  return prefix.join('/')
}

function stripPrefix(path, prefix) {
  if (!prefix) return path.replace(/\\/g, '/').replace(/^\/+/, '')
  const n = path.replace(/\\/g, '/').replace(/^\/+/, '')
  const pref = prefix.replace(/\\/g, '/').replace(/\/+$/, '')
  if (n === pref) return ''
  if (n.startsWith(pref + '/')) return n.slice(pref.length + 1)
  return n
}

function shouldSkipImportPath(rel) {
  if (!rel) return true
  const lower = rel.toLowerCase()
  if (lower.includes('/.git/') || lower.endsWith('/.git')) return true
  if (lower.includes('__macosx/')) return true
  if (lower.endsWith('.ds_store')) return true
  return false
}

function resetForm() {
  form.id = null
  form.skillName = ''
  form.description = ''
  form.files = [{ path: 'SKILL.md', content: DEFAULT_SKILL_MD }]
  form.status = 1
}

function openCreate() {
  isEdit.value = false
  isCopy.value = false
  resetForm()
  dialogVisible.value = true
}

async function openDetail(row) {
  detail.value = await getPlatformSkill(row.id)
  detailVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  isCopy.value = false
  await loadFormFromRow(row.id)
}

async function openCopy(row) {
  isEdit.value = false
  isCopy.value = true
  await loadFormFromCopy(row)
}

async function loadFormFromRow(id) {
  const d = await getPlatformSkill(id)
  form.id = d.id
  form.skillName = d.skillName
  form.description = d.description || ''
  form.files = (d.files || []).map((f) => ({ path: f.path, content: f.content ?? '' }))
  form.status = d.status
  dialogVisible.value = true
}

async function loadFormFromCopy(row) {
  const d = await getPlatformSkill(row.id)
  form.id = null
  form.skillName = `${d.skillName}-copy`
  form.description = d.description || ''
  form.files = (d.files || []).map((f) => ({ path: f.path, content: f.content ?? '' }))
  form.status = d.status
  dialogVisible.value = true
}

async function saveDialog() {
  await formRef.value.validate()
  const files = normalizeFilesForPayload(form.files)
  if (!files.find((f) => f.path === 'SKILL.md')) {
    ElMessage.warning('必须包含 SKILL.md')
    return
  }
  saving.value = true
  try {
    const payload = {
      skillName: form.skillName.trim(),
      description: form.description?.trim() || null,
      files,
      status: form.status,
    }
    if (isEdit.value) {
      await updatePlatformSkill(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createPlatformSkill(payload)
      ElMessage.success(isCopy.value ? '已从复制创建' : '已创建')
    }
    dialogVisible.value = false
    if (!hasSearched.value) {
      runSearch()
    } else {
      await load()
    }
  } finally {
    saving.value = false
  }
}

async function addEmptyFile() {
  try {
    const { value } = await ElMessageBox.prompt('相对路径（如 scripts/check.sh）', '添加文件', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: 'refs/notes.md',
    })
    const p = (value || '').trim().replace(/\\/g, '/').replace(/^\/+/, '')
    if (!p) return
    upsertFile(p, '')
    ElMessage.success('已添加，请点击「编辑内容」填写')
  } catch {
    /* cancel */
  }
}

function onFileEditorClosed() {
  fileEditorViewMode.value = 'edit'
}

async function triggerDirImport() {
  await nextTick()
  dirImportRef.value?.click()
}

async function triggerFileImport() {
  await nextTick()
  fileImportRef.value?.click()
}

async function triggerJsonImport() {
  await nextTick()
  jsonImportRef.value?.click()
}

async function onDirectoryPicked(ev) {
  const list = ev.target.files
  ev.target.value = ''
  if (!list?.length) return
  const paths = []
  for (const f of list) {
    const rel = f.webkitRelativePath || f.name
    if (shouldSkipImportPath(rel)) continue
    paths.push(rel.replace(/\\/g, '/'))
  }
  const prefix = commonDirPrefix(paths)
  let count = 0
  for (const f of list) {
    const rel = f.webkitRelativePath || f.name
    if (shouldSkipImportPath(rel)) continue
    const relNorm = rel.replace(/\\/g, '/')
    const shortPath = stripPrefix(relNorm, prefix)
    if (!shortPath || shouldSkipImportPath(shortPath)) continue
    try {
      const text = await f.text()
      upsertFile(shortPath, text)
      count++
    } catch {
      ElMessage.warning(`无法读取为文本，已跳过: ${shortPath}`)
    }
  }
  ElMessage.success(`已导入 ${count} 个文件${prefix ? `（已去掉公共前缀「${prefix}」）` : ''}`)
}

async function onMultiFilesPicked(ev) {
  const list = ev.target.files
  ev.target.value = ''
  if (!list?.length) return
  let n = 0
  for (const f of list) {
    const name = (f.name || '').replace(/\\/g, '/').replace(/^\/+/, '')
    if (!name || shouldSkipImportPath(name)) continue
    try {
      const text = await f.text()
      upsertFile(name, text)
      n++
    } catch {
      ElMessage.warning(`跳过: ${name}`)
    }
  }
  ElMessage.success(`已导入 ${n} 个文件`)
}

async function onJsonPicked(ev) {
  const file = ev.target.files?.[0]
  ev.target.value = ''
  if (!file) return
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    let arr = null
    if (Array.isArray(data)) arr = data
    else if (data && Array.isArray(data.files)) arr = data.files
    if (!arr) {
      ElMessage.error('JSON 格式应为 { "files": [ { "path", "content" } ] } 或文件数组')
      return
    }
    let n = 0
    for (const item of arr) {
      if (!item || !item.path) continue
      upsertFile(String(item.path), item.content != null ? String(item.content) : '')
      n++
    }
    ElMessage.success(`已从 JSON 合并 ${n} 个文件`)
  } catch (e) {
    ElMessage.error(`JSON 解析失败: ${e.message || e}`)
  }
}

function exportJsonBundle() {
  const files = normalizeFilesForPayload(form.files)
  const bundle = {
    skillName: form.skillName || '',
    description: form.description || '',
    files,
  }
  const blob = new Blob([JSON.stringify(bundle, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const base = (form.skillName || 'platform-skill').replace(/[/\\?%*:|"<>]/g, '_') || 'platform-skill'
  a.download = `${base}-bundle.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已开始下载 JSON')
}

function openFileEditor(index) {
  const row = form.files[index]
  if (!row) return
  editingIndex.value = index
  editingPath.value = row.path || ''
  editingContent.value = row.content ?? ''
  fileEditorViewMode.value = 'edit'
  fileEditorVisible.value = true
}

function applyFileEditor() {
  const i = editingIndex.value
  if (i < 0 || !form.files[i]) {
    fileEditorVisible.value = false
    return
  }
  form.files[i].content = editingContent.value
  fileEditorVisible.value = false
}

function removeFileRow(index) {
  form.files.splice(index, 1)
  if (!form.files.length) {
    form.files.push({ path: 'SKILL.md', content: DEFAULT_SKILL_MD })
  }
}

function isOnlySkillMd(files, index) {
  const row = files[index]
  if (!row) return false
  if ((row.path || '').toLowerCase() !== 'skill.md') return false
  return files.length === 1
}

function syncPrimaryCase(index) {
  const row = form.files[index]
  if (!row) return
  if ((row.path || '').trim().toLowerCase() === 'skill.md') {
    row.path = 'SKILL.md'
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除平台技能「${row.skillName}」？`, '提示', { type: 'warning' })
  await deletePlatformSkill(row.id)
  ElMessage.success('已删除')
  if (!hasSearched.value) {
    runSearch()
  } else {
    await load()
  }
}
</script>

<style scoped lang="scss">
.page-card {
  min-height: calc(100vh - 120px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  code {
    background: #f4f4f5;
    padding: 2px 6px;
    border-radius: 4px;
  }
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.detail-desc {
  margin-bottom: 16px;
}
h4 {
  margin: 16px 0 8px;
}
.files-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
.field-hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
.files-table {
  width: 100%;
}
.file-input-native {
  position: fixed;
  left: -9999px;
  top: 0;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}
.file-editor-toolbar {
  margin-bottom: 10px;
}
.file-editor-body {
  min-height: 200px;
}
.file-editor-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: stretch;
}
@media (max-width: 900px) {
  .file-editor-split {
    grid-template-columns: 1fr;
  }
}
.split-editor :deep(.el-textarea__inner) {
  min-height: 320px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
.split-preview {
  min-height: 320px;
  max-height: 55vh;
  overflow: auto;
}
.file-editor-ta :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
}
</style>

<style lang="scss">
.skill-form-dialog .el-dialog__body {
  padding-top: 8px;
}
.skill-file-editor-dialog .el-dialog__body {
  padding-top: 8px;
}
</style>
