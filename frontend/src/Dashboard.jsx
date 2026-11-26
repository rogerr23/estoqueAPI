import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { products, stores, stock, transfers, productEdits, entries, createProduct, createTransfer, createEntry, deleteProduct, deleteTransfer } from './services/api'
import Funcionarios from './Funcionarios'
import ProdutoEditor from './ProdutoEditor'
import Painel from './Painel'
import Lojas from './Lojas'
import useAutoClear from './useAutoClear'
import { Search, SortableTable, Pager, MobileCollapse } from './tableTools'
import useMediaQuery from './useMediaQuery'
import { matches, sortRows, paginate, downloadCsv, inPeriod } from './tableUtils'

const baseSections = ['Painel', 'Produtos', 'Estoque', 'Entradas', 'Transferências', 'Histórico', 'Edições', 'API']
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
const editLabels = { EDICAO: 'Edição', EXCLUSAO_PRODUTO: 'Exclusão de produto', EXCLUSAO_TRANSFERENCIA: 'Exclusão de transferência' }
const descriptions = {
  Painel: 'Resumo do estoque e das últimas atividades.', Produtos: 'Cadastre e consulte o catálogo de produtos.',
  Estoque: 'Consulte a quantidade disponível em cada loja.', Entradas: 'Registre mercadoria que chegou a uma loja.',
  Transferências: 'Movimente produtos entre lojas.', Histórico: 'Operações concluídas, da mais recente para a mais antiga.',
  Edições: 'Alterações e exclusões feitas pelos administradores.', Lojas: 'Cadastre novas lojas e ajuste os nomes.',
  Funcionários: 'Cadastre funcionários e acompanhe o que cada um fez.', API: 'Explore os endpoints usando a sessão atual.',
}
// Ordem inicial de cada aba (o usuário pode mudar clicando nas colunas).
const defaultSort = {
  Produtos: { key: 'codigo', dir: 'asc' }, Estoque: { key: 'codigo', dir: 'asc' },
  Entradas: { key: 'data', dir: 'desc' }, Histórico: { key: 'data', dir: 'desc' }, Edições: { key: 'data', dir: 'desc' },
}

export default function Dashboard({ user, onLeave, onExpired, busy, apiTools, accountMessage }) {
  const isAdmin = user.perfil === 'ADMIN'
  const isMobile = useMediaQuery('(max-width: 700px)')
  const sections = isAdmin ? [...baseSections.slice(0, -1), 'Lojas', 'Funcionários', 'API'] : baseSections
  const [section, setSection] = useState('Painel')
  const [data, setData] = useState({ products: [], stores: [], transfers: [], edits: [], entries: [], stocks: {} })
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [revision, setRevision] = useState(0)
  const [saving, setSaving] = useState(false)
  const lock = useRef(false)
  const expired = useRef(onExpired)
  useEffect(() => { expired.current = onExpired }, [onExpired])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useAutoClear(notice, setNotice, 4000)
  useAutoClear(error, setError, 8000)
  const [product, setProduct] = useState({ codigo: '', nome: '', lojaId: '', quantidade: '', estoqueMinimo: '' })
  const [movement, setMovement] = useState({ produtoId: '', lojaOrigemId: '', lojaDestinoId: '', quantidade: '' })
  const [editingProduct, setEditingProduct] = useState(null)
  const [entry, setEntry] = useState({ produtoId: '', lojaId: '', quantidade: '', observacao: '' })
  // Busca, filtros, ordem e página ficam guardados por aba: voltar à aba mantém o que estava.
  const [ui, setUi] = useState({ Estoque: { store: 'all' } })
  const tab = ui[section] || {}
  const patchTab = useCallback((name, changes) => setUi(v => ({ ...v, [name]: { ...(v[name] || {}), ...changes } })), [])
  const patch = (changes) => patchTab(section, changes)
  const sortOf = (name) => (ui[name]?.sort === undefined ? defaultSort[name] : ui[name].sort)

  useEffect(() => {
    const controller = new AbortController()
    // A consulta externa inicia um novo ciclo de carregamento.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    setLoadError('')
    Promise.all([products(controller.signal), stores(controller.signal), transfers(controller.signal), productEdits(controller.signal), entries(controller.signal)])
      .then(async ([p, s, t, e, en]) => {
        // Saldos de todas as lojas alimentam painel, produtos, estoque e transferências.
        const saldos = await Promise.all(s.map(loja => stock(loja.id, controller.signal)))
        setData({ products: p, stores: s, transfers: t, edits: e, entries: en, stocks: Object.fromEntries(s.map((loja, i) => [loja.id, saldos[i]])) })
      })
      .catch(e => { if (e.name !== 'AbortError') { if (e.status === 401) expired.current(); else setLoadError(e.message) } })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [revision])

  const contentRef = useRef(null)
  const navRef = useRef(null)
  // Mantém a aba ativa visível quando a faixa de abas rola no celular.
  useEffect(() => { navRef.current?.querySelector('[aria-current]')?.scrollIntoView({ block: 'nearest', inline: 'nearest' }) }, [section])
  // Cada célula recebe o nome da coluna, usado quando a tabela vira lista no celular.
  useEffect(() => {
    const root = contentRef.current
    if (!root) return
    const label = () => root.querySelectorAll('table').forEach(table => {
      const names = [...table.querySelectorAll('thead th')].map(th => th.textContent.replace(/[▲▼↕]/g, '').trim())
      table.querySelectorAll('tbody tr').forEach(tr => [...tr.children].forEach((td, i) => { if (td.dataset.label !== (names[i] ?? '')) td.dataset.label = names[i] ?? '' }))
    })
    label()
    // Funcionários carrega a própria tabela; o observador cobre linhas que chegam depois.
    const observer = new MutationObserver(label)
    observer.observe(root, { childList: true, subtree: true })
    return () => observer.disconnect()
  }, [])

  function goTo(name, changes) {
    setSection(name); setError(''); setNotice(''); setEditingProduct(null)
    if (changes) patchTab(name, changes)
  }
  function fail(e) { if (e.status === 401) expired.current(); else setError(e.campos ? `${e.message} ${Object.values(e.campos).join(' ')}` : e.message) }
  const onExpiredStable = useCallback(() => expired.current(), [])
  function refresh() { setRevision(v => v + 1) }

  // Saldos por produto, somando todas as lojas.
  const totals = useMemo(() => {
    const map = {}
    Object.values(data.stocks).forEach(list => list.forEach(p => { map[p.produtoId] = (map[p.produtoId] || 0) + p.quantidade }))
    return map
  }, [data.stocks])
  const isLow = (p) => p.estoqueMinimo > 0 && (totals[p.id] || 0) < p.estoqueMinimo
  const balanceOf = (lojaId, produtoId) => data.stocks[lojaId]?.find(p => String(p.produtoId) === String(produtoId))?.quantidade

  const originBalance = movement.produtoId && movement.lojaOrigemId ? balanceOf(movement.lojaOrigemId, movement.produtoId) ?? 0 : undefined
  const destinationBalance = movement.produtoId && movement.lojaDestinoId ? balanceOf(movement.lojaDestinoId, movement.produtoId) ?? 0 : undefined

  async function save(event, kind) {
    event.preventDefault()
    if (lock.current) return
    setError(''); setNotice('')
    if (kind === 'entry' && (!Number.isSafeInteger(Number(entry.quantidade)) || Number(entry.quantidade) <= 0)) { setError('Informe uma quantidade inteira positiva.'); return }
    if (kind === 'transfer') {
      if (movement.lojaOrigemId === movement.lojaDestinoId) { setError('Selecione lojas diferentes para origem e destino.'); return }
      if (!Number.isSafeInteger(Number(movement.quantidade)) || Number(movement.quantidade) <= 0) { setError('Informe uma quantidade inteira positiva.'); return }
      if (Number(movement.quantidade) > (originBalance ?? 0)) { setError(`A loja de origem tem só ${originBalance ?? 0} unidade(s) desse produto.`); return }
    }
    if (kind === 'product') {
      const quantidade = product.quantidade === '' ? 0 : Number(product.quantidade)
      const minimo = product.estoqueMinimo === '' ? 0 : Number(product.estoqueMinimo)
      if (!Number.isSafeInteger(quantidade) || quantidade < 0 || !Number.isSafeInteger(minimo) || minimo < 0) { setError('Informe quantidades inteiras, zero ou maiores.'); return }
    }
    lock.current = true
    setSaving(true)
    try {
      if (kind === 'product') {
        const quantidade = product.quantidade === '' ? 0 : Number(product.quantidade)
        const loja = data.stores.find(s => String(s.id) === product.lojaId)
        const created = await createProduct({ codigo: product.codigo, nome: product.nome, lojaId: product.lojaId ? Number(product.lojaId) : null, quantidade, estoqueMinimo: product.estoqueMinimo === '' ? 0 : Number(product.estoqueMinimo) })
        setProduct(v => ({ codigo: '', nome: '', lojaId: v.lojaId, quantidade: '', estoqueMinimo: '' }))
        setNotice(loja ? `Produto ${created.codigo} cadastrado com ${quantidade} unidade(s) na ${loja.nome}.` : `Produto ${created.codigo} cadastrado.`)
      } else if (kind === 'entry') {
        const created = await createEntry({ produtoId: Number(entry.produtoId), lojaId: Number(entry.lojaId), quantidade: Number(entry.quantidade), observacao: entry.observacao })
        setEntry(v => ({ ...v, quantidade: '', observacao: '' }))
        setNotice(`Entrada registrada: ${created.quantidade} unidade(s) de ${created.produto.codigo} na ${created.loja.nome}.`)
      } else {
        const created = await createTransfer(Object.fromEntries(Object.entries(movement).map(([k, v]) => [k, Number(v)])))
        setMovement(v => ({ ...v, quantidade: '' }))
        setNotice(`Transferência #${created.id} concluída: ${created.quantidade} unidade(s) de ${created.produto.codigo}.`)
      }
      refresh()
    } catch (e) { fail(e) } finally { lock.current = false; setSaving(false) }
  }
  async function remove(kind, item) {
    const question = kind === 'product'
      ? `Excluir o produto ${item.codigo}? Ele sai das listas e do estoque, mas continua no histórico.`
      : `Excluir a transferência #${item.id}? As ${item.quantidade} unidade(s) voltam para ${item.lojaOrigem.nome}.`
    if (lock.current || !window.confirm(question)) return
    lock.current = true
    setSaving(true); setError(''); setNotice('')
    try {
      if (kind === 'product') { await deleteProduct(item.id); setEditingProduct(null); setNotice(`Produto ${item.codigo} excluído.`) }
      else { await deleteTransfer(item.id); setNotice(`Transferência #${item.id} excluída e estoque devolvido.`) }
      refresh()
    } catch (e) { fail(e) } finally { lock.current = false; setSaving(false) }
  }
  function choose(field, value) { setMovement(v => ({ ...v, [field]: value })); setError(''); setNotice('') }

  const disabled = saving || loading || !!loadError
  const productOptions = data.products.map(p => <option key={p.id} value={p.id}>{p.codigo} — {p.nome}</option>)
  const storeOptions = data.stores.map(s => <option key={s.id} value={s.id}>{s.nome}</option>)
  const ready = !loading && !loadError
  const searchBox = (placeholder) => <Search id={`search-${section}`} value={tab.query || ''} onChange={query => patch({ query, page: 1 })} placeholder={placeholder} />
  const sortProps = (name) => ({ sort: sortOf(name), onSort: sort => patchTab(name, { sort, page: 1 }) })

  // Produtos
  const productRows = sortRows(data.products.filter(p => matches(ui.Produtos?.query, p.codigo, p.nome) && (!ui.Produtos?.lowOnly || isLow(p))),
    sortOf('Produtos'), { codigo: p => p.codigo, nome: p => p.nome, total: p => totals[p.id] || 0, minimo: p => p.estoqueMinimo })
  const lowCount = data.products.filter(isLow).length

  // Estoque
  const stockStore = ui.Estoque?.store ?? 'all'
  const stockGetters = { codigo: p => p.codigo, nome: p => p.nome, total: p => p.total }
  const stockRows = sortRows(data.products
    .filter(p => matches(ui.Estoque?.query, p.codigo, p.nome))
    .map(p => ({ ...p, total: stockStore === 'all' ? totals[p.id] || 0 : balanceOf(stockStore, p.id) ?? 0 })), sortOf('Estoque'), stockGetters)

  // Entradas
  const entryRows = sortRows(data.entries.filter(e => matches(ui.Entradas?.query, e.produto.codigo, e.produto.nome, e.loja.nome, e.observacao, e.usuario.nome)
    && (!ui.Entradas?.store || String(e.loja.id) === ui.Entradas.store)),
  sortOf('Entradas'), { data: e => new Date(e.dataHora).getTime(), produto: e => e.produto.codigo, loja: e => e.loja.nome, quantidade: e => e.quantidade, responsavel: e => e.usuario.nome })
  const entryPage = paginate(entryRows, ui.Entradas?.page)

  // Histórico
  const historyRows = sortRows(data.transfers.filter(t => matches(ui.Histórico?.query, t.id, t.produto.codigo, t.produto.nome, t.lojaOrigem.nome, t.lojaDestino.nome, t.usuario.nome)
    && inPeriod(t.dataHora, ui.Histórico?.period)
    && (!ui.Histórico?.store || String(t.lojaOrigem.id) === ui.Histórico.store || String(t.lojaDestino.id) === ui.Histórico.store)),
  sortOf('Histórico'), { numero: t => t.id, data: t => new Date(t.dataHora).getTime(), produto: t => t.produto.codigo, quantidade: t => t.quantidade, responsavel: t => t.usuario.nome })
  const historyPage = paginate(historyRows, ui.Histórico?.page)
  function exportHistory() {
    downloadCsv('historico-transferencias',
      ['Nº', 'Data e hora', 'Código', 'Produto', 'Origem', 'Destino', 'Quantidade', 'Responsável'],
      historyRows.map(t => [t.id, dateFormat.format(new Date(t.dataHora)), t.produto.codigo, t.produto.nome, t.lojaOrigem.nome, t.lojaDestino.nome, t.quantidade, t.usuario.nome]))
    setNotice(`${historyRows.length} transferência(s) exportada(s) para CSV.`)
  }

  // Edições
  const editRows = sortRows(data.edits.filter(e => matches(ui.Edições?.query, e.produto.codigo, e.produto.nome, e.alteracoes, e.usuario.nome)
    && (!ui.Edições?.kind || e.acao === ui.Edições.kind)),
  sortOf('Edições'), { data: e => new Date(e.dataHora).getTime(), acao: e => editLabels[e.acao], produto: e => e.produto.codigo, responsavel: e => e.usuario.nome })
  const editPage = paginate(editRows, ui.Edições?.page)

  const recentTransfers = data.transfers.slice(0, 5)

  return <section className="dashboard" aria-label="Gestão de estoque">
    <header className="app-bar">
      <div className="app-bar-row">
        <div className="brand"><span className="brand-mark" aria-hidden="true">E</span>EstoqueAPI<span className="brand-tagline">Controle de estoque entre lojas</span></div>
        <div className="session"><span className="role-pill">{isAdmin ? 'ADMIN' : 'FUNCIONÁRIO'}</span><span className="session-name" title={user.email}>{user.nome}</span><button className="bar-button" onClick={onLeave} disabled={busy || saving}>Sair</button></div>
      </div>
      <nav className="navigation" aria-label="Módulos" ref={navRef}>{sections.map(name => <button key={name} aria-current={section === name ? 'page' : undefined} onClick={() => goTo(name)} disabled={saving}>{name}</button>)}</nav>
    </header>
    <div className="content" ref={contentRef}>
    <div className="page-heading"><div><h2>{section === 'API' ? 'Acesso à API' : section}</h2><p className="muted">{descriptions[section]}</p></div>{section !== 'API' && <button className="secondary-button compact" onClick={refresh} disabled={saving || loading}>Atualizar</button>}</div>
    {accountMessage}
    {error && <p className="message error" role="alert">{error}</p>}
    {notice && <p className="message success" role="status">{notice}</p>}
    {loadError && <p className="message error" role="alert">{loadError} Use Atualizar para tentar novamente.</p>}
    {loading && <p role="status" className="muted">Atualizando dados…</p>}

    {section === 'Painel' && ready && <Painel data={data} lowProducts={data.products.filter(isLow)} totals={totals} onGo={goTo} />}

    {section === 'Produtos' && <>
      <MobileCollapse mobile={isMobile} label="+ Novo produto"><form className="erp-form product-form" onSubmit={e => save(e, 'product')} aria-busy={saving}>
        <div><label htmlFor="product-code">Código</label><input id="product-code" required maxLength={60} value={product.codigo} onChange={e => setProduct(v => ({ ...v, codigo: e.target.value }))} disabled={disabled} /></div>
        <div className="span-2"><label htmlFor="product-name">Nome do produto</label><input id="product-name" required maxLength={150} value={product.nome} onChange={e => setProduct(v => ({ ...v, nome: e.target.value }))} disabled={disabled} /></div>
        <div><label htmlFor="product-min">Estoque mínimo</label><input id="product-min" type="number" inputMode="numeric" min="0" max="2147483647" step="1" placeholder="0 = sem alerta" value={product.estoqueMinimo} onChange={e => setProduct(v => ({ ...v, estoqueMinimo: e.target.value }))} disabled={disabled} /></div>
        <div><label htmlFor="product-store">Loja do estoque inicial</label><select id="product-store" value={product.lojaId} onChange={e => setProduct(v => ({ ...v, lojaId: e.target.value }))} disabled={disabled}><option value="">Sem estoque inicial</option>{storeOptions}</select></div>
        <div><label htmlFor="product-quantity">Quantidade inicial</label><input id="product-quantity" type="number" inputMode="numeric" min="0" max="2147483647" step="1" required={!!product.lojaId} disabled={disabled || !product.lojaId} value={product.quantidade} onChange={e => setProduct(v => ({ ...v, quantidade: e.target.value }))} /></div>
        <button className="primary-button compact" disabled={disabled}>{saving ? 'Cadastrando…' : 'Cadastrar produto'}</button>
      </form></MobileCollapse>
      {editingProduct && <ProdutoEditor key={editingProduct.id} product={editingProduct} stores={data.stores} onExpired={onExpiredStable} onCancel={() => setEditingProduct(null)} onSaved={saved => { setEditingProduct(null); setError(''); setNotice(`Produto ${saved.codigo} atualizado.`); refresh() }} />}
      <div className="toolbar">
        {searchBox('Buscar por código ou nome')}
        <label className="checkbox toggle"><input type="checkbox" checked={!!tab.lowOnly} onChange={e => patch({ lowOnly: e.target.checked })} /> Só estoque baixo{lowCount > 0 && <span className="count-pill">{lowCount}</span>}</label>
      </div>
      {ready && <SortableTable {...sortProps('Produtos')} empty={!productRows.length} columns={[{ label: 'Código', key: 'codigo' }, { label: 'Nome', key: 'nome' }, { label: 'Estoque total', key: 'total' }, { label: 'Mínimo', key: 'minimo' }, { label: 'Situação' }, ...(isAdmin ? [{ label: 'Ações' }] : [])]}>
        {productRows.map(p => <tr key={p.id} className={isLow(p) ? 'row-low' : undefined}>
          <td><span className="code">{p.codigo}</span> <span className="muted">#{p.id}</span></td><td>{p.nome}</td>
          <td className="quantity">{totals[p.id] || 0}</td><td className="quantity">{p.estoqueMinimo || '—'}</td>
          <td>{p.estoqueMinimo > 0 ? <span className={isLow(p) ? 'kind kind-out' : 'kind kind-in'}>{isLow(p) ? 'Baixo' : 'OK'}</span> : <span className="muted">Sem mínimo</span>}</td>
          {isAdmin && <td><div className="row-actions"><button className="row-button" onClick={() => { setEditingProduct(p); setNotice(''); setError(''); window.scrollTo({ top: 0, behavior: 'smooth' }) }} disabled={saving}>Editar</button><button className="row-button danger" onClick={() => remove('product', p)} disabled={saving}>Excluir</button></div></td>}
        </tr>)}
      </SortableTable>}
    </>}

    {section === 'Estoque' && <>
      <div className="toolbar">
        <div className="filter"><label htmlFor="stock-store">Loja</label><select id="stock-store" value={stockStore} onChange={e => patch({ store: e.target.value })} disabled={disabled}><option value="all">Todas as lojas</option>{storeOptions}</select></div>
        {searchBox('Buscar por código ou nome')}
      </div>
      {ready && stockStore === 'all' && <SortableTable {...sortProps('Estoque')} className="matrix" empty={!stockRows.length} columns={[{ label: 'Código', key: 'codigo' }, { label: 'Produto', key: 'nome' }, ...data.stores.map(s => ({ label: s.nome })), { label: 'Total', key: 'total' }]}>
        {stockRows.map(p => <tr key={p.id} className={isLow(p) ? 'row-low' : undefined}><td className="code">{p.codigo}</td><td>{p.nome}</td>
          {data.stores.map(s => { const q = balanceOf(s.id, p.id) ?? 0; return <td key={s.id} className={q ? 'quantity' : 'quantity muted'}>{q}</td> })}
          <td className="quantity"><strong>{p.total}</strong>{isLow(p) && <span className="kind kind-out low-tag">Baixo</span>}</td></tr>)}
      </SortableTable>}
      {ready && stockStore !== 'all' && <SortableTable {...sortProps('Estoque')} empty={!stockRows.length} columns={[{ label: 'Código', key: 'codigo' }, { label: 'Produto', key: 'nome' }, { label: 'Quantidade', key: 'total' }]}>
        {stockRows.map(p => <tr key={p.id}><td className="code">{p.codigo}</td><td>{p.nome}</td><td className={p.total ? 'quantity' : 'quantity muted'}>{p.total}</td></tr>)}
      </SortableTable>}
      {ready && !data.stores.length && <p>Nenhuma loja cadastrada.</p>}
    </>}

    {section === 'Entradas' && <>
      <MobileCollapse mobile={isMobile} label="+ Registrar entrada"><form className="erp-form entry-form" onSubmit={e => save(e, 'entry')} aria-busy={saving}>
        <div><label htmlFor="entry-product">Produto</label><select id="entry-product" required value={entry.produtoId} onChange={e => setEntry(v => ({ ...v, produtoId: e.target.value }))} disabled={disabled}><option value="">Selecione um produto</option>{productOptions}</select></div>
        <div><label htmlFor="entry-store">Loja que recebeu</label><select id="entry-store" required value={entry.lojaId} onChange={e => setEntry(v => ({ ...v, lojaId: e.target.value }))} disabled={disabled}><option value="">Selecione a loja</option>{storeOptions}</select></div>
        <div><label htmlFor="entry-quantity">Quantidade</label><input id="entry-quantity" type="number" inputMode="numeric" min="1" max="2147483647" step="1" required value={entry.quantidade} onChange={e => setEntry(v => ({ ...v, quantidade: e.target.value }))} disabled={disabled} /></div>
        <div><label htmlFor="entry-note">Observação (opcional)</label><input id="entry-note" maxLength={200} placeholder="Ex.: nota fiscal 123" value={entry.observacao} onChange={e => setEntry(v => ({ ...v, observacao: e.target.value }))} disabled={disabled} /></div>
        <button className="primary-button compact" disabled={disabled}>{saving ? 'Registrando…' : 'Registrar entrada'}</button>
      </form></MobileCollapse>
      <div className="toolbar">
        {searchBox('Buscar por produto, loja, observação ou responsável')}
        <div className="filter"><label htmlFor="entry-filter-store">Loja</label><select id="entry-filter-store" value={tab.store || ''} onChange={e => patch({ store: e.target.value, page: 1 })}><option value="">Todas</option>{storeOptions}</select></div>
      </div>
      {ready && <SortableTable {...sortProps('Entradas')} empty={!entryRows.length} columns={[{ label: 'Data e hora', key: 'data' }, { label: 'Produto', key: 'produto' }, { label: 'Loja', key: 'loja' }, { label: 'Quantidade', key: 'quantidade' }, { label: 'Observação' }, { label: 'Responsável', key: 'responsavel' }]}>
        {entryPage.pageRows.map(e => <tr key={e.id}><td className="nowrap">{dateFormat.format(new Date(e.dataHora))}</td><td><span className="code">{e.produto.codigo}</span><br />{e.produto.nome}</td><td>{e.loja.nome}</td><td className="quantity">+{e.quantidade}</td><td>{e.observacao || '—'}</td><td>{e.usuario.nome}</td></tr>)}
      </SortableTable>}
      {ready && <Pager {...entryPage} count={entryRows.length} noun="entradas" onPage={page => patch({ page })} />}
    </>}

    {section === 'Transferências' && <>
      <form className="erp-form transfer-form" onSubmit={e => save(e, 'transfer')} aria-busy={saving}>
        <div className="wide"><label htmlFor="transfer-product">Produto</label><select id="transfer-product" required value={movement.produtoId} onChange={e => choose('produtoId', e.target.value)} disabled={disabled}><option value="">Selecione um produto</option>{productOptions}</select></div>
        <div><label htmlFor="transfer-origin">Loja de origem</label><select id="transfer-origin" required value={movement.lojaOrigemId} onChange={e => choose('lojaOrigemId', e.target.value)} disabled={disabled}><option value="">Selecione a origem</option>{storeOptions}</select></div>
        <div><label htmlFor="transfer-destination">Loja de destino</label><select id="transfer-destination" required value={movement.lojaDestinoId} onChange={e => choose('lojaDestinoId', e.target.value)} disabled={disabled}><option value="">Selecione o destino</option>{storeOptions}</select></div>
        {movement.produtoId && movement.lojaOrigemId && movement.lojaDestinoId && <p className="balance-preview wide" role="status">Saldo atual · Origem: {originBalance} · Destino: {destinationBalance}</p>}
        <div><label htmlFor="transfer-quantity">Quantidade{originBalance !== undefined && <span className="muted"> (máx. {originBalance})</span>}</label><input id="transfer-quantity" type="number" inputMode="numeric" min="1" max={originBalance !== undefined ? Math.max(originBalance, 1) : 2147483647} step="1" required value={movement.quantidade} onChange={e => choose('quantidade', e.target.value)} disabled={disabled || originBalance === 0} /></div>
        <button className="primary-button compact" disabled={disabled || originBalance === 0}>{saving ? 'Transferindo…' : originBalance === 0 ? 'Origem sem saldo' : 'Confirmar transferência'}</button>
      </form>
      <p className="muted">O saldo é conferido ao confirmar. O responsável e o horário são registrados automaticamente.</p>
      {ready && <div className="panel recent">
        <h3 className="panel-head">Últimas transferências <button type="button" className="link-button" onClick={() => goTo('Histórico')}>Ver histórico completo →</button></h3>
        {!recentTransfers.length ? <p className="empty">Nenhuma transferência ainda.</p> : <div className="table-scroll"><table><thead><tr><th scope="col">Nº</th><th scope="col">Quando</th><th scope="col">Produto</th><th scope="col">Movimento</th><th scope="col">Responsável</th></tr></thead>
          <tbody>{recentTransfers.map(t => <tr key={t.id}><td>{t.id}</td><td className="nowrap">{dateFormat.format(new Date(t.dataHora))}</td><td><span className="code">{t.produto.codigo}</span> {t.produto.nome}</td><td>{t.quantidade} un. · {t.lojaOrigem.nome} → {t.lojaDestino.nome}</td><td>{t.usuario.nome}</td></tr>)}</tbody></table></div>}
      </div>}
    </>}

    {section === 'Histórico' && <>
      <div className="toolbar">
        {searchBox('Buscar por produto, loja ou responsável')}
        <div className="filter"><label htmlFor="history-period">Período</label><select id="history-period" value={tab.period || ''} onChange={e => patch({ period: e.target.value, page: 1 })}><option value="">Tudo</option><option value="hoje">Hoje</option><option value="7">Últimos 7 dias</option><option value="30">Últimos 30 dias</option></select></div>
        <div className="filter"><label htmlFor="history-store">Loja (origem ou destino)</label><select id="history-store" value={tab.store || ''} onChange={e => patch({ store: e.target.value, page: 1 })}><option value="">Todas</option>{storeOptions}</select></div>
        <button className="secondary-button compact export" onClick={exportHistory} disabled={!ready || !historyRows.length}>Exportar CSV</button>
      </div>
      {ready && <SortableTable {...sortProps('Histórico')} empty={!historyRows.length} columns={[{ label: 'Nº', key: 'numero' }, { label: 'Data e hora', key: 'data' }, { label: 'Produto', key: 'produto' }, { label: 'Origem' }, { label: 'Destino' }, { label: 'Quantidade', key: 'quantidade' }, { label: 'Responsável', key: 'responsavel' }, ...(isAdmin ? [{ label: 'Ações' }] : [])]}>
        {historyPage.pageRows.map(t => <tr key={t.id}><td>{t.id}</td><td className="nowrap">{dateFormat.format(new Date(t.dataHora))}</td><td><span className="code">{t.produto.codigo}</span><br />{t.produto.nome}</td><td>{t.lojaOrigem.nome}</td><td>{t.lojaDestino.nome}</td><td className="quantity">{t.quantidade}</td><td>{t.usuario.nome}</td>{isAdmin && <td><div className="row-actions"><button className="row-button danger" onClick={() => remove('transfer', t)} disabled={saving}>Excluir</button></div></td>}</tr>)}
      </SortableTable>}
      {ready && <Pager {...historyPage} count={historyRows.length} noun="transferências" onPage={page => patch({ page })} />}
    </>}

    {section === 'Edições' && <>
      <div className="toolbar">
        {searchBox('Buscar por produto, alteração ou responsável')}
        <div className="filter"><label htmlFor="edit-kind">Tipo</label><select id="edit-kind" value={tab.kind || ''} onChange={e => patch({ kind: e.target.value, page: 1 })}><option value="">Todos</option>{Object.entries(editLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
      </div>
      {ready && <SortableTable {...sortProps('Edições')} empty={!editRows.length} columns={[{ label: 'Data e hora', key: 'data' }, { label: 'Ação', key: 'acao' }, { label: 'Produto', key: 'produto' }, { label: 'O que mudou' }, { label: 'Responsável', key: 'responsavel' }]}>
        {editPage.pageRows.map(e => <tr key={e.id}><td className="nowrap">{dateFormat.format(new Date(e.dataHora))}</td><td><span className={`kind ${e.acao === 'EDICAO' ? 'kind-edit' : 'kind-out'}`}>{editLabels[e.acao] || e.acao}</span></td><td><span className="code">{e.produto.codigo}</span><br />{e.produto.nome}</td><td>{e.alteracoes.split('; ').map((m, i) => <div key={i}>{m}</div>)}</td><td>{e.usuario.nome}</td></tr>)}
      </SortableTable>}
      {ready && <Pager {...editPage} count={editRows.length} noun="registros" onPage={page => patch({ page })} />}
    </>}

    {section === 'Lojas' && isAdmin && ready && <Lojas stores={data.stores} stocks={data.stocks} disabled={disabled} onError={e => { setNotice(''); fail(e) }} onDone={message => { setError(''); setNotice(message); refresh() }} />}
    {section === 'Funcionários' && isAdmin && <Funcionarios user={user} revision={revision} onExpired={() => expired.current()} />}
    {section === 'API' && apiTools}
    </div>
  </section>
}
