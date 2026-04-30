<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2 class="title">{{ t('app.loginTitle') }}</h2>
      <p class="sub">{{ t('app.loginSub') }}</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('login.username')" size="large" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.password')"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="onSubmit">{{
          t('login.submit')
        }}</el-button>
      </el-form>
      <p class="hint">{{ t('login.hint') }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const { t } = useI18n()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules = computed(() => ({
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
}))

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    user.setSession(res.token, res.username)
    ElMessage.success(t('login.success'))
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
  font-weight: 600;
  color: #303133;
}
.sub {
  margin: 0 0 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
.btn {
  width: 100%;
  margin-top: 4px;
}
.hint {
  margin: 16px 0 0;
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.5;
}
</style>
