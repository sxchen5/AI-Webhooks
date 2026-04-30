<template>
  <div class="login-page" :class="{ 'login-page--dark': isDark }">
    <el-card class="login-card" shadow="hover">
      <h2 class="title">{{ t('app.loginTitle') }}</h2>
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
        <el-form-item prop="captchaCode" class="captcha-row">
          <el-input
            v-model="form.captchaCode"
            :placeholder="t('login.captchaPlaceholder')"
            size="large"
            maxlength="8"
            clearable
            class="captcha-input"
            @keyup.enter="onSubmit"
          />
          <div class="captcha-img-wrap" @click="refreshCaptcha" :title="t('login.captchaRefresh')">
            <img v-if="captchaDataUrl" :src="captchaDataUrl" alt="" class="captcha-img" />
            <span v-else class="captcha-placeholder">{{ t('login.captcha') }}</span>
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="onSubmit">{{
          t('login.submit')
        }}</el-button>
      </el-form>
      <p class="captcha-hint">{{ t('login.captchaRefresh') }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { login, fetchCaptcha } from '@/api/auth'
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
const captchaId = ref('')
const captchaDataUrl = ref('')

const form = reactive({
  username: 'admin',
  password: 'admin123',
  captchaCode: '',
})

const rules = computed(() => ({
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
  captchaCode: [{ required: true, message: t('login.captchaRequired'), trigger: 'blur' }],
}))

async function refreshCaptcha() {
  try {
    const res = await fetchCaptcha()
    captchaId.value = res.captchaId || ''
    captchaDataUrl.value = res.imageBase64 ? `data:image/png;base64,${res.imageBase64}` : ''
    form.captchaCode = ''
  } catch {
    captchaId.value = ''
    captchaDataUrl.value = ''
    ElMessage.error(t('login.captchaLoadFailed'))
  }
}

onMounted(() => {
  refreshCaptcha()
})

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login({
      username: form.username,
      password: form.password,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode,
    })
    user.setSession(res.token, res.username)
    ElMessage.success(t('login.success'))
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } catch (e) {
    await refreshCaptcha()
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
  margin: 0 0 28px;
  text-align: center;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 0.02em;
}
.btn {
  width: 100%;
  margin-top: 4px;
}

.captcha-row :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}
.captcha-input {
  flex: 1;
  min-width: 0;
}
.captcha-img-wrap {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  overflow: hidden;
  cursor: pointer;
  background: var(--el-fill-color-blank, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.captcha-placeholder {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}
.captcha-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.5;
}

html.dark .login-page .title {
  color: var(--el-text-color-primary, #e5eaf3);
}
html.dark .login-page .captcha-hint {
  color: var(--el-text-color-secondary, #a3a3a3);
}
</style>
