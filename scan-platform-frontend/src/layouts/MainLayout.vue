<template>
  <el-container class="layout-root">
    <el-aside :width="asideWidth" class="aside" :class="{ 'aside--collapsed': collapsed }">
      <div class="aside-inner">
        <div v-if="!collapsed" class="logo-row">
          <div class="logo-brand">
            <el-icon class="logo-icon" :size="26"><Monitor /></el-icon>
            <span class="logo-text">{{ t('app.title') }}</span>
          </div>
          <el-tooltip :content="t('layout.collapseMenu')" placement="right">
            <el-button class="collapse-toggle" type="primary" link @click.stop="collapsed = true">
              <el-icon :size="20"><Fold /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <div v-else class="aside-expand-only">
          <el-tooltip :content="t('layout.expandMenu')" placement="right">
            <el-button class="collapse-toggle collapse-toggle--solo" type="primary" link @click.stop="collapsed = false">
              <el-icon :size="20"><Expand /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <el-menu
          class="side-menu"
          :class="{ 'menu-text-fade': true }"
          :default-active="active"
          :default-openeds="defaultOpenSubmenus"
          :collapse="collapsed"
          :collapse-transition="true"
          router
          background-color="#001529"
          text-color="rgba(255,255,255,0.75)"
          active-text-color="#fff"
        >
          <el-sub-menu index="ai-qa-mgmt">
            <template #title>
              <el-icon><ChatDotRound /></el-icon>
              <span class="submenu-title-text">{{ t('nav.aiQa') }}</span>
            </template>
            <el-menu-item index="/ai-git-qa/projects">
              <el-icon><FolderOpened /></el-icon>
              <template #title>{{ t('nav.gitQaProjects') }}</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="git-scan-mgmt">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span class="submenu-title-text">{{ t('nav.gitScan') }}</span>
            </template>
            <el-menu-item index="/active-scan/git-projects">
              <el-icon><FolderOpened /></el-icon>
              <template #title>{{ t('nav.gitProjects') }}</template>
            </el-menu-item>
            <el-menu-item index="/active-scan/repos">
              <el-icon><Link /></el-icon>
              <template #title>{{ t('nav.gitRepos') }}</template>
            </el-menu-item>
            <el-menu-item index="/active-scan/jobs">
              <el-icon><Timer /></el-icon>
              <template #title>{{ t('nav.jobs') }}</template>
            </el-menu-item>
            <el-menu-item index="/active-scan/logs">
              <el-icon><Clock /></el-icon>
              <template #title>{{ t('nav.logs') }}</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="skill-mgmt">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span class="submenu-title-text">{{ t('nav.skills') }}</span>
            </template>
            <el-menu-item index="/platform-skills">
              <el-icon><Collection /></el-icon>
              <template #title>{{ t('nav.platformSkills') }}</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="system-mgmt">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span class="submenu-title-text">{{ t('nav.system') }}</span>
            </template>
            <el-menu-item index="/system/mail">
              <el-icon><Message /></el-icon>
              <template #title>{{ t('nav.mail') }}</template>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="aside-user" :class="{ 'aside-user--collapsed': collapsed }">
          <div v-if="!collapsed" class="user-pill" @click="openSettings">
            <div class="user-pill-avatar" :class="{ 'user-pill-avatar--img': !!prefs.avatarDataUrl }">
              <img v-if="prefs.avatarDataUrl" :src="prefs.avatarDataUrl" alt="" />
              <el-icon v-else :size="20"><UserFilled /></el-icon>
            </div>
            <span class="user-pill-name">{{ user.username || '—' }}</span>
            <el-tooltip :content="t('layout.settings')" placement="top">
              <el-button class="user-pill-gear" type="primary" link @click.stop="openSettings">
                <el-icon :size="18"><Setting /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <div v-else class="user-pill-collapsed">
            <el-tooltip :content="user.username" placement="right">
              <div class="user-pill-avatar user-pill-avatar--sm" @click="openSettings">
                <img v-if="prefs.avatarDataUrl" :src="prefs.avatarDataUrl" alt="" />
                <el-icon v-else :size="18"><UserFilled /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip :content="t('layout.settings')" placement="right">
              <el-button class="user-pill-gear user-pill-gear--solo" type="primary" link @click.stop="openSettings">
                <el-icon :size="18"><Setting /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">
          <template v-if="breadcrumb.parent">
            <span class="header-parent">{{ breadcrumb.parent }}</span>
            <span class="header-sep">·</span>
            <span class="header-child">{{ breadcrumb.child }}</span>
          </template>
          <template v-else>{{ breadcrumb.child }}</template>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-drawer v-model="settingsOpen" :title="t('settings.title')" direction="rtl" size="360px" destroy-on-close>
      <div class="settings-body">
        <div class="settings-section">
          <div class="settings-label">{{ t('settings.avatar') }}</div>
          <p class="settings-hint">{{ t('settings.avatarHint') }}</p>
          <div class="avatar-preview-row">
            <div class="avatar-preview" :class="{ 'avatar-preview--empty': !draftAvatar }">
              <img v-if="draftAvatar" :src="draftAvatar" alt="" />
              <el-icon v-else :size="40"><UserFilled /></el-icon>
            </div>
            <div class="avatar-actions">
              <el-button size="small" @click="avatarInputRef?.click()">{{ t('settings.pickAvatar') }}</el-button>
              <el-button size="small" text type="danger" :disabled="!draftAvatar" @click="clearDraftAvatar">
                {{ t('settings.clearAvatar') }}
              </el-button>
            </div>
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="hidden-file"
            @change="onAvatarFile"
          />
        </div>

        <div class="settings-section">
          <div class="settings-label">{{ t('settings.language') }}</div>
          <el-radio-group v-model="draftLocale" size="small">
            <el-radio-button label="zh">{{ t('settings.langZh') }}</el-radio-button>
            <el-radio-button label="en">{{ t('settings.langEn') }}</el-radio-button>
          </el-radio-group>
        </div>

        <div class="settings-section">
          <div class="settings-label">{{ t('settings.theme') }}</div>
          <el-radio-group v-model="draftTheme" size="small">
            <el-radio-button label="light">{{ t('settings.themeLight') }}</el-radio-button>
            <el-radio-button label="dark">{{ t('settings.themeDark') }}</el-radio-button>
          </el-radio-group>
        </div>

        <div class="settings-actions">
          <el-button type="primary" block @click="applySettings">{{ t('settings.save') }}</el-button>
          <el-button block @click="onLogoutFromDrawer">{{ t('settings.logout') }}</el-button>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import {
  ChatDotRound,
  Clock,
  Collection,
  Expand,
  Fold,
  Folder,
  FolderOpened,
  Link,
  Message,
  Monitor,
  Setting,
  Timer,
  UserFilled,
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { usePreferencesStore } from '@/stores/preferences'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const prefs = usePreferencesStore()
const { t } = useI18n()

const collapsed = ref(false)
const asideWidth = computed(() => (collapsed.value ? '64px' : '240px'))
const settingsOpen = ref(false)
const avatarInputRef = ref(null)
const draftAvatar = ref(prefs.avatarDataUrl || '')
const draftLocale = ref(prefs.locale)
const draftTheme = ref(prefs.theme)

watch(settingsOpen, (open) => {
  if (open) {
    draftAvatar.value = prefs.avatarDataUrl || ''
    draftLocale.value = prefs.locale
    draftTheme.value = prefs.theme
  }
})

const active = computed(() => route.path)

const defaultOpenSubmenus = computed(() => {
  const p = route.path
  const open = []
  if (p.startsWith('/system')) open.push('system-mgmt')
  if (p.startsWith('/ai-git-qa')) open.push('ai-qa-mgmt')
  if (p.startsWith('/active-scan')) open.push('git-scan-mgmt')
  if (p.startsWith('/platform-skills')) open.push('skill-mgmt')
  return open
})

const breadcrumb = computed(() => {
  const i18n = route.meta?.i18n
  if (!i18n?.child) {
    return { parent: '', child: t('nav.console') }
  }
  const child = t(`nav.${i18n.child}`)
  const parent = i18n.parent ? t(`nav.${i18n.parent}`) : ''
  return { parent, child }
})

function openSettings() {
  settingsOpen.value = true
}

function onAvatarFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 500 * 1024) {
    ElMessage.warning(t('settings.avatarTooLarge'))
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    if (typeof reader.result === 'string') {
      draftAvatar.value = reader.result
    }
  }
  reader.readAsDataURL(file)
}

function clearDraftAvatar() {
  draftAvatar.value = ''
}

function applySettings() {
  prefs.setAvatarDataUrl(draftAvatar.value)
  prefs.setLocale(draftLocale.value)
  prefs.setTheme(draftTheme.value)
  ElMessage.success(t('settings.saved'))
  settingsOpen.value = false
}

async function onLogoutFromDrawer() {
  try {
    await ElMessageBox.confirm(t('settings.logoutConfirm'), t('settings.logout'), { type: 'warning' })
    settingsOpen.value = false
    user.logout()
    router.push({ name: 'Login' })
  } catch {
    /* cancel */
  }
}
</script>

<style scoped lang="scss">
.layout-root {
  height: 100vh;
}
.aside {
  background: #001529;
  color: #fff;
  overflow: hidden;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.aside-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.logo-row {
  flex-shrink: 0;
  min-height: 56px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.logo-icon {
  color: #69b1ff;
  flex-shrink: 0;
}
.logo-text {
  font-weight: 600;
  font-size: 15px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
}
.collapse-toggle {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.88) !important;
  padding: 6px !important;
  margin: 0 !important;
}
.collapse-toggle:hover {
  color: #fff !important;
}
.aside-expand-only {
  flex-shrink: 0;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.collapse-toggle--solo {
  width: 100%;
  justify-content: center;
}
.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
}
.side-menu:not(.el-menu--collapse) {
  width: 240px;
}
.menu-text-fade :deep(.el-sub-menu__title span:not(.el-icon)) {
  transition: opacity 0.22s ease, max-width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: inline-block;
  vertical-align: middle;
}
.el-menu--collapse.menu-text-fade :deep(.el-sub-menu__title .submenu-title-text) {
  opacity: 0;
  max-width: 0;
}
.el-menu:not(.el-menu--collapse).menu-text-fade :deep(.el-sub-menu__title .submenu-title-text) {
  opacity: 1;
  max-width: 200px;
}
.el-menu--collapse.menu-text-fade :deep(.el-menu-item .el-menu-tooltip__trigger span:last-child) {
  opacity: 0;
}

.aside-user {
  flex-shrink: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.15);
}
.aside-user--collapsed {
  padding: 10px 8px;
}
.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 4px 6px 4px 4px;
  border-radius: 10px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.92);
  transition: background 0.15s ease;
}
.user-pill:hover {
  background: rgba(255, 255, 255, 0.08);
}
.user-pill-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.12);
  overflow: hidden;
  color: #69b1ff;
}
.user-pill-avatar--img {
  padding: 0;
}
.user-pill-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.user-pill-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-pill-gear {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.85) !important;
  padding: 4px !important;
  margin: 0 !important;
}
.user-pill-gear:hover {
  color: #fff !important;
}
.user-pill-collapsed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.user-pill-avatar--sm {
  width: 32px;
  height: 32px;
  cursor: pointer;
}
.user-pill-gear--solo {
  padding: 2px !important;
}

.header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
}
.header-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.header-parent {
  color: #909399;
  font-weight: 400;
}
.header-sep {
  margin: 0 8px;
  color: #c0c4cc;
  font-weight: 400;
}
.header-child {
  font-weight: 600;
}
.main {
  padding: 16px;
  background: #f0f2f5;
}

.settings-body {
  padding: 0 4px 16px;
}
.settings-section {
  margin-bottom: 22px;
}
.settings-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.settings-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.avatar-preview-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}
.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.hidden-file {
  display: none;
}
.settings-actions {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>

<style lang="scss">
html.dark .layout-root .header {
  background: var(--el-bg-color);
  border-bottom-color: var(--el-border-color);
}
html.dark .layout-root .header-title,
html.dark .layout-root .header-child {
  color: var(--el-text-color-primary);
}
html.dark .layout-root .header-parent {
  color: var(--el-text-color-secondary);
}
html.dark .layout-root .main {
  background: var(--el-bg-color-page);
}
</style>
