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
      { path: 'projects', name: 'Projects', component: () => import('@/views/ProjectListView.vue') },
      { path: 'sys-config', name: 'SysConfig', component: () => import('@/views/SysConfigView.vue') },
      { path: 'scan-logs', name: 'ScanLogs', component: () => import('@/views/ScanLogListView.vue') },
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
