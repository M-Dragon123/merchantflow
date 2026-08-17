export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('merchantflow_access_token')
  let response: Response
  try {
    response = await fetch(path, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    })
  } catch {
    throw new Error('无法连接服务器，请确认后端已启动（dev-backend.ps1）')
  }
  const text = await response.text()
  let body: unknown = null
  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      body = null
    }
  }
  if (!response.ok) {
    const data = body as { detail?: string; message?: string; error?: string; data?: { message?: string } } | null
    const message =
      data?.data?.message || data?.detail || data?.message || data?.error || `请求失败（HTTP ${response.status}）`
    // 仅当本次请求确实携带了 token 时，401 才视为会话过期；登录失败（凭证错误）不应触发登出跳转
    if (response.status === 401 && token) {
      localStorage.removeItem('merchantflow_access_token')
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login'
    }
    throw new Error(message)
  }
  return (body as { data: T } | null)?.data as T
}
