export function isTokenExpired(token: string): boolean {
  try {
    const parts = token.split('.')
    if (parts.length < 2) return true
    const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const pad = b64.length % 4 === 0 ? '' : '='.repeat(4 - (b64.length % 4))
    const payload = JSON.parse(atob(b64 + pad)) as { exp?: number }
    if (typeof payload.exp !== 'number') return true
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}
