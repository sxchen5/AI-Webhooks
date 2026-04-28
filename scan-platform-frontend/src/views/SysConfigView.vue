<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>系统配置</span>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </div>
    </template>
    <el-form v-loading="loading" :model="form" label-width="140px" class="form">
      <el-divider content-position="left">WebHook</el-divider>
      <el-form-item label="Webhook Token">
        <el-input v-model="form.webhookToken" placeholder="与 GitLab Secret Token 一致；留空则不校验" clearable />
      </el-form-item>
      <el-form-item label="GitLab IP 白名单">
        <el-input
          v-model="form.gitlabAllowIps"
          type="textarea"
          :rows="2"
          placeholder="逗号分隔，如 192.168.1.10,10.0.0.5；留空则不限制"
        />
      </el-form-item>
      <el-divider content-position="left">邮件（SMTP）</el-divider>
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
        description="扫描失败时向项目配置的告警邮箱发送邮件；SMTP 与邮箱均在页面配置，无需改 application.yml。GitLab WebHook 地址：/api/webhook/gitlab"
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
  webhookToken: '',
  gitlabAllowIps: '',
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
    form.webhookToken = c.webhookToken || ''
    form.gitlabAllowIps = c.gitlabAllowIps || ''
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
      webhookToken: form.webhookToken || null,
      gitlabAllowIps: form.gitlabAllowIps || null,
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
