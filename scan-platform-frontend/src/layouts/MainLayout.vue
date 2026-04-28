<template>
  <el-container class="layout-root">
    <el-aside :width="asideWidth" class="aside">
      <div class="aside-inner">
        <div class="logo" :class="{ 'logo--collapsed': collapsed }">
          <div class="logo-brand">
            <el-icon class="logo-icon" :size="26"><Monitor /></el-icon>
            <span v-show="!collapsed" class="logo-text">代码扫描平台</span>
          </div>
        </div>
        <el-menu
          class="side-menu"
          :default-active="active"
          :collapse="collapsed"
          :collapse-transition="true"
          router
          background-color="#001529"
          text-color="rgba(255,255,255,0.75)"
          active-text-color="#fff"
        >
          <el-menu-item index="/projects">
            <el-icon><FolderOpened /></el-icon>
            <template #title>项目管理</template>
          </el-menu-item>
          <el-menu-item index="/sys-config">
            <el-icon><Setting /></el-icon>
            <template #title>系统配置</template>
          </el-menu-item>
          <el-menu-item index="/scan-logs">
            <el-icon><Document /></el-icon>
            <template #title>扫描日志</template>
          </el-menu-item>
        </el-menu>
        <div class="aside-footer">
          <el-tooltip :content="collapsed ? '展开菜单' : '收起菜单'" placement="right">
            <el-button
              class="collapse-btn"
              type="primary"
              link
              @click="collapsed = !collapsed"
            >
              <el-icon :size="20">
                <Expand v-if="collapsed" />
                <Fold v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
        </div>
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
import { Document, Expand, Fold, FolderOpened, Monitor, Setting } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

/** 侧边栏折叠：窄栏仅图标，宽栏图标+文案 */
const collapsed = ref(false)
const asideWidth = computed(() => (collapsed.value ? '64px' : '220px'))

const active = computed(() => route.path)
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
  transition: width 0.2s ease;
}
.aside-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.logo {
  flex-shrink: 0;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
}
.logo--collapsed .logo-brand {
  justify-content: center;
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
  letter-spacing: 0.02em;
}
.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
}
.side-menu:not(.el-menu--collapse) {
  width: 220px;
}
.aside-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.collapse-btn {
  color: rgba(255, 255, 255, 0.85) !important;
  padding: 4px 8px;
}
.collapse-btn:hover {
  color: #fff !important;
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
