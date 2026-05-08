/**
 * 解析 JWT payload（不校验签名，仅用于前端过期提示与路由守卫）。
 */
export function getJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const pad = (4 - (b64.length % 4)) % 4
    const padded = b64 + '='.repeat(pad)
    const binary = atob(padded)
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
    const json = new TextDecoder().decode(bytes)
    return JSON.parse(json)
  } catch {
    return null
  }
}

/** @param {number} [skewSeconds] 提前若干秒视为过期，避免边界请求失败 */
export function isJwtExpired(token, skewSeconds = 30) {
  const p = getJwtPayload(token)
  if (!p || typeof p.exp !== 'number') return false
  return Date.now() >= (p.exp - skewSeconds) * 1000
}
