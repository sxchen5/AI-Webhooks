<template>
  <el-container class="layout-root">
    <el-aside :width="asideWidth" class="aside">
      <div class="aside-inner">
        <div class="logo" :class="{ 'logo--collapsed': collapsed }">
          <div class="logo-brand">
            <el-icon class="logo-icon" :size="collapsed ? 24 : 26"><Monitor /></el-icon>
            <span v-show="!collapsed" class="logo-text">代码扫描平台</span>
          </div>
          <el-tooltip :content="collapsed ? '展开菜单' : '收起菜单'" placement="right">
            <el-button
              class="collapse-btn"
              :class="{ 'collapse-btn--collapsed': collapsed }"
              type="primary"
              link
              @click.stop="collapsed = !collapsed"
            >
              <el-icon :size="collapsed ? 20 : 18">
                <Expand v-if="collapsed" />
                <Fold v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
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
          <el-menu-item index="/active-scan/repos">
            <el-icon><Link /></el-icon>
            <template #title>主动扫描仓库</template>
          </el-menu-item>
          <el-menu-item index="/active-scan/jobs">
            <el-icon><Timer /></el-icon>
            <template #title>主动扫描任务</template>
          </el-menu-item>
          <el-menu-item index="/active-scan/logs">
            <el-icon><Clock /></el-icon>
            <template #title>主动扫描日志</template>
          </el-menu-item>
          <el-menu-item index="/platform-skills">
            <el-icon><Collection /></el-icon>
            <template #title>平台技能</template>
          </el-menu-item>
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
import { Clock, Collection, Document, Expand, Fold, FolderOpened, Link, Monitor, Setting, Timer } from '@element-plus/icons-vue'
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
  justify-content: space-between;
  gap: 8px;
  padding: 0 6px 0 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-brand {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
/* 折叠：纵向排列，避免 64px 内两图标横向挤在一起 */
.logo--collapsed {
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
  padding: 14px 6px 12px;
  min-height: 88px;
}
.logo--collapsed .logo-brand {
  flex: 0 0 auto;
  width: 100%;
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
.collapse-btn {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.85) !important;
  padding: 4px;
  margin: 0;
  min-height: auto;
}
.collapse-btn--collapsed {
  width: 36px;
  height: 36px;
  padding: 0 !important;
  border-radius: 8px;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}
.collapse-btn--collapsed:hover {
  background: rgba(105, 177, 255, 0.15) !important;
  border-color: rgba(105, 177, 255, 0.35) !important;
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
