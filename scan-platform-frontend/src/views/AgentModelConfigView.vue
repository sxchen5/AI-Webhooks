<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>{{ t('nav.agentModels') }}</span>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </template>
    <p class="tip">{{ t('agentModels.tip') }}</p>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="cliKind" label="CLI" width="100" />
      <el-table-column prop="modelKey" label="模型键" min-width="140" />
      <el-table-column prop="displayLabel" label="显示名" min-width="120" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
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
  </el-card>

  <el-dialog v-model="dlg" :title="dlgTitle" width="520px" align-center destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="CLI" prop="cliKind">
        <el-select v-model="form.cliKind" placeholder="CURSOR / CLAUDE" style="width: 100%">
          <el-option label="Cursor (agent)" value="CURSOR" />
          <el-option label="Claude Code (claude)" value="CLAUDE" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型键" prop="modelKey">
        <el-input v-model="form.modelKey" maxlength="64" placeholder="传给 --model 的值" />
      </el-form-item>
      <el-form-item label="显示名">
        <el-input v-model="form.displayLabel" maxlength="128" placeholder="可选，下拉展示用" clearable />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" :max="9999" :controls="true" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dlg = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAgentModel,
  deleteAgentModel,
  fetchAgentModelList,
  updateAgentModel,
} from '@/api/agentModels'

const { t } = useI18n()
const loading = ref(false)
const rows = ref([])
const dlg = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  cliKind: 'CURSOR',
  modelKey: '',
  displayLabel: '',
  sortOrder: 0,
  status: 1,
})
const rules = {
  cliKind: [{ required: true, message: '必选', trigger: 'change' }],
  modelKey: [{ required: true, message: '必填', trigger: 'blur' }],
}

const dlgTitle = computed(() => (isEdit.value ? '编辑模型' : '新增模型'))

async function load() {
  loading.value = true
  try {
    rows.value = (await fetchAgentModelList()) || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.cliKind = 'CURSOR'
  form.modelKey = ''
  form.displayLabel = ''
  form.sortOrder = 0
  form.status = 1
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dlg.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    cliKind: row.cliKind,
    modelKey: row.modelKey,
    displayLabel: row.displayLabel || '',
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
  })
  dlg.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      cliKind: form.cliKind,
      modelKey: form.modelKey.trim(),
      displayLabel: form.displayLabel?.trim() || null,
      sortOrder: form.sortOrder,
      status: form.status,
    }
    if (isEdit.value) {
      await updateAgentModel(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createAgentModel(payload)
      ElMessage.success('已创建')
    }
    dlg.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`删除模型「${row.modelKey}」？`, '提示', { type: 'warning' })
  await deleteAgentModel(row.id)
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
  line-height: 1.5;
}
</style>
