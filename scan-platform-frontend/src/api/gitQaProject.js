import http from './http'
import { useUserStore } from '@/stores/user'

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

export function fetchGitQaChatMessages(projectId, page = 0, size = 200) {
  return http.get(`/ai-git-qa/projects/${projectId}/messages`, { params: { page, size } })
}

export function deleteGitQaChatMessage(projectId, messageId) {
  return http.delete(`/ai-git-qa/projects/${projectId}/messages/${messageId}`)
}

export function clearAllGitQaChatMessages(projectId) {
  return http.delete(`/ai-git-qa/projects/${projectId}/messages`)
}

/** 助手消息点赞/点踩：feedback 为 1、-1 或 null（清除） */
export function patchGitQaChatMessageFeedback(projectId, messageId, feedback) {
  return http.patch(`/ai-git-qa/projects/${projectId}/messages/${messageId}/feedback`, { feedback })
}

/**
 * SSE：POST /chat，返回 fetch Response（body 为 ReadableStream）。
 * 需自行解析 event/data；超时由调用方 AbortSignal 控制。
 */
export async function streamGitQaChat(id, body, signal) {
  const user = useUserStore()
  const res = await fetch(`/api/ai-git-qa/projects/${id}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(user.token ? { Authorization: `Bearer ${user.token}` } : {}),
    },
    body: JSON.stringify(body),
    signal,
  })
  if (!res.ok) {
    const t = await res.text().catch(() => '')
    throw new Error(t || `请求失败 ${res.status}`)
  }
  return res
}
