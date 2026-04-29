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
    redirect: '/active-scan/git-projects',
    children: [
      {
        path: 'system/mail',
        name: 'MailConfig',
        meta: { parentTitle: '系统配置管理', title: '邮件配置' },
        component: () => import('@/views/MailConfigView.vue'),
      },
      {
        path: 'active-scan/git-projects',
        name: 'GitProjects',
        meta: { parentTitle: 'Git仓库扫描管理', title: 'Git项目管理' },
        component: () => import('@/views/GitProjectListView.vue'),
      },
      {
        path: 'active-scan/repos',
        name: 'ActiveScanRepos',
        meta: { parentTitle: 'Git仓库扫描管理', title: 'Git项目配置' },
        component: () => import('@/views/ActiveScanRepoListView.vue'),
      },
      {
        path: 'active-scan/jobs',
        name: 'ActiveScanJobs',
        meta: { parentTitle: 'Git仓库扫描管理', title: '下发任务管理' },
        component: () => import('@/views/ActiveScanJobListView.vue'),
      },
      {
        path: 'active-scan/logs',
        name: 'ActiveScanLogs',
        meta: { parentTitle: 'Git仓库扫描管理', title: '下发任务日志' },
        component: () => import('@/views/ActiveScanLogListView.vue'),
      },
      {
        path: 'platform-skills',
        name: 'PlatformSkills',
        meta: { parentTitle: '技能管理', title: '平台技能' },
        component: () => import('@/views/PlatformSkillListView.vue'),
      },
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
