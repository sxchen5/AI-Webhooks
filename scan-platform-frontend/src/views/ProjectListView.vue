<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>WebHooks回调 · 仓库管理</span>
        <div class="header-actions">
          <el-select
            v-model="filterProjectName"
            clearable
            filterable
            placeholder="项目名称"
            style="width: 220px"
            @change="onFilterChange"
          >
            <el-option
              v-for="o in projectOptions"
              :key="o.id"
              :label="o.name"
              :value="o.name"
            />
          </el-select>
          <el-button type="primary" @click="openCreate">新建项目</el-button>
        </div>
      </div>
    </template>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="gitlabProjectId" label="GitLab 项目ID" width="130" />
      <el-table-column prop="projectName" label="项目名称" min-width="140" />
      <el-table-column prop="localCodePath" label="本地路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="agentCommand" label="Agent/技能" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.scanSkillName" class="skill-tag">技能: {{ row.scanSkillName }}</span>
          <span v-else>{{ row.agentCommand }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="receiveEmail" label="告警邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
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
        @current-change="load"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="640px" destroy-on-close>
    <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="120px">
      <el-form-item label="GitLab 项目ID" prop="gitlabProjectId">
        <el-input-number v-model="dialogForm.gitlabProjectId" :min="1" :controls="false" class="w-full" />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="dialogForm.projectName" maxlength="255" show-word-limit />
      </el-form-item>
      <el-form-item label="Git URL" prop="gitUrl">
        <el-input v-model="dialogForm.gitUrl" maxlength="500" placeholder="可选" />
      </el-form-item>
      <el-form-item label="本地代码目录" prop="localCodePath">
        <el-input v-model="dialogForm.localCodePath" maxlength="500" placeholder="服务器上已存在的代码路径" />
      </el-form-item>
      <el-divider content-position="left">Cursor 技能（可选）</el-divider>
      <el-form-item label="扫描技能">
        <el-radio-group v-model="dialogForm.skillPickMode">
          <el-radio label="none">不使用技能</el-radio>
          <el-radio label="platform">选择平台技能</el-radio>
          <el-radio label="custom">手动输入技能名</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="dialogForm.skillPickMode === 'platform'" label="平台技能">
        <el-select
          v-model="dialogForm.scanSkillName"
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
      <el-form-item v-if="dialogForm.skillPickMode === 'custom'" label="技能名">
        <el-input
          v-model="dialogForm.scanSkillName"
          maxlength="128"
          placeholder="与仓库 .cursor/skills 下目录名一致"
          clearable
        />
      </el-form-item>
      <el-form-item label="技能补充说明" prop="scanSkillPrompt">
        <el-input
          v-model="dialogForm.scanSkillPrompt"
          type="textarea"
          :rows="3"
          placeholder="漏洞、依赖风险等；支持 {{path}} {{branch}} {{commit}}"
          clearable
        />
      </el-form-item>
      <el-form-item label="Agent 命令" prop="agentCommand">
        <el-input
          v-model="dialogForm.agentCommand"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="与技能二选一；仅技能可填 echo 占位"
        />
      </el-form-item>
      <el-form-item label="告警邮箱" prop="receiveEmail">
        <el-input v-model="dialogForm.receiveEmail" maxlength="500" placeholder="多个逗号分隔，可选" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="dialogForm.status">
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
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createProject, deleteProject, fetchProjects, updateProject } from '@/api/project'
import { fetchWebhookProjectOptions } from '@/api/projectOptions'
import { fetchPlatformSkillOptions } from '@/api/platformSkill'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterProjectName = ref('')
const projectOptions = ref([])
const platformSkillOptions = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const dialogFormRef = ref()
const dialogForm = reactive({
  id: null,
  gitlabProjectId: null,
  projectName: '',
  gitUrl: '',
  localCodePath: '',
  agentCommand: '',
  scanSkillName: '',
  scanSkillPrompt: '',
  receiveEmail: '',
  status: 1,
  /** none | platform | custom */
  skillPickMode: 'none',
})

const dialogRules = {
  gitlabProjectId: [{ required: true, message: '必填', trigger: 'blur' }],
  projectName: [{ required: true, message: '必填', trigger: 'blur' }],
  localCodePath: [{ required: true, message: '必填', trigger: 'blur' }],
  agentCommand: [
    {
      validator: (_, v, cb) => {
        if (dialogForm.skillPickMode === 'none') {
          if (!(v && String(v).trim())) {
            cb(new Error('未使用技能时请填写 Agent 命令'))
          } else {
            cb()
          }
          return
        }
        if (
          (dialogForm.skillPickMode === 'platform' || dialogForm.skillPickMode === 'custom') &&
          !dialogForm.scanSkillName?.trim()
        ) {
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
  () => dialogForm.skillPickMode,
  (mode) => {
    if (mode === 'none') {
      dialogForm.scanSkillName = ''
    }
  }
)

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
    const res = await fetchProjects(page.value - 1, size.value, filterProjectName.value || undefined)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  load()
}

onMounted(async () => {
  try {
    projectOptions.value = await fetchWebhookProjectOptions()
  } catch {
    projectOptions.value = []
  }
  await loadPlatformSkillOptions()
  await load()
})

function resetForm() {
  dialogForm.id = null
  dialogForm.gitlabProjectId = null
  dialogForm.projectName = ''
  dialogForm.gitUrl = ''
  dialogForm.localCodePath = ''
  dialogForm.agentCommand = ''
  dialogForm.scanSkillName = ''
  dialogForm.scanSkillPrompt = ''
  dialogForm.receiveEmail = ''
  dialogForm.status = 1
  dialogForm.skillPickMode = 'none'
}

function inferSkillPickMode(savedName) {
  const n = (savedName || '').trim()
  if (!n) return 'none'
  if (platformSkillOptions.value.some((o) => o.skillName === n)) return 'platform'
  return 'custom'
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  const skillName = row.scanSkillName || ''
  Object.assign(dialogForm, {
    id: row.id,
    gitlabProjectId: row.gitlabProjectId,
    projectName: row.projectName,
    gitUrl: row.gitUrl || '',
    localCodePath: row.localCodePath,
    agentCommand: row.agentCommand === '(cursor-skill)' ? '' : row.agentCommand,
    scanSkillName: skillName,
    scanSkillPrompt: row.scanSkillPrompt || '',
    receiveEmail: row.receiveEmail || '',
    status: row.status,
    skillPickMode: inferSkillPickMode(skillName),
  })
  dialogVisible.value = true
}

async function saveDialog() {
  await dialogFormRef.value.validate()
  if (dialogForm.skillPickMode === 'none') {
    dialogForm.scanSkillName = ''
  }
  if (dialogForm.skillPickMode === 'platform' && !dialogForm.scanSkillName?.trim()) {
    ElMessage.warning('请选择平台技能，或改为「手动输入」/「不使用技能」')
    return
  }
  if (dialogForm.skillPickMode === 'custom' && !dialogForm.scanSkillName?.trim()) {
    ElMessage.warning('请填写技能名')
    return
  }
  const skillOut = dialogForm.skillPickMode === 'none' ? null : dialogForm.scanSkillName?.trim() || null
  const cmdTrim = dialogForm.agentCommand?.trim() || ''
  if (!skillOut && !cmdTrim) {
    ElMessage.warning('请填写 Agent 命令，或选择/填写扫描技能')
    return
  }
  saving.value = true
  try {
    const payload = {
      gitlabProjectId: dialogForm.gitlabProjectId,
      projectName: dialogForm.projectName,
      gitUrl: dialogForm.gitUrl || null,
      localCodePath: dialogForm.localCodePath,
      agentCommand: skillOut ? (cmdTrim || '(cursor-skill)') : cmdTrim,
      scanSkillName: skillOut,
      scanSkillPrompt: dialogForm.scanSkillPrompt?.trim() || null,
      receiveEmail: dialogForm.receiveEmail || null,
      status: dialogForm.status,
    }
    if (isEdit.value) {
      await updateProject(dialogForm.id, payload)
      ElMessage.success('已更新')
    } else {
      await createProject(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除项目「${row.projectName}」？`, '提示', { type: 'warning' })
  await deleteProject(row.id)
  ElMessage.success('已删除')
  await load()
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
  gap: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.skill-tag {
  color: #409eff;
  font-size: 13px;
}
.w-full {
  width: 100%;
}
</style>
