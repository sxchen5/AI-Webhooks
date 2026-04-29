import http from './http'

/** WebHook 项目配置下拉：id + 项目名称 */
export function fetchWebhookProjectOptions() {
  return http.get('/project-options/webhook-projects')
}
