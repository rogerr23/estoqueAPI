import { useEffect, useState } from 'react'
import { csrfToken, currentUser, login, logout } from './services/api'
import './App.css'

export default function App() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [tokenFallback, setTokenFallback] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    currentUser(controller.signal)
      .then(setUser)
      .catch((failure) => {
        if (failure.name !== 'AbortError' && failure.status !== 401) setError(failure.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [])

  async function enter(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const authenticated = await login(email, senha)
      setSenha('')
      setUser(authenticated)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  async function leave() {
    if (busy) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await logout()
      setUser(null)
      setSenha('')
      setTokenFallback('')
      setNotice('Você saiu do sistema.')
    } catch (failure) {
      if (failure.status === 401) {
        setUser(null)
        setTokenFallback('')
        setNotice('Sua sessão terminou. Entre novamente.')
      } else {
        setError(failure.message)
      }
    } finally {
      setBusy(false)
    }
  }

  async function copyToken() {
    if (busy) return
    setBusy(true)
    setError('')
    setNotice('')
    setTokenFallback('')
    try {
      // Confere a sessão antes de entregar um token para operações autenticadas.
      await currentUser()
      const csrf = await csrfToken()
      try {
        await navigator.clipboard.writeText(csrf.token)
        setNotice('Token copiado. Cole no campo X-CSRF-TOKEN da operação no Swagger.')
      } catch {
        setTokenFallback(csrf.token)
        setNotice('Selecione e copie o token abaixo para usar no Swagger.')
      }
    } catch (failure) {
      if (failure.status === 401) {
        setUser(null)
        setNotice('Sua sessão terminou. Entre novamente.')
      } else {
        setError(failure.message)
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="shell">
      <header className="intro-panel">
        <div className="brand"><span className="brand-mark" aria-hidden="true">E</span>EstoqueAPI</div>
        <span className="intro-footer">Controle de estoque entre lojas</span>
      </header>

      <div className="workspace">
        {loading ? <p className="loading" role="status">Verificando sua sessão…</p> : user ? (
          <section className="account" aria-labelledby="account-title">
            <div className="account-header">
              <span className="eyebrow">SESSÃO ATIVA</span>
              <button className="text-button" onClick={leave} disabled={busy}>{busy ? 'Aguarde…' : 'Sair'}</button>
            </div>
            <h2 id="account-title">Olá, {user.nome}.</h2>
            <p className="muted">{user.email}</p>
            {error && <p className="message error" role="alert">{error}</p>}
            {notice && <p className="message success" role="status">{notice}</p>}
            <div className="api-card">
              <span className="card-icon" aria-hidden="true">↗</span>
              <h3>Explore o sistema pelo Swagger</h3>
              <p>Consulte produtos e lojas, faça transferências e veja o histórico usando sua sessão.</p>
              <a className="primary-button" href="/swagger-ui/index.html" target="_blank" rel="noopener noreferrer">Abrir Swagger <span aria-hidden="true">↗</span></a>
              <button className="secondary-button" onClick={copyToken} disabled={busy}>Copiar token CSRF</button>
              <p className="help">Para cadastrar ou transferir, copie o token e cole no campo <strong>X-CSRF-TOKEN</strong> do Swagger.</p>
              {tokenFallback && <label className="token-label">Token CSRF<textarea readOnly value={tokenFallback} onFocus={(event) => event.target.select()} /></label>}
            </div>
            <p className="next-step">As telas de produtos, estoque e transferências serão disponibilizadas no próximo marco.</p>
          </section>
        ) : (
          <section className="login-card" aria-labelledby="login-title">
            <span className="eyebrow">BEM-VINDO</span>
            <h2 id="login-title">Entre na sua conta</h2>
            <p className="muted">Use seu e-mail e senha para acessar o sistema.</p>
            {error && <p className="message error" role="alert">{error}</p>}
            {notice && <p className="message success" role="status">{notice}</p>}
            <form onSubmit={enter} aria-busy={busy}>
              <label htmlFor="email">E-mail</label>
              <input id="email" name="email" type="email" autoComplete="username" placeholder="seu@email.com" required value={email} onChange={(event) => setEmail(event.target.value)} disabled={busy} />
              <label htmlFor="senha">Senha</label>
              <input id="senha" name="senha" type="password" autoComplete="current-password" required value={senha} onChange={(event) => setSenha(event.target.value)} disabled={busy} />
              <button className="primary-button" type="submit" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
            </form>
          </section>
        )}
      </div>
    </main>
  )
}
