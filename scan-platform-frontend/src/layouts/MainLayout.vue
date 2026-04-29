<template>
  <el-container class="layout-root">
    <el-aside :width="asideWidth" class="aside" :class="{ 'aside--collapsed': collapsed }">
      <div class="aside-inner">
        <div class="logo-row">
          <div class="logo-brand">
            <el-icon class="logo-icon" :size="26"><Monitor /></el-icon>
            <span class="logo-text">{{ collapsed ? '' : '代码扫描平台' }}</span>
          </div>
          <el-tooltip :content="collapsed ? '展开菜单' : '收起菜单'" placement="right">
            <el-button
              class="collapse-toggle"
              :class="{ 'collapse-toggle--mini': collapsed }"
              type="primary"
              link
              @click.stop="collapsed = !collapsed"
            >
              <el-icon :size="20">
                <Expand v-if="collapsed" />
                <Fold v-else />
              </el-icon>
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
          <el-sub-menu index="system-mgmt">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span class="submenu-title-text">系统配置管理</span>
            </template>
            <el-menu-item index="/system/mail">
              <el-icon><Message /></el-icon>
              <template #title>邮件配置</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="git-scan-mgmt">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span class="submenu-title-text">Git仓库扫描管理</span>
            </template>
            <el-menu-item index="/active-scan/git-projects">
              <el-icon><FolderOpened /></el-icon>
              <template #title>Git项目管理</template>
            </el-menu-item>
            <el-menu-item index="/active-scan/repos">
              <el-icon><Link /></el-icon>
              <template #title>Git项目配置</template>
            </el-menu-item>
            <el-menu-item index="/active-scan/jobs">
              <el-icon><Timer /></el-icon>
              <template #title>下发任务管理</template>
            </el-menu-item>
            <el-menu-item index="/active-scan/logs">
              <el-icon><Clock /></el-icon>
              <template #title>下发任务日志</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="skill-mgmt">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span class="submenu-title-text">技能管理</span>
            </template>
            <el-menu-item index="/platform-skills">
              <el-icon><Collection /></el-icon>
              <template #title>平台技能</template>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ title }}</div>
        <div class="header-right">
          <span class="user-name">{{ user.username }}</span>
          <el-button type="primary" link @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Clock, Collection, Expand, Fold, Folder, FolderOpened, Link, Message, Monitor, Setting, Timer } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const collapsed = ref(false)
const asideWidth = computed(() => (collapsed.value ? '64px' : '240px'))

const active = computed(() => route.path)

const defaultOpenSubmenus = computed(() => {
  const p = route.path
  const open = []
  if (p.startsWith('/system')) {
    open.push('system-mgmt')
  }
  if (p.startsWith('/active-scan')) {
    open.push('git-scan-mgmt')
  }
  if (p.startsWith('/platform-skills')) {
    open.push('skill-mgmt')
  }
  return open
})

const title = computed(() => route.meta.title || '控制台')

function onLogout() {
  user.logout()
  router.push({ name: 'Login' })
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
}
/* 顶栏：平台图标 + 标题与「展开/收起」同一行，收起时图标与按钮并排，不与下方一级菜单图标挤成一列 */
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
  opacity: 1;
  max-width: 200px;
  transition: opacity 0.22s ease, max-width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.aside--collapsed .logo-text {
  opacity: 0;
  max-width: 0;
}
.collapse-toggle {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.88) !important;
  padding: 6px !important;
  margin: 0 !important;
}
.collapse-toggle--mini {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}
.collapse-toggle:hover {
  color: #fff !important;
}
.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
}
.side-menu:not(.el-menu--collapse) {
  width: 240px;
}
/* 展开/收起时子菜单标题文字渐显隐（深度选择器） */
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
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
}
.header-title {
  font-size: 16px;
  font-weight: 500;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  color: #666;
  font-size: 14px;
}
.main {
  padding: 16px;
  background: #f0f2f5;
}
</style>
