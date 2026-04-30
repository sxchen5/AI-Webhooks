<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>Git项目AI问答</span>
        <el-button type="primary" @click="openCreate">新建配置</el-button>
      </div>
    </template>
    <p class="tip">
      配置机器人名称与 Git 克隆；对话时默认在仓库目录执行
      <code>agent --print -p &lt;问题&gt; --output-format stream-json</code>。可选填写平台技能或自定义 Agent（将自动追加 stream-json）。保存后点「AI问答」进入对话。
    </p>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="botName" label="机器人名称" min-width="120" />
      <el-table-column prop="gitUrl" label="Git URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="gitUsername" label="用户名" width="110" show-overflow-tooltip />
      <el-table-column prop="branch" label="分支" width="90" />
      <el-table-column prop="localClonePath" label="本地克隆目录" min-width="140" show-overflow-tooltip />
      <el-table-column prop="agentCommand" label="Agent/技能" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.scanSkillName" style="color: #409eff">技能: {{ row.scanSkillName }}</span>
          <span v-else-if="row.agentCommand">{{ row.agentCommand }}</span>
          <span v-else class="muted">默认 stream-json 问答</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="success" link :disabled="row.status !== 1" @click="openChat(row)">AI问答</el-button>
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

  <el-drawer v-model="detailVisible" title="Git项目AI问答详情" size="56%">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="机器人名称">{{ detail.botName }}</el-descriptions-item>
        <el-descriptions-item label="关联 Git 项目 ID">{{ detail.gitProjectId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="Git URL">{{ detail.gitUrl }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.gitUsername || '—' }}</el-descriptions-item>
        <el-descriptions-item label="分支">{{ detail.branch || '—' }}</el-descriptions-item>
        <el-descriptions-item label="本地克隆目录">{{ detail.localClonePath || '—' }}</el-descriptions-item>
        <el-descriptions-item label="扫描技能">{{ detail.scanSkillName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="技能补充说明">{{ detail.scanSkillPrompt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="Agent 命令">
          <el-input type="textarea" :rows="4" readonly :model-value="detail.agentCommand || ''" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ detail.status === 1 ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatBackendDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatBackendDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-drawer>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="机器人名称" prop="botName">
        <el-input v-model="form.botName" maxlength="128" placeholder="如：代码助手" />
      </el-form-item>
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
        <el-input v-model="form.localClonePath" maxlength="500" placeholder="留空则使用工作目录下 git-qa-{id}" clearable />
      </el-form-item>
      <el-divider content-position="left">可选：技能或自定义 Agent</el-divider>
      <el-form-item label="扫描技能">
        <el-select
          v-model="form.scanSkillName"
          clearable
          filterable
          allow-create
          default-first-option
          placeholder="选平台技能或手动输入技能目录名，可不填"
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
      <el-form-item label="技能补充说明">
        <el-input
          v-model="form.scanSkillPrompt"
          type="textarea"
          :rows="2"
          placeholder="可选；可用 {{path}} {{branch}} {{commit}} {{question}}"
          clearable
        />
      </el-form-item>
      <el-form-item label="Agent 命令" prop="agentCommand">
        <el-input
          v-model="form.agentCommand"
          type="textarea"
          :rows="2"
          maxlength="1000"
          show-word-limit
          placeholder="可选；不填则对话使用默认 stream-json。可含 {{path}} {{branch}} {{commit}} {{question}}"
        />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createGitQaProject,
  deleteGitQaProject,
  fetchGitQaProjects,
  getGitQaProject,
  updateGitQaProject,
} from '@/api/gitQaProject'
import { fetchGitProjectOptions } from '@/api/gitProject'
import { fetchPlatformSkillOptions } from '@/api/platformSkill'
import { formatBackendDateTime } from '@/utils/formatTime'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const platformSkillOptions = ref([])
const gitProjectOptions = ref([])

const detailVisible = ref(false)
const detail = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  repoLinkMode: 'manual',
  gitProjectId: null,
  botName: '',
  gitUrl: '',
  gitUsername: '',
  gitPassword: '',
  branch: 'main',
  localClonePath: '',
  agentCommand: '',
  scanSkillName: '',
  scanSkillPrompt: '',
  status: 1,
})

const dialogTitle = computed(() => (isEdit.value ? '编辑 Git项目AI问答' : '新建 Git项目AI问答'))

const rules = {
  botName: [{ required: true, message: '必填', trigger: 'blur' }],
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
}

function onRepoLinkModeChange(mode) {
  if (mode === 'manual') {
    form.gitProjectId = null
  }
}

function onGitProjectSelect(id) {
  if (id == null) return
  const o = gitProjectOptions.value.find((x) => x.id === id)
  if (o) {
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

async function load() {
  loading.value = true
  try {
    const res = await fetchGitQaProjects(page.value - 1, size.value)
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
  form.botName = ''
  form.gitUrl = ''
  form.gitUsername = ''
  form.gitPassword = ''
  form.branch = 'main'
  form.localClonePath = ''
  form.agentCommand = ''
  form.scanSkillName = ''
  form.scanSkillPrompt = ''
  form.status = 1
}

function ensureLinkModeForDisabledProject() {
  if (form.repoLinkMode !== 'project' || form.gitProjectId == null) return
  const ok = gitProjectOptions.value.some((o) => o.id === form.gitProjectId)
  if (!ok) {
    form.repoLinkMode = 'manual'
    form.gitProjectId = null
    ElMessage.warning('关联的 Git 项目已禁用或已删除，已改为手动填写 Git URL')
  }
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

async function openDetail(row) {
  detail.value = await getGitQaProject(row.id)
  detailVisible.value = true
}

async function openEdit(row) {
  await loadGitProjectOptions()
  isEdit.value = true
  const skillName = row.scanSkillName || ''
  const linked = row.gitProjectId != null
  Object.assign(form, {
    id: row.id,
    repoLinkMode: linked ? 'project' : 'manual',
    gitProjectId: linked ? row.gitProjectId : null,
    botName: row.botName,
    gitUrl: row.gitUrl,
    gitUsername: row.gitUsername || '',
    gitPassword: '',
    branch: row.branch || 'main',
    localClonePath: row.localClonePath || '',
    agentCommand: row.agentCommand === '(cursor-skill)' ? '' : (row.agentCommand || ''),
    scanSkillName: skillName,
    scanSkillPrompt: row.scanSkillPrompt || '',
    status: row.status,
  })
  ensureLinkModeForDisabledProject()
  dialogVisible.value = true
}

function openChat(row) {
  router.push({ name: 'GitQaChat', params: { id: String(row.id) } })
}

async function saveDialog() {
  await formRef.value.validate()
  if (form.repoLinkMode === 'project' && form.gitProjectId == null) {
    ElMessage.warning('请选择 Git 项目，或改为「手动填写」')
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
      botName: form.botName.trim(),
      gitProjectId: form.repoLinkMode === 'project' ? form.gitProjectId : null,
      gitUrl: form.gitUrl,
      gitUsername: form.gitUsername || null,
      gitPassword: form.gitPassword || null,
      branch: form.branch || 'main',
      localClonePath: form.localClonePath || null,
      agentCommand: form.agentCommand?.trim() || null,
      scanSkillName: form.scanSkillName?.trim() || null,
      scanSkillPrompt: form.scanSkillPrompt?.trim() || null,
      status: form.status,
    }
    if (isEdit.value) {
      if (!form.gitPassword) delete payload.gitPassword
      await updateGitQaProject(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createGitQaProject(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.botName}」？`, '提示', { type: 'warning' })
  await deleteGitQaProject(row.id)
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
.tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
}
.tip code {
  font-size: 12px;
}
.muted {
  color: #909399;
  font-size: 13px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
