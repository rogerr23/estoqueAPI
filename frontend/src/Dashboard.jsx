import { useEffect, useRef, useState } from 'react'
import { products, stores, stock, transfers, createProduct, createTransfer } from './services/api'

const sections = ['Produtos', 'Estoque', 'Transferências', 'Histórico', 'API']
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })

function Table({ headers, children, empty }) {
  return <div className="table-scroll"><table><thead><tr>{headers.map(h => <th key={h} scope="col">{h}</th>)}</tr></thead><tbody>{children}</tbody></table>{empty && <p className="empty">Nenhum registro encontrado.</p>}</div>
}

export default function Dashboard({ user, onLeave, onExpired, busy, apiTools, accountMessage }) {
  const [section, setSection] = useState('Produtos')
  const [data, setData] = useState({ products: [], stores: [], transfers: [] })
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [revision, setRevision] = useState(0)
  const [saving, setSaving] = useState(false)
  const lock = useRef(false)
  const expired = useRef(onExpired)
  useEffect(() => { expired.current = onExpired }, [onExpired])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [product, setProduct] = useState({ codigo: '', nome: '' })
  const [storeId, setStoreId] = useState('')
  const [balances, setBalances] = useState(null)
  const [stockError, setStockError] = useState('')
  const [movement, setMovement] = useState({ produtoId: '', lojaOrigemId: '', lojaDestinoId: '', quantidade: '' })
  const [preview, setPreview] = useState(null)
  const [previewError, setPreviewError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    // A consulta externa inicia um novo ciclo de carregamento.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    setLoadError('')
    Promise.all([products(controller.signal), stores(controller.signal), transfers(controller.signal)])
      .then(([p, s, t]) => { setData({ products: p, stores: s, transfers: t }); setStoreId(id => id || String(s[0]?.id || '')) })
      .catch(e => { if (e.name !== 'AbortError') { if (e.status === 401) expired.current(); else setLoadError(e.message) } })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [revision])

  useEffect(() => {
    const controller = new AbortController()
    // Remove o saldo anterior enquanto consulta a loja selecionada.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setBalances(null)
    setStockError('')
    if (storeId) stock(storeId, controller.signal).then(setBalances).catch(e => {
      if (e.name !== 'AbortError') { if (e.status === 401) expired.current(); else setStockError(e.message) }
    })
    return () => controller.abort()
  }, [storeId, revision])

  useEffect(() => {
    const controller = new AbortController()
    // Os saldos anteriores não representam a nova seleção.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPreview(null)
    setPreviewError('')
    if (movement.lojaOrigemId && movement.lojaDestinoId) {
      Promise.all([stock(movement.lojaOrigemId, controller.signal), stock(movement.lojaDestinoId, controller.signal)])
        .then(([origin, destination]) => setPreview({ origin, destination }))
        .catch(e => { if (e.name !== 'AbortError') { if (e.status === 401) expired.current(); else setPreviewError(e.message) } })
    }
    return () => controller.abort()
  }, [movement.lojaOrigemId, movement.lojaDestinoId, revision])

  function navigate(name) { setSection(name); setError(''); setNotice('') }
  function refresh() { setRevision(v => v + 1) }
  async function save(event, kind) {
    event.preventDefault()
    if (lock.current) return
    setError(''); setNotice('')
    if (kind === 'transfer') {
      if (movement.lojaOrigemId === movement.lojaDestinoId) { setError('Selecione lojas diferentes para origem e destino.'); return }
      if (!Number.isSafeInteger(Number(movement.quantidade)) || Number(movement.quantidade) <= 0) { setError('Informe uma quantidade inteira positiva.'); return }
    }
    lock.current = true
    setSaving(true)
    try {
      if (kind === 'product') {
        const created = await createProduct(product)
        setProduct({ codigo: '', nome: '' })
        setNotice(`Produto ${created.codigo} cadastrado.`)
      } else {
        const created = await createTransfer(Object.fromEntries(Object.entries(movement).map(([k, v]) => [k, Number(v)])))
        setMovement(v => ({ ...v, quantidade: '' }))
        setNotice(`Transferência #${created.id} concluída: ${created.quantidade} unidade(s) de ${created.produto.codigo}.`)
      }
      refresh()
    } catch (e) {
      if (e.status === 401) expired.current()
      else setError(e.campos ? `${e.message} ${Object.values(e.campos).join(' ')}` : e.message)
    } finally { lock.current = false; setSaving(false) }
  }
  function choose(field, value) { setMovement(v => ({ ...v, [field]: value })); setError(''); setNotice('') }
  const originBalance = preview?.origin.find(p => String(p.produtoId) === movement.produtoId)?.quantidade
  const destinationBalance = preview?.destination.find(p => String(p.produtoId) === movement.produtoId)?.quantidade
  const disabled = saving || loading || !!loadError
  const productOptions = data.products.map(p => <option key={p.id} value={p.id}>{p.codigo} — {p.nome}</option>)
  const storeOptions = data.stores.map(s => <option key={s.id} value={s.id}>{s.nome}</option>)

  return <section className="dashboard" aria-label="Gestão de estoque">
    <div className="session-bar"><span>{user.nome} <span className="muted">· {user.email}</span></span><button className="text-button" onClick={onLeave} disabled={busy || saving}>Sair</button></div>
    <nav className="navigation" aria-label="Módulos">{sections.map(name => <button key={name} aria-current={section === name ? 'page' : undefined} onClick={() => navigate(name)} disabled={saving}>{name}</button>)}</nav>
    <div className="page-heading"><div><h2>{section === 'API' ? 'Acesso à API' : section}</h2><p className="muted">{({ Produtos: 'Cadastre e consulte o catálogo de produtos.', Estoque: 'Consulte a quantidade disponível em cada loja.', Transferências: 'Movimente produtos entre lojas.', Histórico: 'Operações concluídas, da mais recente para a mais antiga.', API: 'Explore os endpoints usando a sessão atual.' })[section]}</p></div>{section !== 'API' && <button className="secondary-button compact" onClick={refresh} disabled={saving || loading}>Atualizar</button>}</div>
    {accountMessage}
    {error && <p className="message error" role="alert">{error}</p>}
    {notice && <p className="message success" role="status">{notice}</p>}
    {loadError && <p className="message error" role="alert">{loadError} Use Atualizar para tentar novamente.</p>}
    {loading && <p role="status" className="muted">Atualizando dados…</p>}
    {section === 'Produtos' && <>
      <form className="erp-form product-form" onSubmit={e => save(e, 'product')} aria-busy={saving}>
        <div><label htmlFor="product-code">Código</label><input id="product-code" required maxLength={60} value={product.codigo} onChange={e => setProduct(v => ({ ...v, codigo: e.target.value }))} disabled={disabled} /></div>
        <div><label htmlFor="product-name">Nome do produto</label><input id="product-name" required maxLength={150} value={product.nome} onChange={e => setProduct(v => ({ ...v, nome: e.target.value }))} disabled={disabled} /></div>
        <button className="primary-button compact" disabled={disabled}>{saving ? 'Cadastrando…' : 'Cadastrar produto'}</button>
      </form>
      {!loading && !loadError && <Table headers={['Código', 'Nome', 'ID']} empty={!data.products.length}>{data.products.map(p => <tr key={p.id}><td className="code">{p.codigo}</td><td>{p.nome}</td><td>{p.id}</td></tr>)}</Table>}
    </>}
    {section === 'Estoque' && <>
      <div className="filter"><label htmlFor="stock-store">Loja</label><select id="stock-store" value={storeId} onChange={e => setStoreId(e.target.value)} disabled={disabled}>{storeOptions}</select></div>
      {stockError && <p className="message error" role="alert">{stockError}</p>}
      {!stockError && storeId && !balances && <p role="status">Consultando estoque…</p>}
      {balances && !loading && !loadError && <Table headers={['Código', 'Produto', 'Quantidade']} empty={!balances.length}>{balances.map(p => <tr key={p.produtoId}><td className="code">{p.codigo}</td><td>{p.nome}</td><td className="quantity">{p.quantidade}</td></tr>)}</Table>}
      {!loading && !data.stores.length && <p>Nenhuma loja cadastrada.</p>}
    </>}
    {section === 'Transferências' && <>
      <form className="erp-form transfer-form" onSubmit={e => save(e, 'transfer')} aria-busy={saving}>
        <div className="wide"><label htmlFor="transfer-product">Produto</label><select id="transfer-product" required value={movement.produtoId} onChange={e => choose('produtoId', e.target.value)} disabled={disabled}><option value="">Selecione um produto</option>{productOptions}</select></div>
        <div><label htmlFor="transfer-origin">Loja de origem</label><select id="transfer-origin" required value={movement.lojaOrigemId} onChange={e => choose('lojaOrigemId', e.target.value)} disabled={disabled}><option value="">Selecione a origem</option>{storeOptions}</select></div>
        <div><label htmlFor="transfer-destination">Loja de destino</label><select id="transfer-destination" required value={movement.lojaDestinoId} onChange={e => choose('lojaDestinoId', e.target.value)} disabled={disabled}><option value="">Selecione o destino</option>{storeOptions}</select></div>
        {movement.produtoId && movement.lojaOrigemId && movement.lojaDestinoId && <p className="balance-preview wide" role="status">{preview ? `Saldo atual · Origem: ${originBalance ?? 0} · Destino: ${destinationBalance ?? 0}` : previewError || 'Consultando saldos…'}</p>}
        <div><label htmlFor="transfer-quantity">Quantidade</label><input id="transfer-quantity" type="number" min="1" max="2147483647" step="1" required value={movement.quantidade} onChange={e => choose('quantidade', e.target.value)} disabled={disabled} /></div>
        <button className="primary-button compact" disabled={disabled}>{saving ? 'Transferindo…' : 'Confirmar transferência'}</button>
      </form>
      <p className="muted">O saldo é conferido ao confirmar. O responsável e o horário são registrados automaticamente.</p>
    </>}
    {section === 'Histórico' && !loading && !loadError && <Table headers={['Nº', 'Data e hora', 'Produto', 'Origem', 'Destino', 'Quantidade', 'Responsável']} empty={!data.transfers.length}>{data.transfers.map(t => <tr key={t.id}><td>{t.id}</td><td className="nowrap">{dateFormat.format(new Date(t.dataHora))}</td><td><span className="code">{t.produto.codigo}</span><br />{t.produto.nome}</td><td>{t.lojaOrigem.nome}</td><td>{t.lojaDestino.nome}</td><td className="quantity">{t.quantidade}</td><td>{t.usuario.nome}</td></tr>)}</Table>}
    {section === 'API' && apiTools}
  </section>
}
