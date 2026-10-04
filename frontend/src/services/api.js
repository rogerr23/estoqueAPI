export class ApiError extends Error {
  constructor(message, status, campos) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.campos = campos
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
    throw new ApiError(data?.mensagem || 'Não foi possível concluir a operação.', response.status, data?.campos)
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

export const products = (signal) => request('/api/produtos', { signal })
export const stores = (signal) => request('/api/lojas', { signal })
export const stock = (id, signal) => request(`/api/lojas/${id}/estoque`, { signal })
export const transfers = (signal) => request('/api/transferencias', { signal })

async function post(path, data) {
  const csrf = await csrfToken()
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: JSON.stringify(data),
  })
}
export const createProduct = (data) => post('/api/produtos', data)
export const createTransfer = (data) => post('/api/transferencias', data)
