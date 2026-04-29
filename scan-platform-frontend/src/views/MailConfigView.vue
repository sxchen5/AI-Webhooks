<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>邮件配置</span>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </div>
    </template>
    <el-form v-loading="loading" :model="form" label-width="140px" class="form">
      <el-form-item label="SMTP 主机">
        <el-input v-model="form.smtpHost" placeholder="如 smtp.example.com" clearable />
      </el-form-item>
      <el-form-item label="SMTP 端口">
        <el-input-number v-model="form.smtpPort" :min="1" :max="65535" :controls="false" />
      </el-form-item>
      <el-form-item label="SMTP 用户名">
        <el-input v-model="form.smtpUsername" clearable />
      </el-form-item>
      <el-form-item label="SMTP 密码">
        <el-input
          v-model="form.smtpPasswordInput"
          type="password"
          show-password
          placeholder="留空表示不修改已保存密码"
          clearable
        />
      </el-form-item>
      <el-form-item label="邮件标题前缀">
        <el-input v-model="form.emailTitlePrefix" maxlength="255" show-word-limit />
      </el-form-item>
      <el-alert
        title="说明"
        type="info"
        :closable="false"
        description="用于「Git 仓库扫描」主动扫描任务成功/失败通知；收件人在各仓库的「通知邮箱」中配置。无需改 application.yml。"
      />
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSysConfig, saveSysConfig } from '@/api/sysConfig'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  smtpHost: '',
  smtpPort: null,
  smtpUsername: '',
  smtpPasswordInput: '',
  emailTitlePrefix: '',
})

async function load() {
  loading.value = true
  try {
    const c = await getSysConfig()
    form.smtpHost = c.smtpHost || ''
    form.smtpPort = c.smtpPort
    form.smtpUsername = c.smtpUsername || ''
    form.smtpPasswordInput = ''
    form.emailTitlePrefix = c.emailTitlePrefix || '【代码扫描通知】'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    await saveSysConfig({
      smtpHost: form.smtpHost || null,
      smtpPort: form.smtpPort,
      smtpUsername: form.smtpUsername || null,
      smtpPassword: form.smtpPasswordInput || null,
      emailTitlePrefix: form.emailTitlePrefix || null,
    })
    ElMessage.success('已保存')
    form.smtpPasswordInput = ''
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-card {
  max-width: 900px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.form {
  max-width: 720px;
}
</style>
