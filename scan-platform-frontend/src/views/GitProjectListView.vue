<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>Git项目管理</span>
        <div class="header-actions">
          <el-input
            v-model="filterDraft"
            clearable
            placeholder="项目名称"
            style="width: 220px"
            @keyup.enter="runSearch"
          />
          <el-button type="primary" plain @click="runSearch">查询</el-button>
          <el-button type="primary" @click="openCreate">新建项目</el-button>
        </div>
      </div>
    </template>
    <p class="tip">维护项目名称与 Git 地址主数据；「Git 项目配置」中可选择此处项目并自动带出地址。进入页面后请点击<strong>查询</strong>加载列表。</p>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="projectName" label="项目名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="gitUrl" label="Git项目地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
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

  <el-drawer v-model="detailVisible" title="Git 项目详情" size="480px">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName }}</el-descriptions-item>
        <el-descriptions-item label="Git 项目地址">{{ detail.gitUrl }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ detail.status === 1 ? '启用' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatBackendDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatBackendDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-drawer>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="form.projectName" maxlength="255" />
      </el-form-item>
      <el-form-item label="Git项目地址" prop="gitUrl">
        <el-input v-model="form.gitUrl" maxlength="500" placeholder="https://..." />
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
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createGitProject,
  deleteGitProject,
  fetchGitProjects,
  getGitProject,
  updateGitProject,
} from '@/api/gitProject'
import { formatBackendDateTime } from '@/utils/formatTime'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterDraft = ref('')
const filterKeyword = ref('')
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
  projectName: '',
  gitUrl: '',
  status: 1,
})

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑 Git 项目'
  if (isCopy.value) return '复制 Git 项目'
  return '新建 Git 项目'
})

const rules = {
  projectName: [{ required: true, message: '必填', trigger: 'blur' }],
  gitUrl: [{ required: true, message: '必填', trigger: 'blur' }],
}

function runSearch() {
  filterKeyword.value = filterDraft.value?.trim() || ''
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
    const res = await fetchGitProjects(page.value - 1, size.value, filterKeyword.value?.trim() || undefined)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.projectName = ''
  form.gitUrl = ''
  form.status = 1
}

function openCreate() {
  isEdit.value = false
  isCopy.value = false
  resetForm()
  dialogVisible.value = true
}

async function openDetail(row) {
  detail.value = await getGitProject(row.id)
  detailVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  isCopy.value = false
  Object.assign(form, {
    id: row.id,
    projectName: row.projectName,
    gitUrl: row.gitUrl,
    status: row.status,
  })
  dialogVisible.value = true
}

function openCopy(row) {
  isEdit.value = false
  isCopy.value = true
  Object.assign(form, {
    id: null,
    projectName: `${row.projectName}（复制）`,
    gitUrl: row.gitUrl,
    status: row.status,
  })
  dialogVisible.value = true
}

async function saveDialog() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      projectName: form.projectName.trim(),
      gitUrl: form.gitUrl.trim(),
      status: form.status,
    }
    if (isEdit.value) {
      await updateGitProject(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createGitProject(payload)
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

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除 Git 项目「${row.projectName}」？`, '提示', { type: 'warning' })
  await deleteGitProject(row.id)
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
</style>
