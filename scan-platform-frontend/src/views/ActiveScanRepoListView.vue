<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>Git仓库扫描 · Git项目配置</span>
        <div class="header-actions">
          <el-input
            v-model="filterRepoName"
            clearable
            placeholder="筛选项目名称"
            style="width: 220px"
            @change="onFilterChange"
            @clear="onFilterChange"
          />
          <el-button type="primary" @click="openCreate">新建配置</el-button>
        </div>
      </div>
    </template>
    <p class="tip">支持多个 HTTP(S) 仓库；用户名/密码用于非交互克隆。可从「Git项目管理」选择项目自动带出地址。通知邮箱用于主动扫描结果邮件，发信账号在<strong>系统配置管理 → 邮件配置</strong>中维护。</p>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="repoName" label="项目名称" min-width="120" />
      <el-table-column prop="gitUrl" label="Git URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="gitUsername" label="用户名" width="120" show-overflow-tooltip />
      <el-table-column prop="branch" label="分支" width="100" />
      <el-table-column prop="localClonePath" label="本地目录" min-width="140" show-overflow-tooltip />
      <el-table-column prop="agentCommand" label="Agent/技能" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.scanSkillName" style="color: #409eff">技能: {{ row.scanSkillName }}</span>
          <span v-else>{{ row.agentCommand }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
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
        @current-change="load"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="名称来源">
        <el-radio-group v-model="form.repoLinkMode" @change="onRepoLinkModeChange">
          <el-radio label="project">选择 Git 项目</el-radio>
          <el-radio label="manual">手动填写</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.repoLinkMode === 'project'" label="Git 项目" prop="gitProjectId">
        <el-select
          v-model="form.gitProjectId"
          clearable
          filterable
          placeholder="从 Git项目管理 中选择"
          style="width: 100%"
          @change="onGitProjectSelect"
        >
          <el-option
            v-for="o in gitProjectOptions"
            :key="o.id"
            :label="`${o.projectName} (#${o.id})`"
            :value="o.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目名称" prop="repoName">
        <el-input
          v-model="form.repoName"
          maxlength="255"
          :disabled="form.repoLinkMode === 'project' && form.gitProjectId != null"
          placeholder="与 Git 项目一致或手动填写"
        />
      </el-form-item>
      <el-form-item label="Git URL" prop="gitUrl">
        <el-input v-model="form.gitUrl" maxlength="500" placeholder="https://..." :disabled="form.repoLinkMode === 'project'" />
      </el-form-item>
      <el-form-item label="用户名" prop="gitUsername">
        <el-input v-model="form.gitUsername" maxlength="255" placeholder="HTTPS 可选" clearable />
      </el-form-item>
      <el-form-item label="密码/Token" prop="gitPassword">
        <el-input v-model="form.gitPassword" type="password" show-password placeholder="新建必填；编辑留空表示不修改" clearable />
      </el-form-item>
      <el-form-item label="分支" prop="branch">
        <el-input v-model="form.branch" maxlength="255" placeholder="默认 main" />
      </el-form-item>
      <el-form-item label="本地克隆目录" prop="localClonePath">
        <el-input v-model="form.localClonePath" maxlength="500" placeholder="留空则自动生成到工作区" clearable />
      </el-form-item>
      <el-divider content-position="left">Cursor 技能（可选）</el-divider>
      <el-form-item label="扫描技能">
        <el-radio-group v-model="form.skillPickMode">
          <el-radio label="none">不使用技能</el-radio>
          <el-radio label="platform">选择平台技能</el-radio>
          <el-radio label="custom">手动输入技能名</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.skillPickMode === 'platform'" label="平台技能">
        <el-select
          v-model="form.scanSkillName"
          clearable
          filterable
          placeholder="从已启用的平台技能中选择"
          style="width: 100%"
        >
          <el-option
            v-for="o in platformSkillOptions"
            :key="o.skillName"
            :label="o.description ? `${o.skillName} — ${o.description}` : o.skillName"
            :value="o.skillName"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.skillPickMode === 'custom'" label="技能名">
        <el-input
          v-model="form.scanSkillName"
          maxlength="128"
          placeholder="与仓库 .cursor/skills 下目录名一致"
          clearable
        />
      </el-form-item>
      <el-form-item label="技能补充说明">
        <el-input v-model="form.scanSkillPrompt" type="textarea" :rows="2" placeholder="漏洞、供应链风险等" clearable />
      </el-form-item>
      <el-form-item label="Agent 命令" prop="agentCommand">
        <el-input v-model="form.agentCommand" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="与技能二选一；仅技能可填 echo 占位" />
      </el-form-item>
      <el-form-item label="展示检出 Commit">
        <el-switch :active-value="1" :inactive-value="0" v-model="form.displayCommit" />
        <span class="form-hint">关闭后列表与邮件不强调单次提交 hash，适合全仓类技能</span>
      </el-form-item>
      <el-form-item label="通知邮箱" prop="receiveEmail">
        <el-input v-model="form.receiveEmail" maxlength="500" placeholder="逗号分隔，可选" clearable />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createActiveRepo,
  deleteActiveRepo,
  fetchActiveRepos,
  updateActiveRepo,
} from '@/api/activeScan'
import { fetchGitProjectOptions } from '@/api/gitProject'
import { fetchPlatformSkillOptions } from '@/api/platformSkill'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterRepoName = ref('')

const platformSkillOptions = ref([])
const gitProjectOptions = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const isCopy = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  /** manual | project */
  repoLinkMode: 'manual',
  gitProjectId: null,
  repoName: '',
  gitUrl: '',
  gitUsername: '',
  gitPassword: '',
  branch: 'main',
  localClonePath: '',
  agentCommand: '',
  scanSkillName: '',
  scanSkillPrompt: '',
  receiveEmail: '',
  displayCommit: 1,
  status: 1,
  skillPickMode: 'none',
})

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑 Git 项目配置'
  if (isCopy.value) return '复制 Git 项目配置'
  return '新建 Git 项目配置'
})

const rules = {
  repoName: [{ required: true, message: '必填', trigger: 'blur' }],
  gitUrl: [{ required: true, message: '必填', trigger: 'blur' }],
  gitProjectId: [
    {
      validator: (_, v, cb) => {
        if (form.repoLinkMode === 'project' && (v == null || v === '')) {
          cb(new Error('请选择 Git 项目'))
        } else {
          cb()
        }
      },
      trigger: 'change',
    },
  ],
  agentCommand: [
    {
      validator: (_, v, cb) => {
        if (form.skillPickMode === 'none') {
          if (!(v && String(v).trim())) {
            cb(new Error('未使用技能时请填写 Agent 命令'))
          } else {
            cb()
          }
          return
        }
        if ((form.skillPickMode === 'platform' || form.skillPickMode === 'custom') && !form.scanSkillName?.trim()) {
          cb(new Error('请选择或填写扫描技能名'))
          return
        }
        cb()
      },
      trigger: 'blur',
    },
  ],
}

watch(
  () => form.skillPickMode,
  (mode) => {
    if (mode === 'none') {
      form.scanSkillName = ''
    }
  }
)

function onRepoLinkModeChange(mode) {
  if (mode === 'manual') {
    form.gitProjectId = null
  }
}

function onGitProjectSelect(id) {
  if (id == null) {
    return
  }
  const o = gitProjectOptions.value.find((x) => x.id === id)
  if (o) {
    form.repoName = o.projectName
    form.gitUrl = o.gitUrl || ''
  }
}

async function loadGitProjectOptions() {
  try {
    gitProjectOptions.value = (await fetchGitProjectOptions()) || []
  } catch {
    gitProjectOptions.value = []
  }
}

async function loadPlatformSkillOptions() {
  try {
    platformSkillOptions.value = (await fetchPlatformSkillOptions()) || []
  } catch {
    platformSkillOptions.value = []
  }
}

function onFilterChange() {
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const res = await fetchActiveRepos(page.value - 1, size.value, filterRepoName.value?.trim() || undefined)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.repoLinkMode = 'manual'
  form.gitProjectId = null
  form.repoName = ''
  form.gitUrl = ''
  form.gitUsername = ''
  form.gitPassword = ''
  form.branch = 'main'
  form.localClonePath = ''
  form.agentCommand = ''
  form.scanSkillName = ''
  form.scanSkillPrompt = ''
  form.receiveEmail = ''
  form.displayCommit = 1
  form.status = 1
  form.skillPickMode = 'none'
}

function inferSkillPickMode(savedName) {
  const n = (savedName || '').trim()
  if (!n) return 'none'
  if (platformSkillOptions.value.some((o) => o.skillName === n)) return 'platform'
  return 'custom'
}

function openCreate() {
  isEdit.value = false
  isCopy.value = false
  resetForm()
  dialogVisible.value = true
}

/** 下拉仅含启用项目；若当前关联项已禁用则改为手动并清除关联 */
function ensureLinkModeForDisabledProject() {
  if (form.repoLinkMode !== 'project' || form.gitProjectId == null) return
  const ok = gitProjectOptions.value.some((o) => o.id === form.gitProjectId)
  if (!ok) {
    form.repoLinkMode = 'manual'
    form.gitProjectId = null
    ElMessage.warning('关联的 Git 项目已禁用或已删除，已改为手动填写，请确认项目名称与 Git URL 后保存')
  }
}

async function openEdit(row) {
  await loadGitProjectOptions()
  isEdit.value = true
  isCopy.value = false
  const skillName = row.scanSkillName || ''
  const linked = row.gitProjectId != null
  Object.assign(form, {
    id: row.id,
    repoLinkMode: linked ? 'project' : 'manual',
    gitProjectId: linked ? row.gitProjectId : null,
    repoName: row.repoName,
    gitUrl: row.gitUrl,
    gitUsername: row.gitUsername || '',
    gitPassword: '',
    branch: row.branch || 'main',
    localClonePath: row.localClonePath || '',
    agentCommand: row.agentCommand === '(cursor-skill)' ? '' : row.agentCommand,
    scanSkillName: skillName,
    scanSkillPrompt: row.scanSkillPrompt || '',
    receiveEmail: row.receiveEmail || '',
    displayCommit: row.displayCommit === 0 ? 0 : 1,
    status: row.status,
    skillPickMode: inferSkillPickMode(skillName),
  })
  ensureLinkModeForDisabledProject()
  dialogVisible.value = true
}

async function openCopy(row) {
  await loadGitProjectOptions()
  isEdit.value = false
  isCopy.value = true
  const skillName = row.scanSkillName || ''
  const linked = row.gitProjectId != null
  Object.assign(form, {
    id: null,
    repoLinkMode: linked ? 'project' : 'manual',
    gitProjectId: linked ? row.gitProjectId : null,
    repoName: `${row.repoName}（复制）`,
    gitUrl: row.gitUrl,
    gitUsername: row.gitUsername || '',
    gitPassword: '',
    branch: row.branch || 'main',
    localClonePath: row.localClonePath || '',
    agentCommand: row.agentCommand === '(cursor-skill)' ? '' : row.agentCommand,
    scanSkillName: skillName,
    scanSkillPrompt: row.scanSkillPrompt || '',
    receiveEmail: row.receiveEmail || '',
    displayCommit: row.displayCommit === 0 ? 0 : 1,
    status: row.status,
    skillPickMode: inferSkillPickMode(skillName),
  })
  ensureLinkModeForDisabledProject()
  dialogVisible.value = true
}

async function saveDialog() {
  await formRef.value.validate()
  if (form.repoLinkMode === 'project' && form.gitProjectId == null) {
    ElMessage.warning('请选择 Git 项目，或改为「手动填写」')
    return
  }
  if (form.skillPickMode === 'none') {
    form.scanSkillName = ''
  }
  if (form.skillPickMode === 'platform' && !form.scanSkillName?.trim()) {
    ElMessage.warning('请选择平台技能，或改为「手动输入」/「不使用技能」')
    return
  }
  if (form.skillPickMode === 'custom' && !form.scanSkillName?.trim()) {
    ElMessage.warning('请填写技能名')
    return
  }
  const skillOut = form.skillPickMode === 'none' ? null : form.scanSkillName?.trim() || null
  const cmdTrim = form.agentCommand?.trim() || ''
  if (!skillOut && !cmdTrim) {
    ElMessage.warning('请填写 Agent 命令，或选择/填写扫描技能')
    return
  }
  const u = (form.gitUrl || '').toLowerCase()
  if (!isEdit.value && u.startsWith('http') && form.gitUsername && !form.gitPassword) {
    ElMessage.warning('已填写用户名时，请填写密码或 Token')
    return
  }
  saving.value = true
  try {
    const payload = {
      repoName: form.repoName,
      gitProjectId: form.repoLinkMode === 'project' ? form.gitProjectId : null,
      gitUrl: form.gitUrl,
      gitUsername: form.gitUsername || null,
      gitPassword: form.gitPassword || null,
      branch: form.branch || 'main',
      localClonePath: form.localClonePath || null,
      agentCommand: skillOut ? (cmdTrim || '(cursor-skill)') : cmdTrim,
      scanSkillName: skillOut,
      scanSkillPrompt: form.scanSkillPrompt?.trim() || null,
      receiveEmail: form.receiveEmail || null,
      displayCommit: form.displayCommit === 0 ? 0 : 1,
      status: form.status,
    }
    if (isEdit.value) {
      if (!form.gitPassword) delete payload.gitPassword
      await updateActiveRepo(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createActiveRepo(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除配置「${row.repoName}」？`, '提示', { type: 'warning' })
  await deleteActiveRepo(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await loadGitProjectOptions()
  await loadPlatformSkillOptions()
  await load()
})
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
  gap: 12px;
}
.tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  vertical-align: middle;
}
</style>
