<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>项目管理</span>
        <el-button type="primary" @click="openCreate">新建项目</el-button>
      </div>
    </template>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="gitlabProjectId" label="GitLab 项目ID" width="130" />
      <el-table-column prop="projectName" label="项目名称" min-width="140" />
      <el-table-column prop="localCodePath" label="本地路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="agentCommand" label="Agent 命令" min-width="200" show-overflow-tooltip />
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
      <el-form-item label="Agent 命令" prop="agentCommand">
        <el-input
          v-model="dialogForm.agentCommand"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="例：包装脚本或 agent -p &quot;...&quot; --output-format text；占位符 {{path}} {{branch}} {{commit}}；子进程含 WEBHOOK_* 环境变量"
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createProject, deleteProject, fetchProjects, updateProject } from '@/api/project'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

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
  agentCommand: '/bin/bash /path/to/AI-Webhooks/scripts/cursor-gitlab-webhook-scan.sh',
  receiveEmail: '',
  status: 1,
})

const dialogRules = {
  gitlabProjectId: [{ required: true, message: '必填', trigger: 'blur' }],
  projectName: [{ required: true, message: '必填', trigger: 'blur' }],
  localCodePath: [{ required: true, message: '必填', trigger: 'blur' }],
  agentCommand: [{ required: true, message: '必填', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const res = await fetchProjects(page.value - 1, size.value)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  dialogForm.id = null
  dialogForm.gitlabProjectId = null
  dialogForm.projectName = ''
  dialogForm.gitUrl = ''
  dialogForm.localCodePath = ''
  dialogForm.agentCommand = '/bin/bash /path/to/AI-Webhooks/scripts/cursor-gitlab-webhook-scan.sh'
  dialogForm.receiveEmail = ''
  dialogForm.status = 1
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(dialogForm, {
    id: row.id,
    gitlabProjectId: row.gitlabProjectId,
    projectName: row.projectName,
    gitUrl: row.gitUrl || '',
    localCodePath: row.localCodePath,
    agentCommand: row.agentCommand,
    receiveEmail: row.receiveEmail || '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function saveDialog() {
  await dialogFormRef.value.validate()
  saving.value = true
  try {
    const payload = {
      gitlabProjectId: dialogForm.gitlabProjectId,
      projectName: dialogForm.projectName,
      gitUrl: dialogForm.gitUrl || null,
      localCodePath: dialogForm.localCodePath,
      agentCommand: dialogForm.agentCommand,
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

onMounted(load)
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
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.w-full {
  width: 100%;
}
</style>
