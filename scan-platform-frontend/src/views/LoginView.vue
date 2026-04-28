<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2 class="title">代码扫描管理平台</h2>
      <p class="sub">GitLab WebHook 触发 · 异步扫描 · 邮件告警</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="onSubmit">登录</el-button>
      </el-form>
      <p class="hint">默认账号 admin / admin123（首次启动由数据库初始化）</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    user.setSession(res.token, res.username)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d39c4 0%, #10239e 50%, #061178 100%);
}
.login-card {
  width: 400px;
  padding: 8px 8px 4px;
}
.title {
  margin: 0 0 8px;
  text-align: center;
  font-size: 22px;
}
.sub {
  margin: 0 0 24px;
  text-align: center;
  color: #888;
  font-size: 13px;
}
.btn {
  width: 100%;
  margin-top: 8px;
}
.hint {
  margin: 16px 0 0;
  font-size: 12px;
  color: #999;
  text-align: center;
}
</style>
