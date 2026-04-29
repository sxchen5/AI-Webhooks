import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/projects',
    children: [
      { path: 'projects', name: 'Projects', meta: { title: '仓库管理' }, component: () => import('@/views/ProjectListView.vue') },
      { path: 'sys-config', name: 'SysConfig', meta: { title: '系统配置' }, component: () => import('@/views/SysConfigView.vue') },
      { path: 'scan-logs', name: 'ScanLogs', meta: { title: '扫描日志' }, component: () => import('@/views/ScanLogListView.vue') },
      { path: 'active-scan/repos', name: 'ActiveScanRepos', meta: { title: '仓库管理' }, component: () => import('@/views/ActiveScanRepoListView.vue') },
      { path: 'active-scan/jobs', name: 'ActiveScanJobs', meta: { title: '下发任务管理' }, component: () => import('@/views/ActiveScanJobListView.vue') },
      { path: 'active-scan/logs', name: 'ActiveScanLogs', meta: { title: '下发任务日志' }, component: () => import('@/views/ActiveScanLogListView.vue') },
      { path: 'platform-skills', name: 'PlatformSkills', meta: { title: '平台技能' }, component: () => import('@/views/PlatformSkillListView.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (!to.meta.public && !user.token) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'Login' && user.token) {
    return { path: '/' }
  }
})

export default router
