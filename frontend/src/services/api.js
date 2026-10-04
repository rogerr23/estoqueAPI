export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(path, { credentials: 'same-origin', ...options })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('Não foi possível conectar ao sistema. Tente novamente.', 0)
  }
  if (response.status === 204) return null
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(data?.mensagem || 'Não foi possível concluir a operação.', response.status)
  }
  return data
}

export function currentUser(signal) {
  return request('/api/auth/me', { signal })
}

export function csrfToken() {
  return request('/api/auth/csrf', { cache: 'no-store' })
}

export async function login(email, senha) {
  const csrf = await csrfToken()
  const user = await request('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      [csrf.headerName]: csrf.token,
    },
    body: new URLSearchParams({ email: email.trim(), senha }),
  })
  // O Spring Security invalida o token anterior ao autenticar.
  // As próximas operações sempre obtêm um novo token da sessão atual.
  return user
}

export async function logout() {
  const csrf = await csrfToken()
  return request('/api/auth/logout', {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token },
  })
}
