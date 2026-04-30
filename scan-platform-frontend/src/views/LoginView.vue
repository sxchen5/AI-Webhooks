<template>
  <div class="login-page" :class="{ 'login-page--dark': isDark }">
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
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { usePreferencesStore } from '@/stores/preferences'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const prefs = usePreferencesStore()
const { theme } = storeToRefs(prefs)
const isDark = computed(() => theme.value === 'dark')
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
  --login-overlay: rgba(247, 247, 247, 0.72);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  background-color: #eef1f6;
  background-image: url('/login-bg-light.svg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--login-overlay);
  pointer-events: none;
}

.login-page--dark {
  --login-overlay: rgba(10, 12, 18, 0.78);
  background-color: #0a0c10;
  background-image: url('/login-bg-dark.svg');
}

.login-page > * {
  position: relative;
  z-index: 1;
}

.login-card {
  width: 100%;
  max-width: 400px;
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

html.dark .login-page .title {
  color: var(--el-text-color-primary, #e5eaf3);
}
html.dark .login-page .sub,
html.dark .login-page .hint {
  color: var(--el-text-color-secondary, #a3a3a3);
}
</style>
