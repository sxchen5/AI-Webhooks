<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>主动扫描 · Git 仓库</span>
        <el-button type="primary" @click="openCreate">新建仓库</el-button>
      </div>
    </template>
    <p class="tip">支持多个 HTTP(S) 仓库；用户名/密码用于非交互克隆。凭据存库请注意权限；密码不会在列表中回显。</p>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="repoName" label="名称" min-width="120" />
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
      <el-table-column label="操作" width="140" fixed="right">
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

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑仓库' : '新建仓库'" width="640px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="显示名称" prop="repoName">
        <el-input v-model="form.repoName" maxlength="255" />
      </el-form-item>
      <el-form-item label="Git URL" prop="gitUrl">
        <el-input v-model="form.gitUrl" maxlength="500" placeholder="https://..." />
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
      <el-form-item label="扫描技能名">
        <el-input v-model="form.scanSkillName" maxlength="128" placeholder="与平台技能或仓库 .cursor/skills 目录名一致" clearable />
      </el-form-item>
      <el-form-item label="技能补充说明">
        <el-input v-model="form.scanSkillPrompt" type="textarea" :rows="2" placeholder="漏洞、供应链风险等" clearable />
      </el-form-item>
      <el-form-item label="Agent 命令" prop="agentCommand">
        <el-input v-model="form.agentCommand" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="与技能二选一；仅技能可填 echo 占位" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createActiveRepo,
  deleteActiveRepo,
  fetchActiveRepos,
  updateActiveRepo,
} from '@/api/activeScan'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
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
  status: 1,
})

const rules = {
  repoName: [{ required: true, message: '必填', trigger: 'blur' }],
  gitUrl: [{ required: true, message: '必填', trigger: 'blur' }],
  agentCommand: [
    {
      validator: (_, v, cb) => {
        if (!form.scanSkillName?.trim() && !(v && String(v).trim())) {
          cb(new Error('请填写 Agent 命令或扫描技能名'))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function load() {
  loading.value = true
  try {
    const res = await fetchActiveRepos(page.value - 1, size.value)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
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
  form.status = 1
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    repoName: row.repoName,
    gitUrl: row.gitUrl,
    gitUsername: row.gitUsername || '',
    gitPassword: '',
    branch: row.branch || 'main',
    localClonePath: row.localClonePath || '',
    agentCommand: row.agentCommand,
    scanSkillName: row.scanSkillName || '',
    scanSkillPrompt: row.scanSkillPrompt || '',
    receiveEmail: row.receiveEmail || '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function saveDialog() {
  await formRef.value.validate()
  const u = (form.gitUrl || '').toLowerCase()
  if (!isEdit.value && u.startsWith('http') && form.gitUsername && !form.gitPassword) {
    ElMessage.warning('已填写用户名时，请填写密码或 Token')
    return
  }
  saving.value = true
  try {
    const payload = {
      repoName: form.repoName,
      gitUrl: form.gitUrl,
      gitUsername: form.gitUsername || null,
      gitPassword: form.gitPassword || null,
      branch: form.branch || 'main',
      localClonePath: form.localClonePath || null,
      agentCommand: form.agentCommand?.trim() || '(cursor-skill)',
      scanSkillName: form.scanSkillName?.trim() || null,
      scanSkillPrompt: form.scanSkillPrompt?.trim() || null,
      receiveEmail: form.receiveEmail || null,
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
  await ElMessageBox.confirm(`确定删除仓库「${row.repoName}」？`, '提示', { type: 'warning' })
  await deleteActiveRepo(row.id)
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
</style>
