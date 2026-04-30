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
      与仓库内 <code>.cursor/skills/&lt;技能名&gt;/SKILL.md</code> 同名时，<strong>每次扫描前</strong>由平台写入工作区，<strong>覆盖仓库文件</strong>，优先级最高；未在平台配置时仍使用仓库自带技能。
      「扫描技能名」须与下方「技能名」一致。进入页面后请点击<strong>查询</strong>加载列表。
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
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

  <el-drawer v-model="detailVisible" title="平台技能详情" size="62%">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small" class="detail-desc">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="技能名">{{ detail.skillName }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ detail.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ detail.status === 1 ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatBackendDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatBackendDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <h4>SKILL.md 预览</h4>
      <MarkdownPreview :source="detail.skillBody || ''" />
    </template>
  </el-drawer>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑平台技能' : '新建平台技能'" width="92%" top="4vh" class="skill-dialog" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="技能名" prop="skillName">
        <el-input v-model="form.skillName" maxlength="128" placeholder="仅字母数字及 ._-，与 SKILL 目录名一致" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="说明" prop="description">
        <el-input v-model="form.description" maxlength="500" placeholder="列表展示用" />
      </el-form-item>
      <el-form-item label="SKILL.md" prop="skillBody">
        <div class="skill-toolbar">
          <el-radio-group v-model="skillBodyViewMode" size="small">
            <el-radio-button label="edit">编辑</el-radio-button>
            <el-radio-button label="split">编辑+预览</el-radio-button>
            <el-radio-button label="preview">预览</el-radio-button>
          </el-radio-group>
          <div class="skill-actions">
            <el-button size="small" @click="copySkillBody">复制</el-button>
            <el-button size="small" @click="downloadSkillMd">下载 SKILL.md</el-button>
          </div>
        </div>
        <p class="field-hint">正文为 Markdown；预览与下发扫描日志中的预览使用相同渲染规则。</p>
        <div v-if="skillBodyViewMode === 'edit'" class="skill-body-block">
          <el-input
            v-model="form.skillBody"
            type="textarea"
            :rows="22"
            placeholder="粘贴完整 SKILL.md（含 YAML frontmatter 的 --- name / description ---）"
          />
        </div>
        <div v-else-if="skillBodyViewMode === 'split'" class="skill-split">
          <el-input
            v-model="form.skillBody"
            type="textarea"
            :rows="20"
            class="split-editor"
            placeholder="左侧编辑，右侧实时预览"
          />
          <MarkdownPreview class="split-preview" :source="form.skillBody" />
        </div>
        <div v-else class="skill-body-block">
          <MarkdownPreview :source="form.skillBody" />
        </div>
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
</template>

<script setup>
import { reactive, ref } from 'vue'
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

const skillBodyViewMode = ref('edit')

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
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  skillName: '',
  description: '',
  skillBody: '',
  status: 1,
})

const rules = {
  skillName: [{ required: true, message: '必填', trigger: 'blur' }],
  skillBody: [{ required: true, message: '必填', trigger: 'blur' }],
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

function resetForm() {
  form.id = null
  form.skillName = ''
  form.description = ''
  form.skillBody = `---
name: my-scan-skill
description: 示例：按需修改 name 与正文
---

# 扫描说明

在此编写技能正文…
`
  form.status = 1
  skillBodyViewMode.value = 'edit'
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

async function openDetail(row) {
  detail.value = await getPlatformSkill(row.id)
  detailVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    skillName: row.skillName,
    description: row.description || '',
    skillBody: row.skillBody || '',
    status: row.status,
  })
  skillBodyViewMode.value = 'edit'
  dialogVisible.value = true
}

async function saveDialog() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      skillName: form.skillName.trim(),
      description: form.description?.trim() || null,
      skillBody: form.skillBody.trim(),
      status: form.status,
    }
    if (isEdit.value) {
      await updatePlatformSkill(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createPlatformSkill(payload)
      ElMessage.success('已创建')
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

async function copySkillBody() {
  const t = form.skillBody ?? ''
  try {
    await navigator.clipboard.writeText(t)
    ElMessage.success('已复制 SKILL.md 原文')
  } catch {
    ElMessage.error('复制失败，请手动选中编辑区文本复制')
  }
}

function downloadSkillMd() {
  const name = (form.skillName || 'SKILL').trim().replace(/[/\\?%*:|"<>]/g, '_') || 'SKILL'
  const blob = new Blob([form.skillBody ?? ''], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${name}.md`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已开始下载')
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
.skill-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
}
.skill-actions {
  display: flex;
  gap: 8px;
}
.field-hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
.skill-body-block {
  width: 100%;
}
.skill-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: stretch;
  min-height: 360px;
}
@media (max-width: 900px) {
  .skill-split {
    grid-template-columns: 1fr;
  }
}
.split-editor :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  min-height: 360px;
}
.split-preview {
  min-height: 360px;
  max-height: 70vh;
}
</style>

<style lang="scss">
.skill-dialog .el-dialog__body {
  padding-top: 8px;
}
</style>
