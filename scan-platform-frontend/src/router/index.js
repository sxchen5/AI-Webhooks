import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { isJwtExpired } from '@/utils/jwt'
import { runSessionExpiredFlow } from '@/utils/sessionExpired'

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
        path: 'system/agent-models',
        name: 'AgentModelConfig',
        meta: { i18n: { parent: 'system', child: 'agentModels' } },
        component: () => import('@/views/AgentModelConfigView.vue'),
      },
      {
        path: 'system/mail',
        name: 'MailConfig',
        meta: { i18n: { parent: 'system', child: 'mail' } },
        component: () => import('@/views/MailConfigView.vue'),
      },
      {
        path: 'active-scan/git-projects',
        name: 'GitProjects',
        meta: { i18n: { parent: 'gitScan', child: 'gitProjects' } },
        component: () => import('@/views/GitProjectListView.vue'),
      },
      {
        path: 'active-scan/repos',
        name: 'ActiveScanRepos',
        meta: { i18n: { parent: 'gitScan', child: 'gitRepos' } },
        component: () => import('@/views/ActiveScanRepoListView.vue'),
      },
      {
        path: 'active-scan/jobs',
        name: 'ActiveScanJobs',
        meta: { i18n: { parent: 'gitScan', child: 'jobs' } },
        component: () => import('@/views/ActiveScanJobListView.vue'),
      },
      {
        path: 'active-scan/logs',
        name: 'ActiveScanLogs',
        meta: { i18n: { parent: 'gitScan', child: 'logs' } },
        component: () => import('@/views/ActiveScanLogListView.vue'),
      },
      {
        path: 'platform-skills',
        name: 'PlatformSkills',
        meta: { i18n: { parent: 'skills', child: 'platformSkills' } },
        component: () => import('@/views/PlatformSkillListView.vue'),
      },
      {
        path: 'ai-git-qa/projects',
        name: 'GitQaProjects',
        meta: { i18n: { parent: 'aiQa', child: 'gitQaProjects' } },
        component: () => import('@/views/GitQaProjectListView.vue'),
      },
      {
        path: 'ai-git-qa/chat/:id',
        name: 'GitQaChat',
        meta: { i18n: { parent: 'aiQa', child: 'chat' } },
        component: () => import('@/views/GitProjectAiQaChatView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const user = useUserStore()

  if (to.meta.public) {
    if (to.name === 'Login' && user.token) {
      if (isJwtExpired(user.token)) {
        user.logout()
        return true
      }
      return { path: '/' }
    }
    return true
  }

  if (!user.token) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (isJwtExpired(user.token)) {
    user.logout()
    await runSessionExpiredFlow()
    return false
  }

  return true
})

export default router
