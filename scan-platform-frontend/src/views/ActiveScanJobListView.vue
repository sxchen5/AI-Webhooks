<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>下发任务管理</span>
        <div class="card-header-actions">
          <el-input
            v-model="jobNameDraft"
            clearable
            placeholder="任务名（留空查全部）"
            style="width: 200px"
            @keyup.enter="runSearch"
          />
          <el-select
            v-model="repoIdDraft"
            clearable
            filterable
            placeholder="关联配置（留空查全部）"
            style="width: 240px"
          >
            <el-option v-for="r in repoOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
          <el-button type="primary" plain @click="runSearch">查询</el-button>
          <el-button
            type="warning"
            :disabled="!selectedRows.length || batchRunning"
            :loading="batchRunning"
            @click="onBatchRun"
          >
            批量手动执行 ({{ selectedRows.length }})
          </el-button>
          <el-button type="primary" @click="openCreate">新建任务</el-button>
        </div>
      </div>
    </template>
    <p class="tip">定时使用 Spring 6 段 Cron（秒 分 时 日 月 周），例：每天 2 点 <code>0 0 2 * * ?</code>；启用定时后保存会自动计算下次执行时间。邮件通知使用<strong>系统配置管理 → 邮件配置</strong>中的 SMTP，收件人请在<strong>Git项目配置</strong>中配置通知邮箱。进入页面后请点击<strong>查询</strong>加载列表；条件留空为全部。</p>
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="48" :selectable="rowSelectable" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="jobName" label="任务名" min-width="120" />
      <el-table-column label="配置" min-width="120">
        <template #default="{ row }">{{ repoName(row.repoId) }}</template>
      </el-table-column>
      <el-table-column label="定时" width="90">
        <template #default="{ row }">
          <el-tag :type="row.scheduleEnabled === 1 ? 'success' : 'info'">{{ row.scheduleEnabled === 1 ? '开' : '关' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cronExpression" label="Cron" min-width="140" show-overflow-tooltip />
      <el-table-column prop="nextScheduleRun" label="下次执行" width="170">
        <template #default="{ row }">{{ formatBackendDateTime(row.nextScheduleRun) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          <el-button type="success" link :disabled="row.status !== 1" @click="onRun(row)">立即扫描</el-button>
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

  <el-drawer v-model="detailVisible" title="下发任务详情" size="520px">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ detail.jobName }}</el-descriptions-item>
        <el-descriptions-item label="关联 Git 项目配置">
          {{ repoName(detail.repoId) }} (repoId {{ detail.repoId }})
        </el-descriptions-item>
        <el-descriptions-item label="启用定时">
          <el-tag :type="detail.scheduleEnabled === 1 ? 'success' : 'info'">{{ detail.scheduleEnabled === 1 ? '开' : '关' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Cron">{{ detail.cronExpression || '—' }}</el-descriptions-item>
        <el-descriptions-item label="下次执行">{{ formatBackendDateTime(detail.nextScheduleRun) }}</el-descriptions-item>
        <el-descriptions-item label="上次执行">{{ formatBackendDateTime(detail.lastScheduleRun) }}</el-descriptions-item>
        <el-descriptions-item label="覆盖技能名">{{ detail.scanSkillName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="覆盖技能说明">{{ detail.scanSkillPrompt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="覆盖 Agent 命令">
          <el-input type="textarea" :rows="3" readonly :model-value="detail.agentCommandOverride || ''" />
        </el-descriptions-item>
        <el-descriptions-item label="失败发邮件">{{ detail.notifyOnFailure === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="成功发邮件">{{ detail.notifyOnSuccess === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ detail.status === 1 ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatBackendDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatBackendDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-drawer>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
      <el-form-item label="任务名称" prop="jobName">
        <el-input v-model="form.jobName" maxlength="255" />
      </el-form-item>
      <el-form-item label="关联配置" prop="repoId">
        <el-select v-model="form.repoId" placeholder="选择 Git 项目配置" filterable style="width: 100%">
          <el-option v-for="r in repoOptions" :key="r.id" :label="`${r.name} (#${r.id})`" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用定时">
        <el-switch :active-value="1" :inactive-value="0" v-model="form.scheduleEnabled" />
      </el-form-item>
      <el-form-item label="Cron 表达式" prop="cronExpression">
        <el-input v-model="form.cronExpression" maxlength="120" placeholder="0 0 2 * * ?" clearable />
      </el-form-item>
      <el-form-item label="覆盖技能名">
        <el-input v-model="form.scanSkillName" maxlength="128" placeholder="留空用 Git 项目配置" clearable />
      </el-form-item>
      <el-form-item label="覆盖技能说明">
        <el-input v-model="form.scanSkillPrompt" type="textarea" :rows="2" placeholder="留空用 Git 项目配置" clearable />
      </el-form-item>
      <el-form-item label="覆盖 Agent 命令">
        <el-input v-model="form.agentCommandOverride" type="textarea" :rows="2" maxlength="1000" placeholder="留空使用 Git 项目配置中的命令" clearable />
      </el-form-item>
      <el-form-item label="失败发邮件">
        <el-switch :active-value="1" :inactive-value="0" v-model="form.notifyOnFailure" />
      </el-form-item>
      <el-form-item label="成功发邮件">
        <el-switch :active-value="1" :inactive-value="0" v-model="form.notifyOnSuccess" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createActiveJob,
  deleteActiveJob,
  fetchActiveJobs,
  fetchRepoOptions,
  getActiveJob,
  runActiveJob,
  runActiveJobsBatch,
  updateActiveJob,
} from '@/api/activeScan'
import { formatBackendDateTime } from '@/utils/formatTime'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const hasSearched = ref(false)
const jobNameDraft = ref('')
const jobNameFilter = ref('')
const repoIdDraft = ref(null)
const repoIdFilter = ref(null)
const selectedRows = ref([])
const batchRunning = ref(false)
const repoOptions = ref([])
const repoMap = ref({})

const detailVisible = ref(false)
const detail = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const isCopy = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  jobName: '',
  repoId: null,
  scheduleEnabled: 0,
  cronExpression: '',
  agentCommandOverride: '',
  scanSkillName: '',
  scanSkillPrompt: '',
  notifyOnFailure: 1,
  notifyOnSuccess: 0,
  status: 1,
})

const rules = {
  jobName: [{ required: true, message: '必填', trigger: 'blur' }],
  repoId: [{ required: true, message: '必选', trigger: 'change' }],
}

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑任务'
  if (isCopy.value) return '复制任务'
  return '新建任务'
})

function repoName(id) {
  return repoMap.value[id] || `#${id}`
}

async function loadRepos() {
  try {
    repoOptions.value = (await fetchRepoOptions()) || []
  } catch {
    repoOptions.value = []
  }
  const m = {}
  repoOptions.value.forEach((r) => {
    m[r.id] = r.name
  })
  repoMap.value = m
}

function runSearch() {
  jobNameFilter.value = jobNameDraft.value?.trim() || ''
  repoIdFilter.value = repoIdDraft.value ?? null
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
    const res = await fetchActiveJobs(
      page.value - 1,
      size.value,
      jobNameFilter.value || undefined,
      repoIdFilter.value,
    )
    tableData.value = res.content || []
    total.value = res.totalElements || 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

function rowSelectable(row) {
  return row.status === 1
}

function onSelectionChange(rows) {
  selectedRows.value = rows || []
}

async function onBatchRun() {
  const rows = selectedRows.value.filter((r) => r.status === 1)
  if (!rows.length) return
  await ElMessageBox.confirm(`将对选中的 ${rows.length} 个启用任务各触发一次手动扫描，是否继续？`, '批量执行', {
    type: 'warning',
  })
  batchRunning.value = true
  try {
    const jobIds = rows.map((r) => r.id)
    const list = await runActiveJobsBatch(jobIds)
    const ok = list.filter((r) => r.logId != null).length
    const fail = list.filter((r) => r.error).length
    ElMessage.success(`已提交 ${ok} 条；失败 ${fail} 条`)
    if (fail > 0) {
      const lines = list
        .filter((r) => r.error)
        .map((r) => `任务 #${r.jobId}: ${r.error}`)
        .slice(0, 8)
      ElMessageBox.alert(lines.join('\n') + (list.filter((r) => r.error).length > 8 ? '\n…' : ''), '部分失败', {
        type: 'warning',
      })
    }
    if (hasSearched.value) {
      await load()
    }
  } finally {
    batchRunning.value = false
  }
}

function resetForm() {
  form.id = null
  form.jobName = ''
  form.repoId = repoOptions.value[0]?.id ?? null
  form.scheduleEnabled = 0
  form.cronExpression = ''
  form.agentCommandOverride = ''
  form.scanSkillName = ''
  form.scanSkillPrompt = ''
  form.notifyOnFailure = 1
  form.notifyOnSuccess = 0
  form.status = 1
}

function openCreate() {
  isEdit.value = false
  isCopy.value = false
  resetForm()
  dialogVisible.value = true
}

async function openDetail(row) {
  detail.value = await getActiveJob(row.id)
  detailVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  isCopy.value = false
  Object.assign(form, {
    id: row.id,
    jobName: row.jobName,
    repoId: row.repoId,
    scheduleEnabled: row.scheduleEnabled,
    cronExpression: row.cronExpression || '',
    agentCommandOverride: row.agentCommandOverride || '',
    scanSkillName: row.scanSkillName || '',
    scanSkillPrompt: row.scanSkillPrompt || '',
    notifyOnFailure: row.notifyOnFailure,
    notifyOnSuccess: row.notifyOnSuccess,
    status: row.status,
  })
  dialogVisible.value = true
}

function openCopy(row) {
  isEdit.value = false
  isCopy.value = true
  Object.assign(form, {
    id: null,
    jobName: `${row.jobName}（复制）`,
    repoId: row.repoId,
    scheduleEnabled: row.scheduleEnabled,
    cronExpression: row.cronExpression || '',
    agentCommandOverride: row.agentCommandOverride || '',
    scanSkillName: row.scanSkillName || '',
    scanSkillPrompt: row.scanSkillPrompt || '',
    notifyOnFailure: row.notifyOnFailure,
    notifyOnSuccess: row.notifyOnSuccess,
    status: row.status,
  })
  dialogVisible.value = true
}

async function saveDialog() {
  await formRef.value.validate()
  if (form.scheduleEnabled === 1 && !form.cronExpression?.trim()) {
    ElMessage.warning('启用定时时请填写 Cron')
    return
  }
  saving.value = true
  try {
    const payload = {
      jobName: form.jobName,
      repoId: form.repoId,
      scheduleEnabled: form.scheduleEnabled,
      cronExpression: form.cronExpression?.trim() || null,
      agentCommandOverride: form.agentCommandOverride?.trim() || null,
      scanSkillName: form.scanSkillName?.trim() || null,
      scanSkillPrompt: form.scanSkillPrompt?.trim() || null,
      notifyOnFailure: form.notifyOnFailure,
      notifyOnSuccess: form.notifyOnSuccess,
      status: form.status,
    }
    if (isEdit.value) {
      await updateActiveJob(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createActiveJob(payload)
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

async function onRun(row) {
  const { logId } = await runActiveJob(row.id)
  ElMessage.success(`已触发扫描，日志 ID: ${logId}`)
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除任务「${row.jobName}」？`, '提示', { type: 'warning' })
  await deleteActiveJob(row.id)
  ElMessage.success('已删除')
  if (!hasSearched.value) {
    runSearch()
  } else {
    await load()
  }
}

onMounted(async () => {
  await loadRepos()
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
.card-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #909399;
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
</style>
