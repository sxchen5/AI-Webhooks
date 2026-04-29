import http from './http'

export function fetchGitQaProjects(page, size) {
  return http.get('/ai-git-qa/projects', { params: { page, size } })
}

export function getGitQaProject(id) {
  return http.get(`/ai-git-qa/projects/${id}`)
}

export function createGitQaProject(data) {
  return http.post('/ai-git-qa/projects', data)
}

export function updateGitQaProject(id, data) {
  return http.put(`/ai-git-qa/projects/${id}`, data)
}

export function deleteGitQaProject(id) {
  return http.delete(`/ai-git-qa/projects/${id}`)
}

/** 同步执行 agent，可能较久；超时 2 小时与后端 Shell 默认一致 */
export function postGitQaChat(id, question) {
  return http.post(
    `/ai-git-qa/projects/${id}/chat`,
    { question },
    { timeout: 2 * 60 * 60 * 1000 },
  )
}
